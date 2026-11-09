package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * US-022 Merge duplicates (API part): AC-022-02, 04, 05, 06, 08 to 12, 14 and EX-022-01 to
 * EX-022-08.
 */
class Us022MergeAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    join(gid, ANA, BORIS, "EDITOR");
  }

  private JsonNode contact(Object... pairs) {
    return newContact(MARKO, gid, j(pairs));
  }

  private JsonNode now(JsonNode c) {
    return openContact(MARKO, gid, c.path("id").asText());
  }

  private ObjectNode pick(JsonNode c) {
    JsonNode cur = now(c);
    return j("id", cur.path("id").asText(), "revision", cur.path("revision").asLong());
  }

  private Res merge(
      Who who, List<ObjectNode> contacts, ObjectNode fields, JsonNode attributeChoices) {
    ObjectNode body = j("contacts", arr(contacts.toArray()), "fields", fields);
    if (attributeChoices != null) {
      body.set("attributeChoices", attributeChoices);
    }
    return post(who, "/groups/" + gid + "/merges", body);
  }

  /** A (older): phone empty, tag a. B (newer): phone 041, tag b. Same e-mail. */
  private JsonNode[] pair() {
    JsonNode a =
        contact(
            "firstName", "Ana", "lastName", "Novak", "email", "ana@example.com", "tags", arr("a"));
    JsonNode b =
        contact(
            "firstName",
            "Ana",
            "lastName",
            "Novak",
            "email",
            "ana@example.com",
            "phone",
            "+386 41 555 123",
            "tags",
            arr("b"));
    return new JsonNode[] {a, b};
  }

  private static ObjectNode chosen(Object... pairs) {
    return j(pairs);
  }

  @Test
  void AC_022_02_confirmingKeepsTheSurvivorWithTheChosenValuesAndCombinedTags() {
    JsonNode[] p = pair();
    int versionBefore = now(p[0]).path("versionNumber").asInt();
    Res r =
        merge(
                MARKO,
                List.of(pick(p[0]), pick(p[1])),
                chosen(
                    "firstName",
                    "Ana",
                    "lastName",
                    "Novak",
                    "email",
                    "ana@example.com",
                    "phone",
                    "+386 41 555 123"),
                null)
            .expect(200);
    assertThat(r.at("/id").asText()).isEqualTo(p[0].path("id").asText());
    assertThat(r.at("/phone").asText()).isEqualTo("+386 41 555 123");
    assertThat(strings(r.at("/tags"))).containsExactlyInAnyOrder("a", "b");
    assertThat(r.at("/versionNumber").asInt()).isEqualTo(versionBefore + 1);
    assertThat(
            get(MARKO, "/groups/" + gid + "/contacts/" + p[1].path("id").asText())
                .at("/state")
                .asText())
        .isEqualTo("TRASHED");
    List<JsonNode> merged = entries(ANA, gid, "CONTACTS_MERGED");
    assertThat(merged).hasSize(1);
    assertThat(merged.get(0).toString()).contains("+386 41 555 123");
  }

  @Test
  void AC_022_04_theSurvivorsHistoryAlsoShowsTheMergedAwayContactsHistoryLinked() {
    JsonNode[] p = pair();
    merge(
            MARKO,
            List.of(pick(p[0]), pick(p[1])),
            chosen("firstName", "Ana", "lastName", "Novak"),
            null)
        .expect(200);
    JsonNode items = contactHistory(MARKO, gid, p[0].path("id").asText()).expect(200).at("/items");
    boolean linked = false;
    for (JsonNode e : items) {
      linked |= p[1].path("id").asText().equals(e.path("linkedFrom").asText());
    }
    assertThat(linked).as(items.toString()).isTrue();
  }

  @Test
  void AC_022_05_restoringTheMergedAwayContactBringsOnlyThatContactBack() {
    JsonNode[] p = pair();
    merge(
            MARKO,
            List.of(pick(p[0]), pick(p[1])),
            chosen("firstName", "Ana", "lastName", "Novak", "phone", "+386 41 555 123"),
            null)
        .expect(200);
    JsonNode survivor = now(p[0]);
    post(MARKO, "/groups/" + gid + "/contacts/" + p[1].path("id").asText() + "/restore", null)
        .expect(204);
    JsonNode b = now(p[1]);
    assertThat(b.path("state").asText()).isEqualTo("ACTIVE");
    assertThat(b.path("phone").asText()).isEqualTo("+386 41 555 123");
    assertThat(strings(b.path("tags"))).containsExactly("b");
    JsonNode after = now(p[0]);
    assertThat(after.path("revision").asLong()).isEqualTo(survivor.path("revision").asLong());
    assertThat(after.path("versionNumber").asInt())
        .isEqualTo(survivor.path("versionNumber").asInt());
  }

  @Test
  void AC_022_06_restoringTheSurvivorsEarlierVersionBringsBackThePreMergeValues() {
    JsonNode[] p = pair();
    Res m =
        merge(
                MARKO,
                List.of(pick(p[0]), pick(p[1])),
                chosen(
                    "firstName",
                    "Ana",
                    "lastName",
                    "Novak",
                    "email",
                    "ana@example.com",
                    "phone",
                    "+386 41 555 123"),
                null)
            .expect(200);
    int pre = m.at("/versionNumber").asInt() - 1;
    post(
            MARKO,
            "/groups/"
                + gid
                + "/contacts/"
                + p[0].path("id").asText()
                + "/versions/"
                + pre
                + "/restore",
            j("revision", m.at("/revision").asLong()))
        .expect(200);
    JsonNode a = now(p[0]);
    assertThat(a.path("phone").asText()).isEmpty();
    assertThat(strings(a.path("tags"))).containsExactly("a");
  }

  @Test
  void AC_022_08_aMergedResultOverTheLimitOf50TagsIsRefusedAndNothingChanges() {
    List<String> first = new ArrayList<>();
    List<String> second = new ArrayList<>();
    for (int i = 1; i <= 30; i++) {
      first.add("a" + i);
      second.add("b" + i);
    }
    JsonNode a =
        contact("lastName", "Novak", "email", "x@example.com", "tags", arr(first.toArray()));
    JsonNode b =
        contact("lastName", "Novak", "email", "x@example.com", "tags", arr(second.toArray()));
    merge(
            MARKO,
            List.of(pick(a), pick(b)),
            chosen("lastName", "Novak", "email", "x@example.com"),
            null)
        .expectFieldErrorStarting("tags", "TOO_MANY");
    assertThat(now(a).path("tags")).hasSize(30);
    assertThat(now(b).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void AC_022_09_aMergeOfOneContactIsRefused() {
    JsonNode a = contact("lastName", "Novak");
    merge(MARKO, List.of(pick(a)), chosen("lastName", "Novak"), null).expect(400);
    assertThat(now(a).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void AC_022_10_aContactOfAnotherGroupGivesNotFoundAndChangesNothing() {
    String other = group(BORIS, "Poroka");
    JsonNode foreign = newContact(BORIS, other, j("lastName", "Tuj"));
    JsonNode a = contact("lastName", "Novak");
    merge(
            MARKO,
            List.of(
                pick(a),
                j(
                    "id",
                    foreign.path("id").asText(),
                    "revision",
                    foreign.path("revision").asLong())),
            chosen("lastName", "Novak"),
            null)
        .expectError(404, "NOT_FOUND");
    assertThat(now(a).path("state").asText()).isEqualTo("ACTIVE");
    assertThat(openContact(BORIS, other, foreign.path("id").asText()).path("state").asText())
        .isEqualTo("ACTIVE");
  }

  @Test
  void AC_022_11_aChangeBySomeoneElseSinceOpeningMakesTheMergeAConflict() {
    JsonNode[] p = pair();
    ObjectNode a = pick(p[0]);
    ObjectNode b = pick(p[1]);
    save(BORIS, gid, now(p[1]), "company", "Acme").expect(200);
    merge(MARKO, List.of(a, b), chosen("lastName", "Novak"), null).expectError(409, "CONFLICT");
    assertThat(now(p[0]).path("state").asText()).isEqualTo("ACTIVE");
    assertThat(now(p[1]).path("state").asText()).isEqualTo("ACTIVE");
    assertThat(now(p[1]).path("company").asText()).isEqualTo("Acme");
  }

  @Test
  void AC_022_12_aViewerCannotMerge() {
    JsonNode[] p = pair();
    merge(PETRA, List.of(pick(p[0]), pick(p[1])), chosen("lastName", "Novak"), null)
        .expectError(403, "FORBIDDEN");
    assertThat(now(p[1]).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void AC_022_14_findingDuplicatesMergesNothing() {
    JsonNode[] p = pair();
    post(MARKO, "/groups/" + gid + "/duplicates/search", null).expect(200);
    assertThat(now(p[0]).path("state").asText()).isEqualTo("ACTIVE");
    assertThat(now(p[1]).path("state").asText()).isEqualTo("ACTIVE");
    assertThat(search(MARKO, gid, "").at("/total").asInt()).isEqualTo(2);
  }
}
