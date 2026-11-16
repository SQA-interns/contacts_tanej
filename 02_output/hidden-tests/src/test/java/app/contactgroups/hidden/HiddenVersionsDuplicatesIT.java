package app.contactgroups.hidden;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases for R-020 (versions), R-021 (duplicates) and R-022 (merge). */
class HiddenVersionsDuplicatesIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Skupina");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private JsonNode contact(Object... pairs) {
    return newContact(MARKO, gid, j(pairs));
  }

  private JsonNode now(JsonNode c) {
    return openContact(ANA, gid, c.path("id").asText());
  }

  private String base(JsonNode c) {
    return "/groups/" + gid + "/contacts/" + c.path("id").asText();
  }

  private ObjectNode pick(JsonNode c) {
    return j("id", c.path("id").asText(), "revision", now(c).path("revision").asLong());
  }

  private Res merge(List<ObjectNode> picks, ObjectNode fields, JsonNode choices) {
    ObjectNode body = j("contacts", arr(picks.toArray()), "fields", fields);
    if (choices != null) {
      body.set("attributeChoices", choices);
    }
    return post(MARKO, "/groups/" + gid + "/merges", body);
  }

  private JsonNode sets() {
    return post(MARKO, "/groups/" + gid + "/duplicates/search", null).expect(200).at("/sets");
  }

  @Test
  @DisplayName(
      "R-020-01 other the list holds number author and time and works for any version count")
  void R_020_01_other_listShape() {
    JsonNode c = contact("lastName", "A", "phone", "1");
    save(ANA, gid, now(c), "phone", "2").expect(200);
    JsonNode v = get(MARKO, base(c) + "/versions").expect(200).json();
    assertThat(v.get(0).path("userName").asText()).isEqualTo("Ana Novak");
    assertThat(v.get(1).path("userName").asText()).isEqualTo("Marko Pirc");
    assertThat(v.get(0).path("createdAt").asText()).isNotBlank();
  }

  @Test
  @DisplayName("R-020-02 invalid version numbers zero negative and text")
  void R_020_02_invalid_numbers() {
    JsonNode c = contact("lastName", "A");
    for (String n : List.of("0", "-1", "x", "1.5", "2147483648")) {
      assertThat(get(MARKO, base(c) + "/versions/" + n).status()).as(n).isIn(400, 404);
    }
    get(MARKO, base(c) + "/versions/1").expect(200);
  }

  @Test
  @DisplayName("R-020-03 other the snapshot has all fields tags and attributes of that version")
  void R_020_03_other_snapshotComplete() {
    JsonNode c =
        contact(
            "firstName",
            "Ana",
            "lastName",
            "A",
            "city",
            "Celje",
            "country",
            "SI",
            "tags",
            arr("t1"),
            "attributes",
            arr(attr("k", "v")));
    save(ANA, gid, now(c), "city", "Maribor", "tags", arr("t2"), "attributes", arr()).expect(200);
    JsonNode s = get(MARKO, base(c) + "/versions/1").expect(200).at("/snapshot");
    assertThat(s.path("city").asText()).isEqualTo("Celje");
    assertThat(s.path("country").asText()).isEqualTo("SI");
    assertThat(strings(s.path("tags"))).containsExactly("t1");
    assertThat(s.at("/attributes/0/value").asText()).isEqualTo("v");
  }

  @Test
  @DisplayName(
      "R-020-04 combination restore recreates a tag that was deleted and keeps the case of the group tag")
  void R_020_04_combination_recreatesTag() {
    JsonNode c = contact("lastName", "A", "tags", arr("Stara"));
    save(ANA, gid, now(c), "tags", arr("nova")).expect(200);
    String tagId =
        get(ANA, "/groups/" + gid + "/tags").expect(200).json().get(0).path("id").asText();
    assertThat(tagId).isNotBlank();
    post(MARKO, base(c) + "/versions/1/restore", j("revision", now(c).path("revision").asLong()))
        .expect(200);
    assertThat(strings(now(c).path("tags"))).containsExactly("Stara");
    assertThat(values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactly("Stara");
    assertThat(entries(ANA, gid, "VERSION_RESTORED").get(0).toString()).contains("tag");
  }

  @Test
  @DisplayName("R-020-05 other a restore for a contact in Trash leaves everything unchanged")
  void R_020_05_other_trashedUnchanged() {
    JsonNode c = contact("lastName", "A", "phone", "1");
    save(ANA, gid, now(c), "phone", "2").expect(200);
    long rev = now(c).path("revision").asLong();
    trash(ANA, gid, c.path("id").asText());
    post(MARKO, base(c) + "/versions/1/restore", j("revision", rev))
        .expectError(409, "CONTACT_IN_TRASH");
    assertThat(now(c).path("phone").asText()).isEqualTo("2");
    assertThat(get(ANA, base(c) + "/versions").expect(200).json()).hasSize(2);
  }

  @Test
  @DisplayName(
      "R-020-06 boundary only a difference in a tag or an attribute makes a restore possible")
  void R_020_06_boundary_tagOnlyDifference() {
    JsonNode c = contact("lastName", "A", "tags", arr("a"));
    save(ANA, gid, now(c), "tags", arr("a", "b")).expect(200);
    post(MARKO, base(c) + "/versions/1/restore", j("revision", now(c).path("revision").asLong()))
        .expect(200);
    post(MARKO, base(c) + "/versions/3/restore", j("revision", now(c).path("revision").asLong()))
        .expectError(409, "NOTHING_TO_CHANGE");
    post(MARKO, base(c) + "/versions/1/restore", j("revision", now(c).path("revision").asLong()))
        .expect(409);
  }

  @Test
  @DisplayName("R-020-07 role restore and view by a Viewer and a stranger")
  void R_020_07_role_viewerAndStranger() {
    JsonNode c = contact("lastName", "A", "phone", "1");
    save(ANA, gid, now(c), "phone", "2").expect(200);
    long rev = now(c).path("revision").asLong();
    post(PETRA, base(c) + "/versions/1/restore", j("revision", rev)).expectError(403, "FORBIDDEN");
    post(BORIS, base(c) + "/versions/1/restore", j("revision", rev)).expectError(404, "NOT_FOUND");
    assertThat(now(c).path("phone").asText()).isEqualTo("2");
  }

  @Test
  @DisplayName(
      "R-020-08 other a missing or old revision is a conflict or a validation error and changes nothing")
  void R_020_08_other_missingRevision() {
    JsonNode c = contact("lastName", "A", "phone", "1");
    save(ANA, gid, now(c), "phone", "2").expect(200);
    Res none = post(MARKO, base(c) + "/versions/1/restore", j());
    assertThat(none.status()).isIn(400, 409);
    post(MARKO, base(c) + "/versions/1/restore", j("revision", 1)).expectError(409, "CONFLICT");
    assertThat(now(c).path("phone").asText()).isEqualTo("2");
  }

  @Test
  @DisplayName(
      "R-020-09 other a restore needs the explicit call and a read of versions changes nothing")
  void R_020_09_other_readsChangeNothing() {
    JsonNode c = contact("lastName", "A", "phone", "1");
    save(ANA, gid, now(c), "phone", "2").expect(200);
    get(MARKO, base(c) + "/versions").expect(200);
    get(MARKO, base(c) + "/versions/1").expect(200);
    assertThat(now(c).path("phone").asText()).isEqualTo("2");
    assertThat(get(ANA, base(c) + "/versions").expect(200).json()).hasSize(2);
  }

  @Test
  @DisplayName("R-021-01 other duplicates of a group of 60 are all found whatever the sort")
  void R_021_01_other_manyContacts() {
    for (int i = 0; i < 20; i++) {
      contact("lastName", "A" + i, "email", "dup" + i + "@x.si");
      contact("lastName", "B" + i, "email", "DUP" + i + "@x.si");
      contact("lastName", "C" + i, "email", "single" + i + "@x.si");
    }
    JsonNode s = sets();
    assertThat(s).hasSize(20);
    for (JsonNode set : s) {
      assertThat(set.path("contacts")).hasSize(2);
    }
  }

  @Test
  @DisplayName("R-021-02 boundary e-mail with spaces and different case")
  void R_021_02_boundary_caseOnly() {
    contact("lastName", "A", "email", "Ana@Primer.SI");
    contact("lastName", "B", "email", "ana@primer.si");
    contact("lastName", "C", "email", "ana@primer.si.");
    contact("lastName", "D", "email", "ana2@primer.si");
    assertThat(sets()).hasSize(1);
    assertThat(sets().get(0).path("contacts")).hasSize(2);
  }

  @Test
  @DisplayName(
      "R-021-03 combination name and company and name and phone together give both reasons")
  void R_021_03_combination_bothReasons() {
    contact("firstName", "Ana", "lastName", "Novak", "company", "Acme", "phone", "+386 41 555 123");
    contact("firstName", "ANA", "lastName", "NOVAK", "company", "ACME", "phone", "+386 41 555 123");
    JsonNode s = sets();
    assertThat(s).hasSize(1);
    assertThat(strings(s.get(0).path("reasons")))
        .containsExactlyInAnyOrder("SAME_NAME_AND_COMPANY", "SAME_NAME_AND_PHONE");
    assertThat(s.get(0).path("kind").asText()).isEqualTo("PROBABLE");
  }

  @Test
  @DisplayName("R-021-04 invalid names with a different first name or last name are not reported")
  void R_021_04_invalid_partialNames() {
    contact("firstName", "Ana", "lastName", "Novak", "company", "Acme");
    contact("firstName", "Anja", "lastName", "Novak", "company", "Acme");
    contact("firstName", "Ana", "lastName", "Novakova", "company", "Acme");
    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("R-021-05 other a contact with only a city or a job title never matches")
  void R_021_05_other_onlyOtherFields() {
    contact("city", "Celje", "jobTitle", "Direktor", "phone", "1");
    contact("city", "Celje", "jobTitle", "Direktor", "phone", "1");
    contact("notes", "isto", "phone", "2");
    contact("notes", "isto", "phone", "2");
    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("R-021-06 other the same data in two groups of the same user never forms a set")
  void R_021_06_other_twoOwnGroups() {
    String other = group(ANA, "Druga");
    newContact(
        ANA,
        other,
        j("lastName", "Novak", "firstName", "Ana", "email", "x@x.si", "company", "Acme"));
    contact("lastName", "Novak", "firstName", "Ana", "email", "x@x.si", "company", "Acme");
    assertThat(sets()).isEmpty();
  }

  @Test
  @DisplayName("R-021-07 other every set lists id and createdAt of its contacts with the kind")
  void R_021_07_other_setShape() {
    JsonNode a = contact("lastName", "A", "email", "x@x.si");
    contact("lastName", "B", "email", "x@x.si");
    JsonNode s = sets().get(0);
    for (JsonNode c : s.path("contacts")) {
      assertThat(c.path("createdAt").asText()).isNotBlank();
      assertThat(c.path("id").asText()).isNotBlank();
    }
    assertThat(s.path("kind").asText()).isEqualTo("CERTAIN");
    assertThat(a).isNotNull();
  }

  @Test
  @DisplayName("R-021-08 combination searching writes no History no version and no revision change")
  void R_021_08_combination_readOnly() {
    JsonNode a = contact("lastName", "A", "email", "x@x.si");
    contact("lastName", "B", "email", "x@x.si");
    int entries = history(ANA, gid).expect(200).at("/total").asInt();
    long revision = now(a).path("revision").asLong();
    sets();
    sets();
    assertThat(history(ANA, gid).expect(200).at("/total").asInt()).isEqualTo(entries);
    assertThat(now(a).path("revision").asLong()).isEqualTo(revision);
    assertThat(now(a).path("versionNumber").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-021-09 combination a dismissal by order of ids and by another user")
  void R_021_09_combination_orderAndUser() {
    JsonNode a = contact("lastName", "A", "email", "x@x.si");
    JsonNode b = contact("lastName", "B", "email", "x@x.si");
    post(
            ANA,
            "/groups/" + gid + "/duplicates/dismissals",
            j("contactIds", arr(b.path("id").asText(), a.path("id").asText())))
        .expect(204);
    assertThat(sets()).isEmpty();
    assertThat(post(ANA, "/groups/" + gid + "/duplicates/search", null).expect(200).at("/sets"))
        .isEmpty();
  }

  @Test
  @DisplayName("R-021-10 combination dismissing a smaller set does not hide a larger one")
  void R_021_10_combination_subsetNotHidden() {
    JsonNode a = contact("lastName", "A", "email", "x@x.si");
    JsonNode b = contact("lastName", "B", "email", "x@x.si");
    JsonNode c = contact("lastName", "C", "email", "x@x.si");
    post(
            MARKO,
            "/groups/" + gid + "/duplicates/dismissals",
            j("contactIds", arr(a.path("id").asText(), b.path("id").asText())))
        .expect(204);
    JsonNode s = sets();
    assertThat(s).hasSize(1);
    assertThat(s.get(0).path("contacts")).hasSize(3);
    assertThat(c).isNotNull();
  }

  @Test
  @DisplayName("R-021-11 invalid a dismissal with one id or an unknown id is refused")
  void R_021_11_invalid_dismissals() {
    JsonNode a = contact("lastName", "A", "email", "x@x.si");
    post(
            MARKO,
            "/groups/" + gid + "/duplicates/dismissals",
            j("contactIds", arr(a.path("id").asText())))
        .expect(400);
    post(
            PETRA,
            "/groups/" + gid + "/duplicates/dismissals",
            j("contactIds", arr(a.path("id").asText(), UUID.randomUUID().toString())))
        .expectError(403, "FORBIDDEN");
    post(
            BORIS,
            "/groups/" + gid + "/duplicates/dismissals",
            j("contactIds", arr(a.path("id").asText(), UUID.randomUUID().toString())))
        .expectError(404, "NOT_FOUND");
  }

  @Test
  @DisplayName("R-021-12 other an empty group and a group of one contact give no sets")
  void R_021_12_other_noSets() {
    assertThat(sets()).isEmpty();
    contact("lastName", "A", "email", "x@x.si");
    assertThat(sets()).isEmpty();
  }

  private JsonNode[] pair() {
    JsonNode a =
        contact(
            "firstName",
            "Ana",
            "lastName",
            "Novak",
            "email",
            "x@x.si",
            "company",
            "Acme",
            "tags",
            arr("a"),
            "attributes",
            arr(attr("k1", "1")));
    JsonNode b =
        contact(
            "firstName",
            "Ana",
            "lastName",
            "Novak",
            "email",
            "x@x.si",
            "phone",
            "+386 41 555 123",
            "tags",
            arr("B", "A"),
            "attributes",
            arr(attr("K2", "2")));
    return new JsonNode[] {a, b};
  }

  @Test
  @DisplayName(
      "R-022-01 other a merge needs the picked contacts of one group and takes the oldest as survivor")
  void R_022_01_other_survivorIsOldest() {
    JsonNode[] p = pair();
    Res r =
        merge(List.of(pick(p[1]), pick(p[0])), j("lastName", "Novak", "email", "x@x.si"), null)
            .expect(200);
    assertThat(r.at("/id").asText()).isEqualTo(p[0].path("id").asText());
  }

  @Test
  @DisplayName("R-022-02 combination the chosen field values come from any picked contact")
  void R_022_02_combination_chosenValues() {
    JsonNode[] p = pair();
    Res r =
        merge(
                List.of(pick(p[0]), pick(p[1])),
                j(
                    "firstName",
                    "Ana",
                    "lastName",
                    "Novak",
                    "email",
                    "x@x.si",
                    "company",
                    "Izbrano",
                    "phone",
                    "+386 41 555 123",
                    "city",
                    "Celje"),
                null)
            .expect(200);
    assertThat(r.at("/company").asText()).isEqualTo("Izbrano");
    assertThat(r.at("/city").asText()).isEqualTo("Celje");
    assertThat(r.at("/phone").asText()).isEqualTo("+386 41 555 123");
  }

  @Test
  @DisplayName("R-022-03 boundary tags that differ only in case are combined once")
  void R_022_03_boundary_tagsOnce() {
    JsonNode[] p = pair();
    Res r =
        merge(List.of(pick(p[0]), pick(p[1])), j("lastName", "Novak", "email", "x@x.si"), null)
            .expect(200);
    assertThat(strings(r.at("/tags"))).hasSize(2);
    assertThat(strings(r.at("/tags")).stream().map(String::toLowerCase).distinct().count())
        .isEqualTo(2);
  }

  @Test
  @DisplayName(
      "R-022-04 combination different keys are combined and a conflicting key needs the chosen value")
  void R_022_04_combination_differentKeys() {
    JsonNode a =
        contact(
            "lastName", "N", "email", "x@x.si", "attributes", arr(attr("a", "1"), attr("c", "x")));
    JsonNode b =
        contact(
            "lastName", "N", "email", "x@x.si", "attributes", arr(attr("b", "2"), attr("C", "y")));
    merge(List.of(pick(a), pick(b)), j("lastName", "N", "email", "x@x.si"), null)
        .expectError(400, "VALIDATION_FAILED");
    Res r =
        merge(List.of(pick(a), pick(b)), j("lastName", "N", "email", "x@x.si"), arr(attr("c", "y")))
            .expect(200);
    assertThat(values(r.at("/attributes"), "key")).containsExactlyInAnyOrder("a", "b", "c");
    assertThat(values(r.at("/attributes"), "value")).containsExactlyInAnyOrder("1", "2", "y");
  }

  @Test
  @DisplayName(
      "R-022-05 combination one history entry one version and the others in Trash for three contacts")
  void R_022_05_combination_threeContacts() {
    JsonNode a = contact("lastName", "N", "email", "x@x.si", "phone", "1");
    JsonNode b = contact("lastName", "N", "email", "x@x.si", "company", "B");
    JsonNode c = contact("lastName", "N", "email", "x@x.si", "city", "C");
    int before = entries(ANA, gid, "CONTACTS_MERGED").size();
    Res r =
        merge(
                List.of(pick(a), pick(b), pick(c)),
                j("lastName", "N", "email", "x@x.si", "phone", "1", "company", "B", "city", "C"),
                null)
            .expect(200);
    assertThat(entries(ANA, gid, "CONTACTS_MERGED")).hasSize(before + 1);
    assertThat(r.at("/versionNumber").asInt()).isEqualTo(2);
    assertThat(now(b).path("state").asText()).isEqualTo("TRASHED");
    assertThat(now(c).path("state").asText()).isEqualTo("TRASHED");
  }

  @Test
  @DisplayName(
      "R-022-06 other the merged-away contact gets its history linked also after restoring it")
  void R_022_06_other_linkedHistory() {
    JsonNode[] p = pair();
    save(MARKO, gid, now(p[1]), "city", "Celje").expect(200);
    merge(List.of(pick(p[0]), pick(p[1])), j("lastName", "Novak", "email", "x@x.si"), null)
        .expect(200);
    String h = get(ANA, base(p[0]) + "/history").expect(200).text();
    assertThat(h).contains(p[1].path("id").asText(), "linkedFrom", "Celje");
  }

  @Test
  @DisplayName("R-022-07 other restoring a merged-away contact twice")
  void R_022_07_other_restoreTwice() {
    JsonNode[] p = pair();
    merge(List.of(pick(p[0]), pick(p[1])), j("lastName", "Novak", "email", "x@x.si"), null)
        .expect(200);
    post(MARKO, base(p[1]) + "/restore", null).expect(204);
    post(MARKO, base(p[1]) + "/restore", null).expectError(409, "NOT_IN_TRASH");
  }

  @Test
  @DisplayName(
      "R-022-08 other a restored earlier version of the survivor keeps the merged-away contacts in Trash")
  void R_022_08_other_revertKeepsTrash() {
    JsonNode[] p = pair();
    Res m =
        merge(
                List.of(pick(p[0]), pick(p[1])),
                j("lastName", "Novak", "email", "x@x.si", "phone", "+386 41 555 123"),
                null)
            .expect(200);
    post(MARKO, base(p[0]) + "/versions/1/restore", j("revision", m.at("/revision").asLong()))
        .expect(200);
    assertThat(now(p[1]).path("state").asText()).isEqualTo("TRASHED");
    assertThat(now(p[0]).path("phone").asText()).isEmpty();
  }

  @Test
  @DisplayName("R-022-09 boundary exactly 50 attributes after a merge and 51")
  void R_022_09_boundary_fiftyAttributes() {
    List<Object> first = new ArrayList<>();
    List<Object> second = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
      first.add(attr("a" + i, "v"));
      second.add(attr("b" + i, "v"));
    }
    JsonNode a = contact("lastName", "N", "email", "x@x.si", "attributes", arr(first.toArray()));
    JsonNode b = contact("lastName", "N", "email", "x@x.si", "attributes", arr(second.toArray()));
    JsonNode c = contact("lastName", "N", "email", "x@x.si", "attributes", arr(attr("c1", "v")));
    merge(List.of(pick(a), pick(b)), j("lastName", "N", "email", "x@x.si"), null).expect(200);
    JsonNode d = contact("lastName", "N", "email", "y@x.si", "attributes", arr(first.toArray()));
    JsonNode e = contact("lastName", "N", "email", "y@x.si", "attributes", arr(second.toArray()));
    JsonNode f = contact("lastName", "N", "email", "y@x.si", "attributes", arr(attr("c1", "v")));
    merge(List.of(pick(d), pick(e), pick(f)), j("lastName", "N", "email", "y@x.si"), null)
        .expectFieldErrorStarting("attributes", "TOO_MANY");
    assertThat(c).isNotNull();
    assertThat(now(d).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("R-022-10 invalid empty list unknown ids and malformed ids")
  void R_022_10_invalid_lists() {
    merge(List.of(), j("lastName", "N"), null).expect(400);
    JsonNode a = contact("lastName", "A", "email", "x@x.si");
    merge(
            List.of(pick(a), j("id", UUID.randomUUID().toString(), "revision", 1)),
            j("lastName", "A"),
            null)
        .expectError(404, "NOT_FOUND");
    merge(List.of(pick(a), j("id", "not-an-id", "revision", 1)), j("lastName", "A"), null).status();
    assertThat(now(a).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("R-022-11 other nothing is merged by creating or editing duplicates")
  void R_022_11_other_neverAutomatic() {
    JsonNode[] p = pair();
    save(MARKO, gid, now(p[0]), "company", "Novo").expect(200);
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(2);
    assertThat(now(p[1]).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("R-022-12 combination a stale revision of the survivor alone is also a conflict")
  void R_022_12_combination_survivorStale() {
    JsonNode[] p = pair();
    ObjectNode a = pick(p[0]);
    ObjectNode b = pick(p[1]);
    save(ANA, gid, now(p[0]), "company", "Pozneje").expect(200);
    merge(List.of(a, b), j("lastName", "Novak", "email", "x@x.si"), null)
        .expectError(409, "CONFLICT");
    assertThat(now(p[0]).path("company").asText()).isEqualTo("Pozneje");
  }

  @Test
  @DisplayName("R-022-13 role merge by Editor works and by Viewer and stranger does not")
  void R_022_13_role_roles() {
    JsonNode[] p = pair();
    post(
            PETRA,
            "/groups/" + gid + "/merges",
            j("contacts", arr(pick(p[0]), pick(p[1])), "fields", j("lastName", "N")))
        .expectError(403, "FORBIDDEN");
    post(
            BORIS,
            "/groups/" + gid + "/merges",
            j("contacts", arr(pick(p[0]), pick(p[1])), "fields", j("lastName", "N")))
        .expectError(404, "NOT_FOUND");
    merge(List.of(pick(p[0]), pick(p[1])), j("lastName", "Novak", "email", "x@x.si"), null)
        .expect(200);
  }

  @Test
  @DisplayName(
      "R-022-14 other a merge that fails validation leaves tags attributes and versions untouched")
  void R_022_14_other_failedMergeUntouched() {
    JsonNode[] p = pair();
    merge(
            List.of(pick(p[0]), pick(p[1])),
            j("lastName", "Novak", "email", "x@x.si", "phone", "ni telefon"),
            null)
        .expectFieldError("phone", "INVALID_CHARACTERS");
    assertThat(strings(now(p[0]).path("tags"))).containsExactly("a");
    assertThat(now(p[0]).path("versionNumber").asInt()).isEqualTo(1);
    assertThat(entries(ANA, gid, "CONTACTS_MERGED")).isEmpty();
  }
}
