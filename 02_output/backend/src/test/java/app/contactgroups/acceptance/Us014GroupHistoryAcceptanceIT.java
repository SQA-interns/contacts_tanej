package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-014 Group-wide History: AC-014-01 to AC-014-08, EX-014-01 to EX-014-05. */
class Us014GroupHistoryAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    join(gid, ANA, BORIS, "EDITOR");
  }

  private Res filtered(Who who, String query) {
    return get(who, "/groups/" + gid + "/history" + query);
  }

  private String contact(Who who, String last) {
    return newContact(who, gid, j("lastName", last)).path("id").asText();
  }

  @Test
  void AC_014_01_aChangeByTheOwnerAppearsAtTheTop() {
    String c = contact(ANA, "Novak");
    save(ANA, gid, openContact(ANA, gid, c), "phone", "+386 41 555 123").expect(200);
    JsonNode top = history(ANA, gid).expect(200).at("/items/0");
    assertThat(top.path("eventType").asText()).isEqualTo("CONTACT_EDITED");
    assertThat(top.at("/actor/name").asText()).isEqualTo("Ana Novak");
  }

  @Test
  void AC_014_02_thePersonFilterShowsOnlyThatPersonsEntries() {
    contact(ANA, "A");
    contact(MARKO, "M");
    contact(BORIS, "B");
    JsonNode r = filtered(ANA, "?actorId=" + MARKO.uid()).expect(200).json();
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    assertThat(r.at("/items/0/actor/name").asText()).isEqualTo("Marko Pirc");
  }

  @Test
  void AC_014_03_theTypeFilterShowsOnlyThoseTypes() {
    String c = contact(ANA, "Novak");
    save(ANA, gid, openContact(ANA, gid, c), "phone", "+386 41 555 123").expect(200);
    post(ANA, "/groups/" + gid + "/exports", j("language", "sl", "contactIds", arr(c))).expect(200);
    JsonNode r = filtered(ANA, "?types=EXPORTED").expect(200).json();
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    assertThat(values(r.path("items"), "eventType")).containsOnly("EXPORTED");
  }

  @Test
  void AC_014_04_theDateRangeUsesDaysInLjubljana() {
    clockAt("2026-07-01T10:00:00Z");
    String c = contact(ANA, "Novak");
    save(ANA, gid, openContact(ANA, gid, c), "company", "Dan1").expect(200);
    clockAt("2026-07-02T10:00:00Z");
    save(ANA, gid, openContact(ANA, gid, c), "company", "Dan2").expect(200);
    clockAt("2026-07-03T10:00:00Z");
    save(ANA, gid, openContact(ANA, gid, c), "company", "Dan3").expect(200);
    JsonNode r = filtered(ANA, "?from=2026-07-02&to=2026-07-02").expect(200).json();
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    assertThat(r.at("/items/0/changes/0/newValue").asText()).isEqualTo("Dan2");
    for (JsonNode entry : r.path("items")) {
      for (JsonNode change : entry.path("changes")) {
        assertThat(change.path("newValue").asText()).isNotIn("Dan1", "Dan3");
      }
    }
  }

  @Test
  void AC_014_05_personAndTypeCombineWithAnd() {
    String a = contact(ANA, "A");
    String m = contact(MARKO, "M");
    save(MARKO, gid, openContact(MARKO, gid, m), "company", "Acme").expect(200);
    save(ANA, gid, openContact(ANA, gid, a), "company", "Acme").expect(200);
    JsonNode r =
        filtered(ANA, "?actorId=" + MARKO.uid() + "&types=CONTACT_EDITED").expect(200).json();
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    assertThat(r.at("/items/0/actor/name").asText()).isEqualTo("Marko Pirc");
    assertThat(r.at("/items/0/eventType").asText()).isEqualTo("CONTACT_EDITED");
  }

  @Test
  void AC_014_06_aFilterWithoutAMatchGivesAnEmptyPage() {
    contact(ANA, "A");
    JsonNode r = filtered(ANA, "?types=GROUP_DELETED").expect(200).json();
    assertThat(r.at("/total").asInt()).isZero();
    assertThat(r.path("items")).isEmpty();
    assertThat(filtered(ANA, "?types=CONTACT_CREATED").expect(200).at("/total").asInt())
        .isEqualTo(1);
  }

  @Test
  void AC_014_07_aViewerHasNoGroupHistory() {
    filtered(PETRA, "").expectError(403, "FORBIDDEN");
    get(PETRA, "/groups/" + gid + "/history/actors").expectError(403, "FORBIDDEN");
  }

  @Test
  void AC_014_08_onlyEntriesOfTheOwnGroupAreShown() {
    contact(ANA, "InA");
    String other = group(ANA, "Druga skupina");
    newContact(ANA, other, j("lastName", "InB"));
    JsonNode r = filtered(ANA, "?types=CONTACT_CREATED").expect(200).json();
    assertThat(r.toString()).doesNotContain("InB");
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    JsonNode b = history(ANA, other).expect(200).json();
    assertThat(b.toString()).doesNotContain("InA");
    assertThat(b.at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void EX_014_01_anEntryAt2330UtcBelongsToTheNextDayInLjubljana() {
    clockAt("2026-07-01T23:30:00Z");
    contact(ANA, "Novak");
    assertThat(filtered(ANA, "?from=2026-07-02&to=2026-07-02").expect(200).at("/total").asInt())
        .isEqualTo(1);
    assertThat(filtered(ANA, "?from=2026-07-01&to=2026-07-01").expect(200).at("/total").asInt())
        .isZero();
  }

  @Test
  void EX_014_02_aRangeWithTheEndBeforeTheStartIsRefused() {
    contact(ANA, "Novak");
    filtered(ANA, "?from=2026-07-03&to=2026-07-02").expectFieldCode("INVALID_DATE_RANGE");
  }

  @Test
  void EX_014_03_aPersonWhoLeftIsStillSelectableWhileEntriesExist() {
    contact(MARKO, "M");
    post(MARKO, "/groups/" + gid + "/leave", null).expect(204);
    JsonNode actors = get(ANA, "/groups/" + gid + "/history/actors").expect(200).json();
    assertThat(values(actors, "id")).contains(MARKO.uid());
    JsonNode byMarko = filtered(ANA, "?actorId=" + MARKO.uid()).expect(200).json();
    assertThat(byMarko.at("/total").asInt()).isEqualTo(2);
    assertThat(values(byMarko.path("items"), "eventType")).contains("MEMBER_REMOVED");
  }

  @Test
  void EX_014_04_25EntriesAreOnePageAnd26AreTwo() {
    List<String> ids = bulkContacts(ANA, gid, 25, i -> j("lastName", "P" + i));
    assertThat(ids).hasSize(25);
    JsonNode base = filtered(ANA, "?types=CONTACT_CREATED").expect(200).json();
    assertThat(base.at("/total").asInt()).isEqualTo(25);
    assertThat(base.path("items")).hasSize(25);
    contact(ANA, "Zadnji");
    JsonNode p2 = filtered(ANA, "?types=CONTACT_CREATED&page=2").expect(200).json();
    assertThat(p2.at("/total").asInt()).isEqualTo(26);
    assertThat(p2.path("items")).hasSize(1);
  }

  @Test
  void EX_014_05_aGroupWithoutContactEventsHasNoContactEntries() {
    JsonNode r = filtered(ANA, "?types=CONTACT_CREATED,CONTACT_EDITED").expect(200).json();
    assertThat(r.at("/total").asInt()).isZero();
    assertThat(r.path("items")).isEmpty();
  }
}
