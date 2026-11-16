package app.contactgroups.hidden;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases for R-005 to R-008 and BR-05, BR-07 (contacts, tags, attributes). */
class HiddenContactsIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Skupina");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private Res create(Object... pairs) {
    return post(MARKO, "/groups/" + gid + "/contacts", j(pairs));
  }

  private JsonNode now(JsonNode c) {
    return openContact(ANA, gid, c.path("id").asText());
  }

  private String tagId(String name) {
    for (JsonNode t : get(ANA, "/groups/" + gid + "/tags").expect(200).json()) {
      if (name.equals(t.path("name").asText())) {
        return t.path("id").asText();
      }
    }
    return null;
  }

  @Test
  @DisplayName("R-005-01 other a created contact has version 1 revision and creation time")
  void R_005_01_other_createdContactShape() {
    JsonNode c =
        create(
                "firstName",
                "Mojca",
                "lastName",
                "Zupan",
                "street",
                "Ulica 1",
                "postalCode",
                "1000",
                "city",
                "Ljubljana",
                "country",
                "SI",
                "jobTitle",
                "Direktorica",
                "notes",
                "Zapis")
            .expect(201)
            .json();
    assertThat(c.path("versionNumber").asInt()).isEqualTo(1);
    assertThat(c.path("state").asText()).isEqualTo("ACTIVE");
    assertThat(c.path("createdAt").asText()).isNotBlank();
    assertThat(c.path("revision").asLong()).isPositive();
    JsonNode opened = now(c);
    assertThat(opened.path("street").asText()).isEqualTo("Ulica 1");
    assertThat(opened.path("jobTitle").asText()).isEqualTo("Direktorica");
    assertThat(opened.path("notes").asText()).isEqualTo("Zapis");
  }

  @Test
  @DisplayName("R-005-02 combination one of four is met by e-mail alone or last name alone")
  void R_005_02_combination_oneOfFour() {
    create("email", "samo@example.com").expect(201);
    create("lastName", "Samo").expect(201);
    create("firstName", " ", "lastName", "\t", "email", " ", "company", "  ", "city", "Ljubljana")
        .expectFieldError("firstName", "REQUIRED_ONE_OF");
    create().expectFieldError("firstName", "REQUIRED_ONE_OF");
  }

  @Test
  @DisplayName("R-005-03 combination every broken field is named in one answer")
  void R_005_03_combination_allFieldsNamed() {
    Res r =
        create(
            "lastName",
            "Zupan",
            "email",
            "a b@example.com",
            "phone",
            "12x",
            "country",
            "ZZ",
            "city",
            repeat("c", 256),
            "notes",
            repeat("n", 5001));
    for (String f : List.of("email", "phone", "country", "city", "notes")) {
      boolean named = false;
      for (JsonNode e : r.expectError(400, "VALIDATION_FAILED").json().path("errors")) {
        named |= f.equals(e.path("field").asText());
      }
      assertThat(named).as(f).isTrue();
    }
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
  }

  @Test
  @DisplayName("R-005-04 boundary lowercase also for non-ASCII and trimmed e-mail")
  void R_005_04_boundary_lowercaseAndTrim() {
    JsonNode c =
        create("lastName", "Zupan", "email", "  MOJCA.Čuk@Example.SI  ").expect(201).json();
    assertThat(now(c).path("email").asText()).isEqualTo("mojca.čuk@example.si");
    create("lastName", "Zupan", "email", repeat("a", 244) + "@example.com")
        .expectFieldError("email", "TOO_LONG");
  }

  @Test
  @DisplayName("R-005-05 other the same e-mail twice in one group and in two groups")
  void R_005_05_other_noUniqueness() {
    String other = group(ANA, "Druga");
    for (int i = 0; i < 3; i++) {
      create("lastName", "Dvojnik", "email", "isti@example.com").expect(201);
    }
    newContact(ANA, other, j("lastName", "Dvojnik", "email", "isti@example.com"));
    assertThat(search(ANA, gid, "isti@example.com").at("/total").asInt()).isEqualTo(3);
  }

  @Test
  @DisplayName("R-005-06 role every role opens the same contact with tags and attributes")
  void R_005_06_role_everyRoleOpensIt() {
    JsonNode c =
        newContact(
            ANA, gid, j("lastName", "Zupan", "tags", arr("t1"), "attributes", arr(attr("k", "v"))));
    for (var w : List.of(ANA, MARKO, PETRA)) {
      JsonNode o = openContact(w, gid, c.path("id").asText());
      assertThat(strings(o.path("tags"))).containsExactly("t1");
      assertThat(o.at("/attributes/0/value").asText()).isEqualTo("v");
    }
  }

  @Test
  @DisplayName("R-005-07 combination consecutive saves with the returned revision succeed")
  void R_005_07_combination_consecutiveSaves() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Zupan"));
    JsonNode a = save(MARKO, gid, now(c), "company", "A").expect(200).json();
    JsonNode b = save(MARKO, gid, a, "company", "B").expect(200).json();
    assertThat(b.path("revision").asLong()).isGreaterThan(a.path("revision").asLong());
    assertThat(b.path("versionNumber").asInt()).isEqualTo(3);
    assertThat(now(c).path("company").asText()).isEqualTo("B");
    save(MARKO, gid, a, "company", "C").expectError(409, "CONFLICT");
  }

  @Test
  @DisplayName("R-005-08 other a missing revision is refused and nothing is overwritten")
  void R_005_08_other_missingRevision() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Zupan", "company", "Acme"));
    Res r =
        put(
            MARKO,
            "/groups/" + gid + "/contacts/" + c.path("id").asText(),
            j("lastName", "Zupan", "company", "Beta"));
    assertThat(r.status()).isIn(400, 409);
    assertThat(now(c).path("company").asText()).isEqualTo("Acme");
  }

  @Test
  @DisplayName("R-005-09 role a Viewer cannot create or save and gets forbidden before validation")
  void R_005_09_role_viewerForbiddenBeforeValidation() {
    post(PETRA, "/groups/" + gid + "/contacts", j("email", "ni-veljaven"))
        .expectError(403, "FORBIDDEN");
    post(PETRA, "/groups/" + gid + "/contacts", j("lastName", "x")).expectError(403, "FORBIDDEN");
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
  }

  @Test
  @DisplayName("R-005-10 other a trashed contact refuses a save also without changes")
  void R_005_10_other_trashedRefusesEvenNoChange() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Zupan"));
    JsonNode opened = now(c);
    trash(ANA, gid, c.path("id").asText());
    save(MARKO, gid, opened).expectError(409, "CONTACT_IN_TRASH");
  }

  @Test
  @DisplayName("R-006-01 boundary a new tag keeps the spelling typed first")
  void R_006_01_boundary_firstSpellingKept() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("MojaOznaka")));
    JsonNode b = newContact(ANA, gid, j("lastName", "B", "tags", arr("mojaoznaka")));
    assertThat(strings(now(a).path("tags"))).containsExactly("MojaOznaka");
    assertThat(strings(now(b).path("tags"))).containsExactly("MojaOznaka");
    assertThat(values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactly("MojaOznaka");
  }

  @Test
  @DisplayName("R-006-02 combination an existing tag in other case on a contact that has it")
  void R_006_02_combination_sameTagOtherCase() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("x")));
    save(ANA, gid, now(a), "tags", arr("X", "y")).expect(200);
    assertThat(strings(now(a).path("tags"))).containsExactlyInAnyOrder("x", "y");
    assertThat(get(ANA, "/groups/" + gid + "/tags").expect(200).json()).hasSize(2);
  }

  @Test
  @DisplayName("R-006-03 invalid tag characters are refused whole save")
  void R_006_03_invalid_characters() {
    for (String bad : List.of("a,b", "a;b", "a#b", "a'b", "a\"b", "a%b", "a\tb", "ž ž")) {
      create("lastName", "Zupan", "tags", arr("dober", bad))
          .expectFieldErrorStarting("tags", "INVALID_CHARACTERS");
    }
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
    assertThat(get(ANA, "/groups/" + gid + "/tags").expect(200).json()).isEmpty();
  }

  @Test
  @DisplayName("R-006-04 boundary 51 tags on create are refused and 50 accepted")
  void R_006_04_boundary_fiftyOnCreate() {
    List<String> tags = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      tags.add("t" + i);
    }
    create("lastName", "A", "tags", arr(tags.toArray())).expect(201);
    tags.add("t50");
    create("lastName", "B", "tags", arr(tags.toArray()))
        .expectFieldErrorStarting("tags", "TOO_MANY");
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-006-05 combination the same name in two groups is two tags")
  void R_006_05_combination_independentTags() {
    String other = group(ANA, "Druga");
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("Skupna")));
    newContact(ANA, other, j("lastName", "B", "tags", arr("skupna")));
    assertThat(values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactly("Skupna");
    assertThat(values(get(ANA, "/groups/" + other + "/tags").expect(200).json(), "name"))
        .containsExactly("skupna");
    patch(ANA, "/groups/" + gid + "/tags/" + tagId("Skupna"), j("name", "Preimenovana"))
        .expect(200);
    assertThat(values(get(ANA, "/groups/" + other + "/tags").expect(200).json(), "name"))
        .containsExactly("skupna");
    assertThat(a).isNotNull();
  }

  @Test
  @DisplayName("R-006-06 other removing one of several tags keeps the others")
  void R_006_06_other_removeOne() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("a", "b", "c")));
    save(ANA, gid, now(a), "tags", arr("a", "c")).expect(200);
    assertThat(strings(now(a).path("tags"))).containsExactlyInAnyOrder("a", "c");
    assertThat(values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactlyInAnyOrder("a", "c");
  }

  @Test
  @DisplayName("R-006-07 combination a tag stays while one contact in Trash holds it")
  void R_006_07_combination_trashedHolderKeepsTag() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("x")));
    JsonNode b = newContact(ANA, gid, j("lastName", "B", "tags", arr("x")));
    trash(ANA, gid, b.path("id").asText());
    save(ANA, gid, now(a), "tags", arr()).expect(200);
    JsonNode listed = get(ANA, "/groups/" + gid + "/tags").expect(200).json();
    assertThat(listed).hasSize(1);
    assertThat(listed.get(0).path("contactCount").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-006-08 other tags do not change search or listing beyond display")
  void R_006_08_other_labelOnly() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A"));
    String before = search(ANA, gid, j()).path("items").get(0).path("lastName").asText();
    save(ANA, gid, now(a), "tags", arr("pozor", "admin", "delete", "OWNER")).expect(200);
    assertThat(search(ANA, gid, j()).path("items").get(0).path("lastName").asText())
        .isEqualTo(before);
    assertThat(get(ANA, "/groups/" + gid).expect(200).at("/role").asText()).isEqualTo("OWNER");
  }

  @Test
  @DisplayName("R-006-09 role a Viewer reads tags in the list too")
  void R_006_09_role_viewerReadsTagsInList() {
    newContact(ANA, gid, j("lastName", "A", "tags", arr("x", "y")));
    JsonNode row = search(PETRA, gid, "").at("/items/0");
    assertThat(strings(row.path("tags"))).containsExactlyInAnyOrder("x", "y");
    assertThat(values(get(PETRA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactlyInAnyOrder("x", "y");
  }

  @Test
  @DisplayName("R-006-10 combination suggestions match part of the name ignoring case")
  void R_006_10_combination_suggestions() {
    newContact(
        ANA, gid, j("lastName", "A", "tags", arr("Udelezenec2025", "gost", "udelezenec2026")));
    JsonNode s = get(ANA, "/groups/" + gid + "/tags?q=DELEZ").expect(200).json();
    assertThat(values(s, "name")).containsExactlyInAnyOrder("Udelezenec2025", "udelezenec2026");
    assertThat(get(ANA, "/groups/" + gid + "/tags?q=zzz").expect(200).json()).isEmpty();
    assertThat(get(ANA, "/groups/" + gid + "/tags?q=").expect(200).json()).hasSize(3);
  }

  @Test
  @DisplayName("R-007-01 combination a rename reaches contacts in and out of Trash and the list")
  void R_007_01_combination_renameEverywhere() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("old")));
    JsonNode b = newContact(ANA, gid, j("lastName", "B", "tags", arr("old", "other")));
    trash(ANA, gid, b.path("id").asText());
    patch(MARKO, "/groups/" + gid + "/tags/" + tagId("old"), j("name", "new")).expect(200);
    assertThat(strings(now(a).path("tags"))).containsExactly("new");
    assertThat(strings(now(b).path("tags"))).containsExactlyInAnyOrder("new", "other");
    assertThat(strings(search(ANA, gid, "").at("/items/0/tags"))).containsExactly("new");
  }

  @Test
  @DisplayName("R-007-02 invalid renaming to another name of different case in a trashed contact")
  void R_007_02_invalid_trashedHolderStillCounts() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("one")));
    JsonNode b = newContact(ANA, gid, j("lastName", "B", "tags", arr("two")));
    trash(ANA, gid, b.path("id").asText());
    patch(ANA, "/groups/" + gid + "/tags/" + tagId("one"), j("name", "TWO"))
        .expectError(409, "TAG_NAME_TAKEN");
    assertThat(strings(now(a).path("tags"))).containsExactly("one");
  }

  @Test
  @DisplayName("R-007-03 boundary a capitalisation change of an accented tag")
  void R_007_03_boundary_accentedCapitalisation() {
    newContact(ANA, gid, j("lastName", "A", "tags", arr("čaj")));
    patch(ANA, "/groups/" + gid + "/tags/" + tagId("čaj"), j("name", "ČAJ")).expect(200);
    assertThat(values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactly("ČAJ");
  }

  @Test
  @DisplayName("R-007-04 invalid new names are refused")
  void R_007_04_invalid_newNames() {
    newContact(ANA, gid, j("lastName", "A", "tags", arr("old")));
    String id = tagId("old");
    for (String bad : List.of("", "a.b", "a/b", " ", "a b")) {
      patch(ANA, "/groups/" + gid + "/tags/" + id, j("name", bad)).expect(400);
    }
    assertThat(values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactly("old");
  }

  @Test
  @DisplayName(
      "R-007-05 combination deleting a tag also removes it from a trashed contact and the suggestions")
  void R_007_05_combination_deleteEverywhere() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("old", "keep")));
    trash(ANA, gid, a.path("id").asText());
    delete(ANA, "/groups/" + gid + "/tags/" + tagId("old")).expect(204);
    assertThat(strings(now(a).path("tags"))).containsExactly("keep");
    assertThat(values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactly("keep");
    assertThat(now(a).path("state").asText()).isEqualTo("TRASHED");
  }

  @Test
  @DisplayName(
      "R-007-06 combination a tag change makes an older save a conflict but adds no version")
  void R_007_06_combination_revisionGrowsNoVersion() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A", "tags", arr("old")));
    JsonNode stale = now(a);
    patch(ANA, "/groups/" + gid + "/tags/" + tagId("old"), j("name", "new")).expect(200);
    assertThat(now(a).path("versionNumber").asInt()).isEqualTo(1);
    assertThat(now(a).path("revision").asLong()).isGreaterThan(stale.path("revision").asLong());
    save(ANA, gid, stale, "company", "Acme").expectError(409, "CONFLICT");
  }

  @Test
  @DisplayName("R-007-07 role an Editor may rename and delete but a Viewer may not")
  void R_007_07_role_editorMayViewerMayNot() {
    newContact(ANA, gid, j("lastName", "A", "tags", arr("one", "two")));
    patch(MARKO, "/groups/" + gid + "/tags/" + tagId("one"), j("name", "uno")).expect(200);
    delete(MARKO, "/groups/" + gid + "/tags/" + tagId("two")).expect(204);
    patch(PETRA, "/groups/" + gid + "/tags/" + tagId("uno"), j("name", "x"))
        .expectError(403, "FORBIDDEN");
    delete(PETRA, "/groups/" + gid + "/tags/" + tagId("uno")).expectError(403, "FORBIDDEN");
    patch(BORIS, "/groups/" + gid + "/tags/" + tagId("uno"), j("name", "x"))
        .expectError(404, "NOT_FOUND");
  }

  @Test
  @DisplayName("R-007-08 other a deleted tag writes one entry per contact with the tag name")
  void R_007_08_other_deleteEntries() {
    for (String l : List.of("A", "B", "C")) {
      newContact(ANA, gid, j("lastName", l, "tags", arr("old")));
    }
    delete(ANA, "/groups/" + gid + "/tags/" + tagId("old")).expect(204);
    List<JsonNode> e = entries(ANA, gid, "TAG_DELETED");
    assertThat(e).hasSize(3);
    assertThat(e.get(0).toString()).contains("old");
  }

  @Test
  @DisplayName("R-008-01 combination attributes are stored with the contact in the order saved")
  void R_008_01_combination_orderAndText() {
    JsonNode c =
        newContact(
            ANA,
            gid,
            j("lastName", "A", "attributes", arr(attr("b", "2"), attr("a", "1"), attr("č", "ž"))));
    assertThat(values(now(c).path("attributes"), "key")).containsExactlyInAnyOrder("b", "a", "č");
    assertThat(values(now(c).path("attributes"), "value")).containsExactlyInAnyOrder("2", "1", "ž");
  }

  @Test
  @DisplayName(
      "R-008-02 boundary the same key twice in one request and key with different case on create")
  void R_008_02_boundary_duplicateOnCreate() {
    create("lastName", "A", "attributes", arr(attr("Ključ", "1"), attr("KLJUČ", "2")))
        .expectFieldErrorStarting("attributes", "DUPLICATE_KEY");
    create("lastName", "A", "attributes", arr(attr("x", "1"), attr(" X ", "2")))
        .expectFieldErrorStarting("attributes", "DUPLICATE_KEY");
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
  }

  @Test
  @DisplayName("R-008-03 invalid key and value limits on create")
  void R_008_03_invalid_limitsOnCreate() {
    create("lastName", "A", "attributes", arr(attr("", "v")))
        .expectFieldErrorStarting("attributes", "REQUIRED");
    create("lastName", "A", "attributes", arr(attr(repeat("k", 51), "v")))
        .expectFieldErrorStarting("attributes", "TOO_LONG");
    create("lastName", "A", "attributes", arr(attr("k", repeat("v", 256))))
        .expectFieldErrorStarting("attributes", "TOO_LONG");
    create("lastName", "A", "attributes", arr(attr(repeat("k", 50), repeat("v", 255)))).expect(201);
  }

  @Test
  @DisplayName("R-008-04 boundary 51 attributes on create")
  void R_008_04_boundary_fiftyOneOnCreate() {
    List<Object> list = new ArrayList<>();
    for (int i = 0; i < 51; i++) {
      list.add(attr("k" + i, "v"));
    }
    create("lastName", "A", "attributes", arr(list.toArray()))
        .expectFieldErrorStarting("attributes", "TOO_MANY");
    list.remove(50);
    create("lastName", "A", "attributes", arr(list.toArray())).expect(201);
  }

  @Test
  @DisplayName(
      "R-008-05 combination key suggestions match part of a key ignoring case and exclude other groups")
  void R_008_05_combination_suggestions() {
    newContact(
        ANA,
        gid,
        j("lastName", "A", "attributes", arr(attr("Kompetenca", "x"), attr("leto", "2025"))));
    String other = group(BORIS, "Tuja");
    newContact(BORIS, other, j("lastName", "T", "attributes", arr(attr("kompot", "y"))));
    assertThat(strings(get(ANA, "/groups/" + gid + "/attribute-keys?q=KOMP").expect(200).json()))
        .containsExactly("Kompetenca");
    assertThat(strings(get(ANA, "/groups/" + gid + "/attribute-keys").expect(200).json()))
        .containsExactlyInAnyOrder("Kompetenca", "leto");
    assertThat(get(ANA, "/groups/" + gid + "/attribute-keys?q=zzz").expect(200).json()).isEmpty();
  }

  @Test
  @DisplayName("R-008-06 other values stay text with leading zeros and dates")
  void R_008_06_other_valuesStayText() {
    JsonNode c =
        newContact(
            ANA,
            gid,
            j(
                "lastName",
                "A",
                "attributes",
                arr(
                    attr("koda", "007"),
                    attr("datum", "2026-07-01"),
                    attr("decimalka", "1,50"),
                    attr("izraz", "=1+1"))));
    JsonNode a = now(c).path("attributes");
    assertThat(values(a, "value")).containsExactlyInAnyOrder("007", "2026-07-01", "1,50", "=1+1");
    for (JsonNode x : a) {
      assertThat(x.path("value").isTextual()).isTrue();
    }
  }

  @Test
  @DisplayName("R-008-07 combination replacing one attribute keeps the others")
  void R_008_07_combination_replaceOne() {
    JsonNode c =
        newContact(ANA, gid, j("lastName", "A", "attributes", arr(attr("a", "1"), attr("b", "2"))));
    save(ANA, gid, now(c), "attributes", arr(attr("b", "2"), attr("c", "3"))).expect(200);
    assertThat(values(now(c).path("attributes"), "key")).containsExactlyInAnyOrder("b", "c");
  }

  @Test
  @DisplayName("R-008-08 role a Viewer sees attributes in the contact and cannot save")
  void R_008_08_role_viewerReads() {
    JsonNode c = newContact(ANA, gid, j("lastName", "A", "attributes", arr(attr("a", "1"))));
    assertThat(openContact(PETRA, gid, c.path("id").asText()).at("/attributes/0/value").asText())
        .isEqualTo("1");
    save(PETRA, gid, now(c), "attributes", arr(attr("a", "2"))).expectError(403, "FORBIDDEN");
    assertThat(now(c).at("/attributes/0/value").asText()).isEqualTo("1");
  }

  @Test
  @DisplayName(
      "BR-05 combination the whole contact is refused when one field is broken also on save")
  void BR_05_combination_wholeSaveRefused() {
    JsonNode c = newContact(ANA, gid, j("lastName", "A", "company", "Acme"));
    save(ANA, gid, now(c), "company", "Beta", "phone", "ni telefon", "tags", arr("ok", "ni ok"))
        .expectFieldError("phone", "INVALID_CHARACTERS");
    JsonNode unchanged = now(c);
    assertThat(unchanged.path("company").asText()).isEqualTo("Acme");
    assertThat(unchanged.path("tags")).isEmpty();
    assertThat(unchanged.path("versionNumber").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("BR-05 boundary country codes in lowercase and unknown codes")
  void BR_05_boundary_countryCodes() {
    create("lastName", "A", "country", "si").expectFieldError("country", "UNKNOWN_COUNTRY");
    create("lastName", "A", "country", "XX").expectFieldError("country", "UNKNOWN_COUNTRY");
    create("lastName", "A", "country", "AT").expect(201);
    create("lastName", "A", "country", "").expect(201);
  }

  @Test
  @DisplayName("BR-05 boundary the phone limit of 30 characters and allowed characters")
  void BR_05_boundary_phoneLimits() {
    create("lastName", "A", "phone", repeat("1", 30)).expect(201);
    create("lastName", "A", "phone", repeat("1", 31)).expectFieldError("phone", "TOO_LONG");
    create("lastName", "A", "phone", "+(1) 2-3.4/5").expect(201);
    create("lastName", "A", "phone", "12#4").expectFieldError("phone", "INVALID_CHARACTERS");
    create("lastName", "A", "phone", "12;4").expectFieldError("phone", "INVALID_CHARACTERS");
  }

  @Test
  @DisplayName("BR-07 combination the three ways of comparing texts")
  void BR_07_combination_comparisons() {
    JsonNode c =
        newContact(
            ANA,
            gid,
            j(
                "lastName",
                "Škrlj",
                "company",
                "Šola",
                "tags",
                arr("Čaj"),
                "attributes",
                arr(attr("Ključ", "Šmarje"))));
    // tags and keys ignore case only
    assertThat(
            search(ANA, gid, j("conditions", arr(j("type", "TAGS_ALL", "tags", arr("čAJ")))))
                .at("/total")
                .asInt())
        .isEqualTo(1);
    assertThat(
            search(ANA, gid, j("conditions", arr(j("type", "TAGS_ALL", "tags", arr("caj")))))
                .at("/total")
                .asInt())
        .isZero();
    assertThat(
            search(ANA, gid, j("conditions", arr(j("type", "ATTRIBUTE_EXISTS", "key", "KLJUČ"))))
                .at("/total")
                .asInt())
        .isEqualTo(1);
    assertThat(
            search(ANA, gid, j("conditions", arr(j("type", "ATTRIBUTE_EXISTS", "key", "kljuc"))))
                .at("/total")
                .asInt())
        .isZero();
    // values, company and search ignore case and accents
    assertThat(
            search(
                    ANA,
                    gid,
                    j(
                        "conditions",
                        arr(j("type", "ATTRIBUTE_EQUALS", "key", "ključ", "value", "smarje"))))
                .at("/total")
                .asInt())
        .isEqualTo(1);
    assertThat(
            search(ANA, gid, j("conditions", arr(j("type", "COMPANY", "company", "SOLA"))))
                .at("/total")
                .asInt())
        .isEqualTo(1);
    assertThat(search(ANA, gid, "SKRLJ").at("/total").asInt()).isEqualTo(1);
    assertThat(c).isNotNull();
  }
}
