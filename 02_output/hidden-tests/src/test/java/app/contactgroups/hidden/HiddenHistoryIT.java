package app.contactgroups.hidden;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import app.contactgroups.acceptance.Who;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases for R-013 to R-016 (History, group History, group Trash, account) and R-019. */
class HiddenHistoryIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Skupina");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private JsonNode contact(Object... pairs) {
    return newContact(ANA, gid, j(pairs));
  }

  private JsonNode items(Who who, JsonNode c) {
    return contactHistory(who, gid, c.path("id").asText()).expect(200).at("/items");
  }

  private static JsonNode change(JsonNode entry, String field) {
    for (JsonNode ch : entry.path("changes")) {
      if (field.equals(ch.path("field").asText())) {
        return ch;
      }
    }
    return null;
  }

  private JsonNode now(JsonNode c) {
    return openContact(ANA, gid, c.path("id").asText());
  }

  @Test
  @DisplayName("R-013-01 combination create edit delete restore erase give five entries in order")
  void R_013_01_combination_lifecycle() {
    JsonNode c = contact("lastName", "Zivljenje");
    String id = c.path("id").asText();
    save(MARKO, gid, now(c), "company", "Acme").expect(200);
    trash(ANA, gid, id);
    post(ANA, "/groups/" + gid + "/contacts/" + id + "/restore", null).expect(204);
    trash(ANA, gid, id);
    delete(ANA, "/groups/" + gid + "/contacts/" + id).expect(204);
    List<String> types = values(history(ANA, gid).expect(200).at("/items"), "eventType");
    assertThat(types)
        .containsSubsequence(
            "CONTACT_ERASED",
            "CONTACT_DELETED",
            "CONTACT_RESTORED",
            "CONTACT_DELETED",
            "CONTACT_EDITED",
            "CONTACT_CREATED");
  }

  @Test
  @DisplayName(
      "R-013-02 combination removing a tag and an attribute is recorded with the old value")
  void R_013_02_combination_removals() {
    JsonNode c =
        contact(
            "lastName",
            "A",
            "tags",
            arr("t1", "t2"),
            "attributes",
            arr(attr("k", "v1"), attr("l", "v2")));
    save(ANA, gid, now(c), "tags", arr("t1"), "attributes", arr(attr("k", "v1"))).expect(200);
    JsonNode top = items(ANA, c);
    String all = top.toString();
    assertThat(all).contains("t2", "attribute:l", "v2");
    List<String> types = values(top, "eventType");
    assertThat(types.subList(0, 2)).containsExactlyInAnyOrder("TAG_CHANGED", "ATTRIBUTE_CHANGED");
  }

  @Test
  @DisplayName(
      "R-013-03 combination a deleted tag gives its name per contact including a trashed one")
  void R_013_03_combination_trashedContactToo() {
    JsonNode a = contact("lastName", "A", "tags", arr("old"));
    JsonNode b = contact("lastName", "B", "tags", arr("old"));
    trash(ANA, gid, b.path("id").asText());
    String tagId = get(ANA, "/groups/" + gid + "/tags").expect(200).at("/0/id").asText();
    delete(ANA, "/groups/" + gid + "/tags/" + tagId).expect(204);
    assertThat(entries(ANA, gid, "TAG_DELETED")).hasSize(2);
    assertThat(items(ANA, a).toString()).contains("TAG_DELETED", "old");
  }

  @Test
  @DisplayName("R-013-04 boundary an export of one contact records the count one")
  void R_013_04_boundary_oneContact() {
    JsonNode a = contact("lastName", "A");
    post(
            MARKO,
            "/groups/" + gid + "/exports",
            j("language", "en", "contactIds", arr(a.path("id").asText())))
        .expect(200);
    JsonNode e = entries(ANA, gid, "EXPORTED").get(0);
    assertThat(change(e, "contacts").path("newValue").asText()).isEqualTo("1");
    assertThat(e.at("/actor/email").asText()).isEqualTo("marko@example.com");
  }

  @Test
  @DisplayName("R-013-05 combination the role of an invitation is recorded and the e-mail never")
  void R_013_05_combination_roleNotEmail() {
    post(
            ANA,
            "/groups/" + gid + "/invitations",
            j("email", "skrivni@example.com", "role", "EDITOR"))
        .expect(201);
    put(ANA, "/groups/" + gid + "/members/" + PETRA.uid(), j("role", "EDITOR")).expect(200);
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    post(ANA, "/groups/" + gid + "/restore", null).expect(204);
    String all = history(ANA, gid).expect(200).text();
    assertThat(all).doesNotContain("skrivni@example.com");
    JsonNode removed = entries(ANA, gid, "MEMBER_REMOVED").get(0);
    assertThat(removed.path("subjectName").asText()).isEqualTo("Marko Pirc");
    JsonNode roleChanged = entries(ANA, gid, "ROLE_CHANGED").get(0);
    assertThat(roleChanged.path("subjectName").asText()).isEqualTo("Petra Kos");
    assertThat(roleChanged.path("role").asText()).isEqualTo("EDITOR");
    assertThat(entries(ANA, gid, "GROUP_DELETED")).hasSize(1);
    assertThat(entries(ANA, gid, "GROUP_RESTORED")).hasSize(1);
  }

  @Test
  @DisplayName("R-013-05 other joining and creating a group write no entry")
  void R_013_05_other_noEntryForJoiningOrCreating() {
    String fresh = group(BORIS, "Nova");
    assertThat(history(BORIS, fresh).expect(200).at("/total").asInt()).isZero();
    int before = history(ANA, gid).expect(200).at("/total").asInt();
    post(ANA, "/groups/" + gid + "/invitations", j("email", "boris@example.com", "role", "VIEWER"))
        .expect(201);
    get(BORIS, "/groups").expect(200);
    assertThat(history(ANA, gid).expect(200).at("/total").asInt()).isEqualTo(before + 1);
  }

  @Test
  @DisplayName(
      "R-013-06 other a version restore and a merge are recorded with their own event types")
  void R_013_06_other_versionRestoreAndMerge() {
    JsonNode a = contact("lastName", "A", "email", "x@x.si", "phone", "1");
    save(ANA, gid, now(a), "phone", "2").expect(200);
    post(
            ANA,
            "/groups/" + gid + "/contacts/" + a.path("id").asText() + "/versions/1/restore",
            j("revision", now(a).path("revision").asLong()))
        .expect(200);
    assertThat(entries(ANA, gid, "VERSION_RESTORED")).hasSize(1);
    JsonNode b = contact("lastName", "B", "email", "x@x.si");
    post(
            ANA,
            "/groups/" + gid + "/merges",
            j(
                "contacts",
                arr(
                    j("id", a.path("id").asText(), "revision", now(a).path("revision").asLong()),
                    j("id", b.path("id").asText(), "revision", now(b).path("revision").asLong())),
                "fields",
                j("lastName", "A", "email", "x@x.si", "phone", "1")))
        .expect(200);
    assertThat(entries(ANA, gid, "CONTACTS_MERGED")).hasSize(1);
  }

  @Test
  @DisplayName("R-013-07 combination one save shares one change set and two saves do not")
  void R_013_07_combination_changeSets() {
    JsonNode c = contact("lastName", "A");
    save(ANA, gid, now(c), "company", "X", "phone", "1", "tags", arr("t")).expect(200);
    save(ANA, gid, now(c), "company", "Y").expect(200);
    JsonNode all = items(ANA, c);
    Set<String> sets = new HashSet<>();
    for (JsonNode e : all) {
      sets.add(e.path("changeSetId").asText());
    }
    assertThat(sets).hasSize(3);
    assertThat(all.toString()).contains("\"oldValue\":\"X\"", "\"newValue\":\"Y\"");
  }

  @Test
  @DisplayName("R-013-08 other reading history and the member list writes nothing")
  void R_013_08_other_readsWriteNothing() {
    contact("lastName", "A");
    int before = history(ANA, gid).expect(200).at("/total").asInt();
    for (int i = 0; i < 3; i++) {
      history(ANA, gid).expect(200);
      get(ANA, "/groups/" + gid + "/tags").expect(200);
      post(PETRA, "/groups/" + gid + "/contacts/search", j("q", "a")).expect(200);
      post(ANA, "/groups/" + gid + "/duplicates/search", null).expect(200);
    }
    assertThat(history(ANA, gid).expect(200).at("/total").asInt()).isEqualTo(before);
  }

  @Test
  @DisplayName("R-013-09 combination the tab is newest first and pages of 25")
  void R_013_09_combination_newestFirstPaged() {
    JsonNode c = contact("lastName", "A");
    JsonNode cur = now(c);
    for (int i = 1; i <= 30; i++) {
      cur = save(ANA, gid, cur, "company", "F" + i).expect(200).json();
    }
    Res p1 = contactHistory(ANA, gid, c.path("id").asText()).expect(200);
    Res p2 =
        get(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/history?page=2")
            .expect(200);
    assertThat(p1.at("/items")).hasSize(25);
    assertThat(p1.at("/total").asInt()).isEqualTo(31);
    assertThat(p2.at("/items")).hasSize(6);
    assertThat(p1.toString()).contains("F30");
    assertThat(p2.at("/items/5/eventType").asText()).isEqualTo("CONTACT_CREATED");
  }

  @Test
  @DisplayName("R-013-10 role History cannot be changed by any role")
  void R_013_10_role_noChange() {
    contact("lastName", "A");
    String entry = history(ANA, gid).expect(200).at("/items/0/id").asText();
    for (Who w : List.of(ANA, MARKO, PETRA, BORIS)) {
      for (String m : List.of("PUT", "PATCH", "DELETE")) {
        Res r =
            request(
                m,
                "/groups/" + gid + "/history/" + entry,
                "Bearer " + token(w),
                m.equals("DELETE") ? null : j());
        assertThat(r.status()).as("%s by %s", m, w.name()).isIn(403, 404, 405);
      }
    }
    assertThat(history(ANA, gid).expect(200).at("/items/0/id").asText()).isEqualTo(entry);
  }

  @Test
  @DisplayName(
      "R-013-11 other entries of a removed member and of a purged contact stay with the group")
  void R_013_11_other_entriesLiveWithGroup() {
    JsonNode c = newContact(MARKO, gid, j("lastName", "A"));
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    assertThat(items(ANA, c).get(0).at("/actor/name").asText()).isEqualTo("Marko Pirc");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM history_entries", Integer.class))
        .isPositive();
  }

  @Test
  @DisplayName(
      "R-013-12 combination erasure removes label and values but keeps type actor and time")
  void R_013_12_combination_erasedEntryShape() {
    JsonNode c = contact("firstName", "Tajna", "lastName", "Oseba", "email", "tajna@example.com");
    trash(MARKO, gid, c.path("id").asText());
    delete(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText()).expect(204);
    for (JsonNode e : history(ANA, gid).expect(200).at("/items")) {
      if (e.path("erased").asBoolean()) {
        assertThat(e.path("actor").path("name").asText()).isNotBlank();
        assertThat(e.path("createdAt").asText()).isNotBlank();
        assertThat(e.has("contactLabel")).isFalse();
        for (JsonNode ch : e.path("changes")) {
          assertThat(ch.has("oldValue") || ch.has("newValue")).isFalse();
        }
      }
    }
    assertThat(history(ANA, gid).text()).doesNotContain("Tajna", "Oseba", "tajna@example.com");
  }

  @Test
  @DisplayName("R-013-13 role a Viewer and a non-member get no history")
  void R_013_13_role_noHistory() {
    JsonNode c = contact("lastName", "A");
    history(PETRA, gid).expectError(403, "FORBIDDEN");
    contactHistory(PETRA, gid, c.path("id").asText()).expectError(403, "FORBIDDEN");
    history(BORIS, gid).expectError(404, "NOT_FOUND");
  }

  @Test
  @DisplayName("R-014-01 boundary entries are newest first with equal pages and a third page")
  void R_014_01_boundary_pagesOf25() {
    bulkContacts(ANA, gid, 52, i -> j("lastName", "P" + i));
    JsonNode p1 = get(ANA, "/groups/" + gid + "/history?types=CONTACT_CREATED").expect(200).json();
    JsonNode p3 =
        get(ANA, "/groups/" + gid + "/history?types=CONTACT_CREATED&page=3").expect(200).json();
    assertThat(p1.at("/total").asInt()).isEqualTo(52);
    assertThat(p3.path("items")).hasSize(2);
    List<String> times = values(p1.path("items"), "createdAt");
    List<String> sorted = new ArrayList<>(times);
    sorted.sort(java.util.Comparator.reverseOrder());
    assertThat(times).isEqualTo(sorted);
  }

  @Test
  @DisplayName("R-014-02 combination the person filter with a person who has no entries")
  void R_014_02_combination_personWithoutEntries() {
    contact("lastName", "A");
    assertThat(
            get(ANA, "/groups/" + gid + "/history?actorId=" + PETRA.uid())
                .expect(200)
                .at("/total")
                .asInt())
        .isZero();
    assertThat(get(ANA, "/groups/" + gid + "/history/actors").expect(200).json()).isNotEmpty();
  }

  @Test
  @DisplayName("R-014-03 combination several event types are combined with OR")
  void R_014_03_combination_severalTypes() {
    JsonNode c = contact("lastName", "A");
    save(ANA, gid, now(c), "company", "X").expect(200);
    trash(ANA, gid, c.path("id").asText());
    JsonNode r =
        get(ANA, "/groups/" + gid + "/history?types=CONTACT_CREATED,CONTACT_DELETED")
            .expect(200)
            .json();
    assertThat(values(r.path("items"), "eventType"))
        .containsExactlyInAnyOrder("CONTACT_CREATED", "CONTACT_DELETED");
    get(ANA, "/groups/" + gid + "/history?types=NOT_A_TYPE").expect(400);
  }

  @Test
  @DisplayName(
      "R-014-04 boundary the last day is inclusive up to 23:59 in Ljubljana and summer time")
  void R_014_04_boundary_inclusiveLastDay() {
    clockAt("2026-07-02T21:59:00Z");
    contact("lastName", "Pozno");
    clockAt("2026-07-02T22:00:00Z");
    contact("lastName", "Naslednji");
    assertThat(
            get(
                    ANA,
                    "/groups/"
                        + gid
                        + "/history?types=CONTACT_CREATED&from=2026-07-02&to=2026-07-02")
                .expect(200)
                .at("/total")
                .asInt())
        .isEqualTo(1);
    assertThat(
            get(
                    ANA,
                    "/groups/"
                        + gid
                        + "/history?types=CONTACT_CREATED&from=2026-07-02&to=2026-07-03")
                .expect(200)
                .at("/total")
                .asInt())
        .isEqualTo(2);
    assertThat(
            get(
                    ANA,
                    "/groups/"
                        + gid
                        + "/history?types=CONTACT_CREATED&from=2026-07-03&to=2026-07-03")
                .expect(200)
                .at("/total")
                .asInt())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("R-014-04 boundary winter time the day starts at 23:00 UTC")
  void R_014_04_boundary_winterDay() {
    clockAt("2026-01-14T22:59:00Z");
    contact("lastName", "Zima1");
    clockAt("2026-01-14T23:00:00Z");
    contact("lastName", "Zima2");
    assertThat(
            get(
                    ANA,
                    "/groups/"
                        + gid
                        + "/history?types=CONTACT_CREATED&from=2026-01-15&to=2026-01-15")
                .expect(200)
                .at("/total")
                .asInt())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("R-014-05 combination three filters together")
  void R_014_05_combination_threeFilters() {
    clockAt("2026-07-02T10:00:00Z");
    JsonNode c = newContact(MARKO, gid, j("lastName", "A"));
    save(MARKO, gid, now(c), "company", "X").expect(200);
    save(ANA, gid, now(c), "company", "Y").expect(200);
    clockAt("2026-07-05T10:00:00Z");
    save(MARKO, gid, now(c), "company", "Z").expect(200);
    JsonNode r =
        get(
                ANA,
                "/groups/"
                    + gid
                    + "/history?actorId="
                    + MARKO.uid()
                    + "&types=CONTACT_EDITED&from=2026-07-02&to=2026-07-03")
            .expect(200)
            .json();
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    assertThat(r.toString()).contains("\"newValue\":\"X\"");
  }

  @Test
  @DisplayName(
      "R-014-06 other a page beyond the last of a filter gives an empty or last page without error")
  void R_014_06_other_noMatchNoError() {
    contact("lastName", "A");
    JsonNode r =
        get(ANA, "/groups/" + gid + "/history?types=GROUP_DELETED&page=4").expect(200).json();
    assertThat(r.at("/total").asInt()).isZero();
    assertThat(r.path("items")).isEmpty();
  }

  @Test
  @DisplayName("R-014-07 role the group history actors and filters are refused to a Viewer")
  void R_014_07_role_viewerRefused() {
    get(PETRA, "/groups/" + gid + "/history?types=EXPORTED").expectError(403, "FORBIDDEN");
    get(PETRA, "/groups/" + gid + "/history/actors").expectError(403, "FORBIDDEN");
    get(MARKO, "/groups/" + gid + "/history/actors").expect(200);
  }

  @Test
  @DisplayName("R-014-08 other the actors list holds only people of this group")
  void R_014_08_other_actorsOfThisGroup() {
    String other = group(BORIS, "Druga");
    newContact(BORIS, other, j("lastName", "T"));
    contact("lastName", "A");
    assertThat(values(get(ANA, "/groups/" + gid + "/history/actors").expect(200).json(), "id"))
        .doesNotContain(BORIS.uid());
    assertThat(get(ANA, "/groups/" + other + "/history/actors").status()).isEqualTo(404);
  }

  @Test
  @DisplayName("R-015-01 combination deleting twice and with the wrong name")
  void R_015_01_combination_wrongThenRight() {
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupin"))
        .expectFieldError("name", "NAME_MISMATCH");
    post(ANA, "/groups/" + gid + "/trash", j()).expect(400);
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    assertThat(post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).status())
        .isIn(404, 409);
    assertThat(get(ANA, "/deleted-groups").expect(200).json()).hasSize(1);
  }

  @Test
  @DisplayName("R-015-02 boundary the name is compared case-sensitively and accents count")
  void R_015_02_boundary_caseAndAccents() {
    String g = group(ANA, "Čebelarji");
    post(ANA, "/groups/" + g + "/trash", j("name", "cebelarji"))
        .expectFieldError("name", "NAME_MISMATCH");
    post(ANA, "/groups/" + g + "/trash", j("name", "ČEBELARJI"))
        .expectFieldError("name", "NAME_MISMATCH");
    post(ANA, "/groups/" + g + "/trash", j("name", "\tČebelarji\n")).expect(204);
  }

  @Test
  @DisplayName(
      "R-015-03 other every endpoint of a group in Trash answers not found to its non-Owner members")
  void R_015_03_other_everyEndpointNotFound() {
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    for (Who w : List.of(MARKO, PETRA)) {
      for (Res r :
          List.of(
              get(w, "/groups/" + gid),
              get(w, "/groups/" + gid + "/members"),
              get(w, "/groups/" + gid + "/tags"),
              post(w, "/groups/" + gid + "/contacts/search", j()),
              post(w, "/groups/" + gid + "/leave", null),
              post(w, "/groups/" + gid + "/restore", null))) {
        r.expectError(404, "NOT_FOUND");
      }
    }
  }

  @Test
  @DisplayName("R-015-04 other deleted groups lists only own Owner groups with the days left")
  void R_015_04_other_ownOwnerGroupsOnly() {
    String second = group(ANA, "Druga");
    String foreign = group(BORIS, "Tuja");
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    clock.advance(Duration.ofDays(5));
    post(ANA, "/groups/" + second + "/trash", j("name", "Druga")).expect(204);
    post(BORIS, "/groups/" + foreign + "/trash", j("name", "Tuja")).expect(204);
    JsonNode d = get(ANA, "/deleted-groups").expect(200).json();
    assertThat(d).hasSize(2);
    assertThat(values(d, "name")).containsExactlyInAnyOrder("Skupina", "Druga");
    for (JsonNode g : d) {
      assertThat(g.path("daysLeft").asInt()).isIn(25, 30);
      assertThat(g.has("role")).isFalse();
    }
    assertThat(get(MARKO, "/deleted-groups").expect(200).json()).isEmpty();
  }

  @Test
  @DisplayName("R-015-05 combination restore returns members contacts tags History and invitations")
  void R_015_05_combination_everythingBack() {
    JsonNode c = contact("lastName", "A", "tags", arr("t"), "attributes", arr(attr("k", "v")));
    post(ANA, "/groups/" + gid + "/invitations", j("email", "novi@example.com", "role", "VIEWER"))
        .expect(201);
    int entries = history(ANA, gid).expect(200).at("/total").asInt();
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    post(ANA, "/groups/" + gid + "/restore", null).expect(204);
    assertThat(get(ANA, "/groups/" + gid + "/members").expect(200).json()).hasSize(3);
    assertThat(now(c).at("/attributes/0/value").asText()).isEqualTo("v");
    assertThat(get(ANA, "/groups/" + gid + "/invitations").expect(200).json()).hasSize(1);
    assertThat(history(ANA, gid).expect(200).at("/total").asInt()).isEqualTo(entries + 2);
    post(ANA, "/groups/" + gid + "/restore", null).expectError(409, "NOT_IN_TRASH");
  }

  @Test
  @DisplayName("R-015-06 combination purging deletes memberships invitations versions and History")
  void R_015_06_combination_purgeCascades() {
    JsonNode c = contact("lastName", "A", "tags", arr("t"));
    save(ANA, gid, now(c), "company", "X").expect(200);
    post(ANA, "/groups/" + gid + "/invitations", j("email", "novi@example.com", "role", "VIEWER"))
        .expect(201);
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    clock.advance(Duration.ofDays(30).plusMinutes(1));
    eventually(
        () -> jdbc.queryForObject("SELECT count(*) FROM groups", Integer.class) == 0,
        "the group is purged");
    for (String table :
        List.of(
            "memberships",
            "invitations",
            "contacts",
            "contact_versions",
            "history_entries",
            "tags",
            "contact_tags",
            "attributes")) {
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class))
          .as(table)
          .isZero();
    }
  }

  @Test
  @DisplayName("R-015-07 role non-Owner restore and delete and a non-member")
  void R_015_07_role_restoreAndDelete() {
    post(MARKO, "/groups/" + gid + "/trash", j("name", "Skupina")).expectError(403, "FORBIDDEN");
    post(PETRA, "/groups/" + gid + "/trash", j("name", "Skupina")).expectError(403, "FORBIDDEN");
    post(BORIS, "/groups/" + gid + "/trash", j("name", "Skupina")).expectError(404, "NOT_FOUND");
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    post(MARKO, "/groups/" + gid + "/restore", null).expectError(404, "NOT_FOUND");
  }

  @Test
  @DisplayName("R-015-08 other nothing in a group in Trash can be changed by an Owner")
  void R_015_08_other_ownerCannotChange() {
    JsonNode c = contact("lastName", "A");
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    for (Res r :
        List.of(
            post(ANA, "/groups/" + gid + "/contacts", j("lastName", "B")),
            put(ANA, "/groups/" + gid + "/members/" + MARKO.uid(), j("role", "VIEWER")),
            delete(ANA, "/groups/" + gid + "/invitations/" + java.util.UUID.randomUUID()),
            post(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/trash", null),
            patch(ANA, "/groups/" + gid, j("name", "Novo")))) {
      r.expectError(404, "NOT_FOUND");
    }
    post(ANA, "/groups/" + gid + "/restore", null).expect(204);
    assertThat(get(ANA, "/groups/" + gid).at("/name").asText()).isEqualTo("Skupina");
  }

  @Test
  @DisplayName("R-015-09 other the group events are recorded by the acting Owner")
  void R_015_09_other_actors() {
    join(gid, ANA, BORIS, "OWNER");
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    post(BORIS, "/groups/" + gid + "/restore", null).expect(204);
    assertThat(entries(ANA, gid, "GROUP_DELETED").get(0).at("/actor/name").asText())
        .isEqualTo("Ana Novak");
    assertThat(entries(ANA, gid, "GROUP_RESTORED").get(0).at("/actor/name").asText())
        .isEqualTo("Boris Zupan");
  }

  @Test
  @DisplayName("R-015-10 combination restoring is refused for every Owner with a clashing name")
  void R_015_10_combination_restoreClash() {
    join(gid, ANA, BORIS, "OWNER");
    post(ANA, "/groups/" + gid + "/trash", j("name", "Skupina")).expect(204);
    group(BORIS, "SKUPINA");
    post(BORIS, "/groups/" + gid + "/restore", null).expectError(409, "GROUP_NAME_TAKEN");
    post(ANA, "/groups/" + gid + "/restore", null).expectError(409, "GROUP_NAME_TAKEN");
    assertThat(get(ANA, "/deleted-groups").expect(200).json()).hasSize(1);
  }

  @Test
  @DisplayName("R-016-01 other the account of a member of two groups leaves both")
  void R_016_01_other_leavesAllGroups() {
    String second = group(BORIS, "Druga");
    join(second, BORIS, MARKO, "VIEWER");
    delete(MARKO, "/me").expect(204);
    assertThat(values(get(ANA, "/groups/" + gid + "/members").expect(200).json(), "userId"))
        .doesNotContain(MARKO.uid());
    assertThat(values(get(BORIS, "/groups/" + second + "/members").expect(200).json(), "userId"))
        .doesNotContain(MARKO.uid());
  }

  @Test
  @DisplayName(
      "R-016-02 combination a sole Owner of one group among several is refused naming only that group")
  void R_016_02_combination_namesOnlySoleGroups() {
    String sole = group(MARKO, "Moja");
    String shared = group(BORIS, "Skupna");
    join(shared, BORIS, MARKO, "OWNER");
    Res r = delete(MARKO, "/me").expectError(409, "SOLE_OWNER");
    assertThat(values(r.at("/groups"), "name")).containsExactly("Moja");
    assertThat(values(r.at("/groups"), "id")).containsExactly(sole);
  }

  @Test
  @DisplayName(
      "R-016-03 combination every kind of entry of the user loses the e-mail and keeps the name")
  void R_016_03_combination_allEntries() {
    JsonNode c = newContact(MARKO, gid, j("lastName", "A"));
    save(MARKO, gid, now(c), "tags", arr("t")).expect(200);
    post(
            MARKO,
            "/groups/" + gid + "/exports",
            j("language", "sl", "contactIds", arr(c.path("id").asText())))
        .expect(200);
    delete(MARKO, "/me").expect(204);
    String all = history(ANA, gid).expect(200).text();
    assertThat(all).doesNotContain("marko@example.com");
    for (JsonNode e : history(ANA, gid).at("/items")) {
      if ("Marko Pirc".equals(e.at("/actor/name").asText())) {
        assertThat(e.at("/actor").has("email")).isFalse();
      }
    }
    assertThat(all).contains("Marko Pirc");
  }

  @Test
  @DisplayName("R-016-04 other the contacts created by the user stay with every field")
  void R_016_04_other_contactsStay() {
    newContact(
        MARKO,
        gid,
        j("firstName", "Ana", "lastName", "Moja", "email", "moja@example.com", "tags", arr("t")));
    delete(MARKO, "/me").expect(204);
    JsonNode r = search(ANA, gid, "moja");
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    assertThat(strings(r.at("/items/0/tags"))).containsExactly("t");
  }

  @Test
  @DisplayName(
      "R-016-05 combination the deleted user leaves lists but the other members keep their roles")
  void R_016_05_combination_otherRolesKept() {
    delete(MARKO, "/me").expect(204);
    JsonNode m = get(ANA, "/groups/" + gid + "/members").expect(200).json();
    assertThat(m).hasSize(2);
    assertThat(get(PETRA, "/groups/" + gid).expect(200).at("/role").asText()).isEqualTo("VIEWER");
  }

  @Test
  @DisplayName("R-016-06 other a deleted account stays refused also after a later token")
  void R_016_06_other_staysRefused() {
    delete(PETRA, "/me").expect(204);
    clock.advance(Duration.ofHours(1));
    get(PETRA, "/groups").expectError(401, "UNAUTHENTICATED");
    clock.advance(Duration.ofDays(1));
    get(ANA, "/groups").expect(200);
  }

  @Test
  @DisplayName("R-016-07 boundary the sign-in time of exactly five minutes and just over")
  void R_016_07_boundary_fiveMinutes() {
    java.time.Instant now = clock.instant();
    String exactlyOver =
        token(
            PETRA,
            PROJECT,
            now.minusSeconds(900),
            now.plus(Duration.ofDays(1)),
            now.minusSeconds(301));
    request("DELETE", "/me", "Bearer " + exactlyOver, null)
        .expectError(403, "RECENT_LOGIN_REQUIRED");
    String inside =
        token(
            PETRA,
            PROJECT,
            now.minusSeconds(900),
            now.plus(Duration.ofDays(1)),
            now.minusSeconds(290));
    request("DELETE", "/me", "Bearer " + inside, null).expect(204);
  }

  @Test
  @DisplayName("R-019-01 other every create gives version one with the creating user")
  void R_019_01_other_versionOneByCreator() {
    JsonNode a = newContact(MARKO, gid, j("lastName", "A"));
    JsonNode v =
        get(ANA, "/groups/" + gid + "/contacts/" + a.path("id").asText() + "/versions")
            .expect(200)
            .json();
    assertThat(v).hasSize(1);
    assertThat(v.get(0).path("userName").asText()).isEqualTo("Marko Pirc");
  }

  @Test
  @DisplayName("R-019-02 combination only a real change counts: same values in another case count")
  void R_019_02_combination_realChangesOnly() {
    JsonNode c = contact("lastName", "A", "company", "Acme", "tags", arr("t"));
    save(ANA, gid, now(c), "company", "Acme", "tags", arr("T")).expect(200);
    assertThat(now(c).path("versionNumber").asInt()).isEqualTo(1);
    save(ANA, gid, now(c), "company", "ACME").expect(200);
    assertThat(now(c).path("versionNumber").asInt()).isEqualTo(2);
    save(ANA, gid, now(c), "company", "ACME  ").expect(200);
    assertThat(now(c).path("versionNumber").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("R-019-03 other deleting a tag creates no version on any holder")
  void R_019_03_other_deleteNoVersion() {
    JsonNode a = contact("lastName", "A", "tags", arr("old"));
    JsonNode b = contact("lastName", "B", "tags", arr("old"));
    delete(
            ANA,
            "/groups/"
                + gid
                + "/tags/"
                + get(ANA, "/groups/" + gid + "/tags").expect(200).at("/0/id").asText())
        .expect(204);
    assertThat(now(a).path("versionNumber").asInt()).isEqualTo(1);
    assertThat(now(b).path("versionNumber").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-019-04 combination a merge and a version restore each record exactly one version")
  void R_019_04_combination_mergeAndRestore() {
    JsonNode a = contact("lastName", "A", "email", "x@x.si", "phone", "1");
    JsonNode b = contact("lastName", "B", "email", "x@x.si", "company", "Nova");
    Res m =
        post(
                ANA,
                "/groups/" + gid + "/merges",
                j(
                    "contacts",
                    arr(
                        j(
                            "id",
                            a.path("id").asText(),
                            "revision",
                            now(a).path("revision").asLong()),
                        j(
                            "id",
                            b.path("id").asText(),
                            "revision",
                            now(b).path("revision").asLong())),
                    "fields",
                    j("lastName", "A", "email", "x@x.si", "phone", "1", "company", "Nova")))
            .expect(200);
    assertThat(m.at("/versionNumber").asInt()).isEqualTo(2);
    post(
            ANA,
            "/groups/" + gid + "/contacts/" + a.path("id").asText() + "/versions/1/restore",
            j("revision", m.at("/revision").asLong()))
        .expect(200);
    assertThat(now(a).path("versionNumber").asInt()).isEqualTo(3);
  }

  @Test
  @DisplayName("R-019-05 other numbers continue after a restore and are never reused")
  void R_019_05_other_neverReused() {
    JsonNode c = contact("lastName", "A", "phone", "1");
    save(ANA, gid, now(c), "phone", "2").expect(200);
    post(
            ANA,
            "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/versions/1/restore",
            j("revision", now(c).path("revision").asLong()))
        .expect(200);
    save(ANA, gid, now(c), "phone", "3").expect(200);
    JsonNode v =
        get(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/versions")
            .expect(200)
            .json();
    assertThat(values(v, "number")).containsExactly("4", "3", "2", "1");
  }

  @Test
  @DisplayName("R-019-06 role Owner Editor list the versions and others do not")
  void R_019_06_role_versionList() {
    JsonNode c = contact("lastName", "A");
    String p = "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/versions";
    get(MARKO, p).expect(200);
    get(PETRA, p).expectError(403, "FORBIDDEN");
    get(BORIS, p).expectError(404, "NOT_FOUND");
  }

  @Test
  @DisplayName("R-019-07 other versions of a purged group and of an erased contact are gone")
  void R_019_07_other_versionsGone() {
    JsonNode c = contact("lastName", "A");
    save(ANA, gid, now(c), "phone", "1").expect(200);
    trash(ANA, gid, c.path("id").asText());
    delete(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText()).expect(204);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM contact_versions", Integer.class))
        .isZero();
  }

  @Test
  @DisplayName("R-019-08 other 60 versions are all kept")
  void R_019_08_other_noLimit() {
    JsonNode cur = contact("lastName", "A");
    String id = cur.path("id").asText();
    cur = now(cur);
    for (int i = 0; i < 60; i++) {
      cur = save(ANA, gid, cur, "company", "F" + i).expect(200).json();
    }
    assertThat(get(ANA, "/groups/" + gid + "/contacts/" + id + "/versions").expect(200).json())
        .hasSize(61);
  }
}
