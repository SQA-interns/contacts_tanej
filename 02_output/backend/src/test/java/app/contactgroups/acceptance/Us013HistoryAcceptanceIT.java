package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-013 History: AC-013-01 to AC-013-12, EX-013-01 to EX-013-08. */
class Us013HistoryAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private JsonNode horvat() {
    return newContact(
        ANA, gid, j("firstName", "Eva", "lastName", "Horvat", "phone", "+386 40 123 456"));
  }

  private JsonNode contactEntries(Who who, JsonNode contact) {
    return contactHistory(who, gid, contact.path("id").asText()).expect(200).json().path("items");
  }

  private int groupTotal() {
    return history(ANA, gid).expect(200).at("/total").asInt();
  }

  private static JsonNode changeOf(JsonNode entry, String field) {
    for (JsonNode c : entry.path("changes")) {
      if (field.equals(c.path("field").asText())) {
        return c;
      }
    }
    return null;
  }

  @Test
  void AC_013_01_aChangeShowsWhoWhenWhichFieldAndOldAndNewValue() {
    JsonNode c = horvat();
    save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText()), "phone", "+386 41 555 123")
        .expect(200);
    JsonNode top = contactEntries(ANA, c).get(0);
    assertThat(top.at("/actor/name").asText()).isEqualTo("Marko Pirc");
    assertThat(top.at("/actor/email").asText()).isEqualTo("marko@example.com");
    assertThat(top.path("createdAt").asText()).isNotBlank();
    assertThat(top.path("eventType").asText()).isEqualTo("CONTACT_EDITED");
    JsonNode phone = changeOf(top, "phone");
    assertThat(phone).isNotNull();
    assertThat(phone.path("oldValue").asText()).isEqualTo("+386 40 123 456");
    assertThat(phone.path("newValue").asText()).isEqualTo("+386 41 555 123");
  }

  @Test
  void AC_013_02_twoFieldChangesOfOneSaveAreGroupedTogether() {
    JsonNode c = horvat();
    save(
            MARKO,
            gid,
            openContact(MARKO, gid, c.path("id").asText()),
            "phone",
            "+386 41 555 123",
            "company",
            "Acme")
        .expect(200);
    JsonNode items = contactEntries(ANA, c);
    String latestSet = items.get(0).path("changeSetId").asText();
    Set<String> fields = new HashSet<>();
    for (JsonNode e : items) {
      if (latestSet.equals(e.path("changeSetId").asText())) {
        for (JsonNode ch : e.path("changes")) {
          fields.add(ch.path("field").asText());
        }
      }
    }
    assertThat(fields).contains("phone", "company");
  }

  @Test
  void AC_013_03_anAddedTagAndAttributeAreRecordedWithNameKeyAndValue() {
    JsonNode c = horvat();
    save(
            ANA,
            gid,
            openContact(ANA, gid, c.path("id").asText()),
            "tags",
            arr("udelezenec2025"),
            "attributes",
            arr(attr("kompetenca", "spletnoProgramiranje")))
        .expect(200);
    JsonNode items = contactEntries(ANA, c);
    List<String> types = values(items, "eventType");
    assertThat(types).contains("TAG_CHANGED", "ATTRIBUTE_CHANGED");
    assertThat(items.toString()).contains("udelezenec2025", "kompetenca", "spletnoProgramiranje");
  }

  @Test
  void AC_013_04_renamingATagGivesOneEntryPerAffectedContactInTheGroupHistory() {
    for (String l : List.of("A", "B", "C", "D")) {
      newContact(ANA, gid, j("lastName", l, "tags", arr("old")));
    }
    String tagId = get(ANA, "/groups/" + gid + "/tags").expect(200).at("/0/id").asText();
    patch(ANA, "/groups/" + gid + "/tags/" + tagId, j("name", "new")).expect(200);
    List<JsonNode> entries = entries(ANA, gid, "TAG_RENAMED");
    assertThat(entries).hasSize(4);
    for (JsonNode e : entries) {
      assertThat(e.toString()).contains("old", "new");
    }
  }

  @Test
  void AC_013_05_anExportIsRecordedWithWhoWhenAndTheCount() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A"));
    JsonNode b = newContact(ANA, gid, j("lastName", "B"));
    post(
            MARKO,
            "/groups/" + gid + "/exports",
            j("language", "sl", "contactIds", arr(a.path("id").asText(), b.path("id").asText())))
        .expect(200);
    List<JsonNode> exported = entries(ANA, gid, "EXPORTED");
    assertThat(exported).hasSize(1);
    assertThat(exported.get(0).at("/actor/name").asText()).isEqualTo("Marko Pirc");
    assertThat(exported.get(0).path("createdAt").asText()).isNotBlank();
    assertThat(changeOf(exported.get(0), "contacts").path("newValue").asText()).isEqualTo("2");
  }

  @Test
  void AC_013_06_everyGroupLevelEventHasOneEntry() {
    post(ANA, "/groups/" + gid + "/invitations", j("email", "ivo@example.com", "role", "VIEWER"))
        .expect(201);
    String invitation =
        get(ANA, "/groups/" + gid + "/invitations").expect(200).at("/0/id").asText();
    put(ANA, "/groups/" + gid + "/members/" + MARKO.uid(), j("role", "VIEWER")).expect(200);
    delete(ANA, "/groups/" + gid + "/members/" + PETRA.uid()).expect(204);
    delete(ANA, "/groups/" + gid + "/invitations/" + invitation).expect(204);
    patch(ANA, "/groups/" + gid, j("name", "Konferenca 2027")).expect(200);
    assertThat(entries(ANA, gid, "MEMBER_INVITED")).hasSize(3);
    assertThat(entries(ANA, gid, "ROLE_CHANGED")).hasSize(1);
    assertThat(entries(ANA, gid, "MEMBER_REMOVED")).hasSize(1);
    assertThat(entries(ANA, gid, "INVITATION_CANCELLED")).hasSize(1);
    assertThat(entries(ANA, gid, "GROUP_RENAMED")).hasSize(1);
    String all = history(ANA, gid).expect(200).text();
    assertThat(all).doesNotContain("petra@example.com", "ivo@example.com");
    assertThat(all).contains("Konferenca 2026", "Konferenca 2027");
  }

  @Test
  void AC_013_07_viewingAndSearchingLeaveNoEntry() {
    JsonNode c = horvat();
    int before = groupTotal();
    openContact(ANA, gid, c.path("id").asText());
    search(ANA, gid, "horvat");
    search(MARKO, gid, "");
    get(PETRA, "/groups/" + gid + "/members").expect(200);
    assertThat(groupTotal()).isEqualTo(before);
  }

  @Test
  void AC_013_08_aViewerHasNoHistory() {
    JsonNode c = horvat();
    contactHistory(PETRA, gid, c.path("id").asText()).expectError(403, "FORBIDDEN");
    history(PETRA, gid).expectError(403, "FORBIDDEN");
  }

  @Test
  void AC_013_09_noEndpointChangesOrDeletesAnEntry() {
    JsonNode c = horvat();
    String entry = history(ANA, gid).expect(200).at("/items/0/id").asText();
    int before = groupTotal();
    for (String method : List.of("PUT", "PATCH", "DELETE", "POST")) {
      for (String path :
          List.of(
              "/groups/" + gid + "/history",
              "/groups/" + gid + "/history/" + entry,
              "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/history")) {
        Res r = request(method, path, "Bearer " + token(ANA), method.equals("DELETE") ? null : j());
        assertThat(r.status()).as("%s %s", method, path).isIn(404, 405);
      }
    }
    assertThat(groupTotal()).isEqualTo(before);
    assertThat(history(ANA, gid).expect(200).at("/items/0/id").asText()).isEqualTo(entry);
  }

  @Test
  void AC_013_10_afterAnErasureTheEntriesStayWithTheActorAndTheValuesAreErased() {
    JsonNode c = horvat();
    String id = c.path("id").asText();
    save(MARKO, gid, openContact(MARKO, gid, id), "phone", "+386 41 555 123").expect(200);
    trash(ANA, gid, id);
    delete(ANA, "/groups/" + gid + "/contacts/" + id).expect(204);
    JsonNode page = history(ANA, gid).expect(200).json();
    List<JsonNode> about = new ArrayList<>();
    for (JsonNode e : page.path("items")) {
      if (e.path("erased").asBoolean()) {
        about.add(e);
      }
    }
    assertThat(about).hasSizeGreaterThanOrEqualTo(3);
    for (JsonNode e : about) {
      assertThat(e.at("/actor/name").asText()).isIn("Ana Novak", "Marko Pirc");
    }
    assertThat(page.toString()).doesNotContain("Horvat", "+386 41 555 123", "+386 40 123 456");
    assertThat(page.toString()).contains("CONTACT_ERASED");
  }

  @Test
  void AC_013_11_deletingAndRestoringAContactGivesTwoEntries() {
    JsonNode c = horvat();
    String id = c.path("id").asText();
    trash(MARKO, gid, id);
    post(MARKO, "/groups/" + gid + "/contacts/" + id + "/restore", null).expect(204);
    List<String> types = values(contactEntries(ANA, c), "eventType");
    assertThat(types).containsSubsequence("CONTACT_RESTORED", "CONTACT_DELETED");
  }

  @Test
  void AC_013_12_deletingAndRestoringAGroupGivesTwoGroupLevelEntries() {
    post(ANA, "/groups/" + gid + "/trash", j("name", "Konferenca 2026")).expect(204);
    post(ANA, "/groups/" + gid + "/restore", null).expect(204);
    assertThat(entries(ANA, gid, "GROUP_DELETED")).hasSize(1);
    assertThat(entries(ANA, gid, "GROUP_RESTORED")).hasSize(1);
  }

  @Test
  void EX_013_01_aSaveWithNoChangeWritesNoEntry() {
    JsonNode c = horvat();
    int before = groupTotal();
    save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText())).expect(200);
    assertThat(groupTotal()).isEqualTo(before);
  }

  @Test
  void EX_013_02_theTimeIsStoredInUtc() {
    JsonNode c = horvat();
    clock.advance(Duration.between(clock.instant(), Instant.parse("2026-07-01T22:30:00Z")));
    save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText()), "company", "Acme").expect(200);
    String at = contactEntries(ANA, c).get(0).path("createdAt").asText();
    assertThat(Instant.parse(at))
        .isBetween(Instant.parse("2026-07-01T22:29:30Z"), Instant.parse("2026-07-01T22:31:30Z"));
  }

  @Test
  void EX_013_03_afterTheActorsAccountIsDeletedTheNameStaysAndTheEmailIsGone() {
    JsonNode c = horvat();
    save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText()), "company", "Acme").expect(200);
    delete(MARKO, "/me").expect(204);
    JsonNode top = contactEntries(ANA, c).get(0);
    assertThat(top.at("/actor/name").asText()).isEqualTo("Marko Pirc");
    assertThat(top.at("/actor").has("email")).isFalse();
    assertThat(history(ANA, gid).text()).doesNotContain("marko@example.com");
  }

  @Test
  void EX_013_04_aRemovedMembersEarlierEntriesRemainWithTheName() {
    JsonNode c = horvat();
    save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText()), "company", "Acme").expect(200);
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    assertThat(contactEntries(ANA, c).get(0).at("/actor/name").asText()).isEqualTo("Marko Pirc");
  }

  @Test
  void EX_013_05_anOldEmptyValueIsReportedAsEmpty() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Horvat"));
    save(ANA, gid, openContact(ANA, gid, c.path("id").asText()), "phone", "+386 41 555 123")
        .expect(200);
    JsonNode change = changeOf(contactEntries(ANA, c).get(0), "phone");
    assertThat(change).isNotNull();
    assertThat(change.path("oldValue").asText()).isEmpty();
    assertThat(change.path("newValue").asText()).isEqualTo("+386 41 555 123");
  }

  @Test
  void EX_013_06_whenTheGroupIsDeletedForGoodAllItsHistoryIsDeleted() {
    newContact(ANA, gid, j("lastName", "Horvat"));
    post(ANA, "/groups/" + gid + "/trash", j("name", "Konferenca 2026")).expect(204);
    clock.advance(Duration.ofDays(30).plusMinutes(1));
    eventually(
        () -> jdbc.queryForObject("SELECT count(*) FROM history_entries", Integer.class) == 0,
        "the History of the purged group is deleted");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM groups", Integer.class)).isZero();
  }

  @Test
  void EX_013_07_oldAndNewValueOfLongNotesAreKeptInFull() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Horvat", "notes", repeat("a", 5000)));
    save(ANA, gid, openContact(ANA, gid, c.path("id").asText()), "notes", repeat("b", 5000))
        .expect(200);
    JsonNode change = changeOf(contactEntries(ANA, c).get(0), "notes");
    assertThat(change.path("oldValue").asText()).isEqualTo(repeat("a", 5000));
    assertThat(change.path("newValue").asText()).isEqualTo(repeat("b", 5000));
  }

  @Test
  void EX_013_08_aRefusedSaveWritesNoEntry() {
    JsonNode c = horvat();
    int before = groupTotal();
    save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText()), "email", "ana.novak@")
        .expect(400);
    JsonNode stale = openContact(MARKO, gid, c.path("id").asText());
    save(ANA, gid, stale, "company", "Acme").expect(200);
    int afterOne = groupTotal();
    save(MARKO, gid, stale, "company", "Beta").expectError(409, "CONFLICT");
    assertThat(afterOne).isEqualTo(before + 1);
    assertThat(groupTotal()).isEqualTo(afterOne);
  }
}
