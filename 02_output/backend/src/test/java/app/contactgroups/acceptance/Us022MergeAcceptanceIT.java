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

  @Test
  void EX_022_01_withThreeContactsTheOldestSurvivesAndTwoGoToTrash() {
    JsonNode a = contact("lastName", "Novak", "email", "x@example.com");
    JsonNode b = contact("lastName", "Novak", "email", "x@example.com");
    JsonNode c = contact("lastName", "Novak", "email", "x@example.com");
    merge(
            MARKO,
            List.of(pick(c), pick(a), pick(b)),
            chosen("lastName", "Novak", "email", "x@example.com"),
            null)
        .expect(200)
        .at("/id");
    assertThat(now(a).path("state").asText()).isEqualTo("ACTIVE");
    assertThat(now(b).path("state").asText()).isEqualTo("TRASHED");
    assertThat(now(c).path("state").asText()).isEqualTo("TRASHED");
    assertThat(search(MARKO, gid, "").at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void EX_022_02_sameKeyAndSameValueNeedsNoChoiceAndGivesOneAttribute() {
    JsonNode a =
        contact(
            "lastName",
            "Novak",
            "email",
            "x@example.com",
            "attributes",
            arr(attr("kompetenca", "java")));
    JsonNode b =
        contact(
            "lastName",
            "Novak",
            "email",
            "x@example.com",
            "attributes",
            arr(attr("Kompetenca", "java")));
    Res r =
        merge(
                MARKO,
                List.of(pick(a), pick(b)),
                chosen("lastName", "Novak", "email", "x@example.com"),
                null)
            .expect(200);
    assertThat(r.at("/attributes")).hasSize(1);
    assertThat(r.at("/attributes/0/value").asText()).isEqualTo("java");
  }

  @Test
  void EX_022_03_withTheSameCreationTimeTheContactWithTheLowerIdSurvives() {
    JsonNode a = contact("lastName", "Novak", "email", "x@example.com");
    JsonNode b = contact("lastName", "Novak", "email", "x@example.com");
    jdbc.update("UPDATE contacts SET created_at = (SELECT min(created_at) FROM contacts)");
    String lower =
        jdbc.queryForObject("SELECT id::text FROM contacts ORDER BY id LIMIT 1", String.class);
    Res r =
        merge(
                MARKO,
                List.of(pick(a), pick(b)),
                chosen("lastName", "Novak", "email", "x@example.com"),
                null)
            .expect(200);
    assertThat(r.at("/id").asText()).isEqualTo(lower);
  }

  @Test
  void EX_022_04_aMergedAwayContactNotRestoredWithin30DaysIsErased() {
    JsonNode[] p = pair();
    merge(MARKO, List.of(pick(p[0]), pick(p[1])), chosen("lastName", "Novak"), null).expect(200);
    clock.advance(Duration.ofDays(30).plusMinutes(1));
    eventually(
        () ->
            get(MARKO, "/groups/" + gid + "/contacts/" + p[1].path("id").asText()).status() == 404,
        "the merged-away contact is erased by the cleanup");
    assertThat(now(p[0]).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void EX_022_05_theSameContactTwiceIsRefused() {
    JsonNode a = contact("lastName", "Novak");
    merge(MARKO, List.of(pick(a), pick(a)), chosen("lastName", "Novak"), null).expect(400);
    assertThat(now(a).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void EX_022_06_aContactAlreadyInTrashIsRefused() {
    JsonNode[] p = pair();
    ObjectNode a = pick(p[0]);
    ObjectNode b = pick(p[1]);
    trash(MARKO, gid, p[1].path("id").asText());
    merge(MARKO, List.of(a, b), chosen("lastName", "Novak"), null).expect(400);
    assertThat(now(p[0]).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void EX_022_07_aChosenValueThatBreaksTheRulesIsRefusedByTheDataRules() {
    JsonNode[] p = pair();
    merge(
            MARKO,
            List.of(pick(p[0]), pick(p[1])),
            chosen("lastName", "Novak", "email", "ana@"),
            null)
        .expectFieldError("email", "INVALID_FORMAT");
    assertThat(now(p[1]).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void EX_022_08_chosenValuesEqualToTheSurvivorsStillConfirmAndRecordNoVersion() {
    JsonNode a = contact("firstName", "Ana", "lastName", "Novak", "email", "x@example.com");
    JsonNode b = contact("lastName", "Nova", "email", "x@example.com");
    int version = now(a).path("versionNumber").asInt();
    Res r =
        merge(
                MARKO,
                List.of(pick(a), pick(b)),
                chosen("firstName", "Ana", "lastName", "Novak", "email", "x@example.com"),
                null)
            .expect(200);
    assertThat(r.at("/versionNumber").asInt()).isEqualTo(version);
    assertThat(now(b).path("state").asText()).isEqualTo("TRASHED");
  }

  @Test
  void AC_022_03_aKeyWithDifferentValuesNeedsAChoiceBeforeTheServerConfirms() {
    JsonNode a =
        contact(
            "lastName",
            "Novak",
            "email",
            "x@example.com",
            "attributes",
            arr(attr("kompetenca", "java")));
    JsonNode b =
        contact(
            "lastName",
            "Novak",
            "email",
            "x@example.com",
            "attributes",
            arr(attr("Kompetenca", "python")));
    Res without =
        merge(
            MARKO,
            List.of(pick(a), pick(b)),
            chosen("lastName", "Novak", "email", "x@example.com"),
            null);
    without.expectError(400, "VALIDATION_FAILED");
    assertThat(without.text()).contains("ATTRIBUTE_CHOICE_REQUIRED");
    Res with =
        merge(
                MARKO,
                List.of(pick(a), pick(b)),
                chosen("lastName", "Novak", "email", "x@example.com"),
                arr(attr("kompetenca", "python")))
            .expect(200);
    assertThat(with.at("/attributes")).hasSize(1);
    assertThat(with.at("/attributes/0/value").asText()).isEqualTo("python");
    assertThat(UUID.fromString(with.at("/id").asText())).isNotNull();
  }
}
