package app.contactgroups.hidden;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import app.contactgroups.acceptance.Workbook;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases for R-009 to R-012 (list, search, filters, export, Trash) and BR-04, BR-06. */
class HiddenListIT extends AcceptanceBase {

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

  private ObjectNode sort(String field, String direction) {
    return j("sort", j("field", field, "direction", direction));
  }

  private List<String> last(JsonNode page) {
    return rowValues(page, "lastName");
  }

  private Res export(ObjectNode body) {
    return post(ANA, "/groups/" + gid + "/exports", body);
  }

  @Test
  @DisplayName("R-009-01 boundary default order breaks ties by first name and shows the total")
  void R_009_01_boundary_tieBreakByFirstName() {
    contact("firstName", "Žan", "lastName", "Kovač");
    contact("firstName", "Ana", "lastName", "Kovač");
    contact("firstName", "Čeh", "lastName", "Kovač");
    JsonNode r = search(ANA, gid, j());
    assertThat(rowValues(r, "firstName")).containsExactly("Ana", "Čeh", "Žan");
    assertThat(r.at("/total").asInt()).isEqualTo(3);
    assertThat(r.at("/page").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-009-02 combination every sortable column in both directions")
  void R_009_02_combination_everyColumn() {
    contact(
        "firstName",
        "B",
        "lastName",
        "A",
        "email",
        "c@x.si",
        "phone",
        "3",
        "company",
        "Ž",
        "city",
        "Šmarje",
        "country",
        "SI");
    contact(
        "firstName",
        "A",
        "lastName",
        "B",
        "email",
        "a@x.si",
        "phone",
        "1",
        "company",
        "C",
        "city",
        "Celje",
        "country",
        "AT");
    contact(
        "firstName",
        "C",
        "lastName",
        "C",
        "email",
        "b@x.si",
        "phone",
        "2",
        "company",
        "S",
        "city",
        "Čatež",
        "country",
        "DE");
    for (String f :
        List.of("lastName", "firstName", "email", "phone", "company", "city", "country")) {
      List<String> asc = rowValues(search(ANA, gid, sort(f, "ASC")), "id");
      List<String> desc = rowValues(search(ANA, gid, sort(f, "DESC")), "id");
      List<String> reversed = new ArrayList<>(desc);
      java.util.Collections.reverse(reversed);
      assertThat(asc).as(f).isEqualTo(reversed).hasSize(3);
    }
    assertThat(rowValues(search(ANA, gid, sort("city", "ASC")), "city"))
        .containsExactly("Celje", "Čatež", "Šmarje");
    post(ANA, "/groups/" + gid + "/contacts/search", sort("tags", "ASC")).expect(400);
    post(ANA, "/groups/" + gid + "/contacts/search", sort("notes", "ASC")).expect(400);
  }

  @Test
  @DisplayName("R-009-03 combination words match different fields and contain accents")
  void R_009_03_combination_wordsAcrossFields() {
    contact(
        "firstName",
        "Marija",
        "lastName",
        "Černe",
        "city",
        "Maribor",
        "tags",
        arr("vip"),
        "attributes",
        arr(attr("klub", "Lipa")));
    assertThat(search(ANA, gid, "marija cerne").at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, gid, "MARIB vip lipa").at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, gid, "marija ljubljana").at("/total").asInt()).isZero();
    assertThat(search(ANA, gid, "  černe   marija ").at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, gid, "klub").at("/total").asInt()).as("keys are not searched").isZero();
  }

  @Test
  @DisplayName("R-009-04 other phone and notes are never searched and e-mail and company are")
  void R_009_04_other_notSearchedFields() {
    contact(
        "lastName",
        "A",
        "phone",
        "040 123 456",
        "notes",
        "zasebno",
        "email",
        "uporabnik@primer.si",
        "company",
        "Podjetje d.o.o.");
    assertThat(search(ANA, gid, "040").at("/total").asInt()).isZero();
    assertThat(search(ANA, gid, "zasebno").at("/total").asInt()).isZero();
    assertThat(search(ANA, gid, "primer.si").at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, gid, "d.o.o").at("/total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-009-05 other no match gives total zero on every page request")
  void R_009_05_other_noMatchTotalZero() {
    contact("lastName", "A");
    JsonNode r = search(ANA, gid, j("q", "zzz", "page", 3));
    assertThat(r.at("/total").asInt()).isZero();
    assertThat(r.path("items")).isEmpty();
    assertThat(r.at("/page").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-009-06 role an empty group answers an empty page for every role")
  void R_009_06_role_emptyGroupEveryRole() {
    for (var w : List.of(ANA, MARKO, PETRA)) {
      JsonNode r = search(w, gid, j());
      assertThat(r.at("/total").asInt()).isZero();
      assertThat(r.path("items")).isEmpty();
    }
    assertThat(get(MARKO, "/groups/" + gid).at("/role").asText()).isEqualTo("EDITOR");
  }

  @Test
  @DisplayName("R-009-07 combination Trash and other groups never appear in any list or search")
  void R_009_07_combination_noTrashNoOtherGroup() {
    String other = group(ANA, "Druga");
    newContact(ANA, other, j("lastName", "Tuja"));
    JsonNode t = contact("lastName", "Zbrisana");
    contact("lastName", "Ostane");
    trash(ANA, gid, t.path("id").asText());
    assertThat(last(search(PETRA, gid, j()))).containsExactly("Ostane");
    assertThat(search(PETRA, gid, "tuja").at("/total").asInt()).isZero();
    assertThat(search(PETRA, gid, "zbrisana").at("/total").asInt()).isZero();
  }

  @Test
  @DisplayName("R-009-08 other every request carries its own search, sort and page")
  void R_009_08_other_stateless() {
    for (int i = 0; i < 30; i++) {
      contact("lastName", "Novak" + String.format("%02d", i), "city", "C" + (29 - i));
    }
    contact("lastName", "Horvat", "city", "A");
    JsonNode p2 =
        search(
            ANA, gid, j("q", "novak", "page", 2, "sort", j("field", "city", "direction", "DESC")));
    assertThat(p2.at("/total").asInt()).isEqualTo(30);
    assertThat(p2.path("items")).hasSize(5);
    assertThat(rowValues(p2, "city")).containsExactly("C4", "C3", "C2", "C1", "C0");
  }

  @Test
  @DisplayName("R-009-09 other the row id opens that contact")
  void R_009_09_other_rowOpensContact() {
    contact("firstName", "Ana", "lastName", "Odprta", "email", "odprta@example.com");
    String id = search(ANA, gid, "odprta").at("/items/0/id").asText();
    assertThat(openContact(PETRA, gid, id).path("email").asText()).isEqualTo("odprta@example.com");
  }

  @Test
  @DisplayName("R-009-10 role the Viewer result equals the Owner result for filters too")
  void R_009_10_role_sameForFilters() {
    contact("lastName", "A", "tags", arr("x"), "country", "SI");
    contact("lastName", "B", "country", "AT");
    ObjectNode req =
        j(
            "conditions",
            arr(j("type", "COUNTRY", "country", "SI")),
            "sort",
            j("field", "lastName", "direction", "ASC"));
    assertThat(search(PETRA, gid, req).toString()).isEqualTo(search(ANA, gid, req).toString());
  }

  @Test
  @DisplayName("R-010-01 combination all of several tags ignoring case with a trashed holder")
  void R_010_01_combination_allOfSeveral() {
    contact("lastName", "A", "tags", arr("Alfa", "beta", "gama"));
    contact("lastName", "B", "tags", arr("alfa", "beta"));
    JsonNode t = contact("lastName", "C", "tags", arr("alfa", "beta", "gama"));
    trash(ANA, gid, t.path("id").asText());
    JsonNode r =
        search(
            ANA,
            gid,
            j("conditions", arr(j("type", "TAGS_ALL", "tags", arr("ALFA", "Beta", "GAMA")))));
    assertThat(last(r)).containsExactly("A");
  }

  @Test
  @DisplayName("R-010-02 combination any of with one unknown tag")
  void R_010_02_combination_anyWithUnknown() {
    contact("lastName", "A", "tags", arr("a"));
    contact("lastName", "B", "tags", arr("b"));
    contact("lastName", "C");
    assertThat(
            last(
                search(
                    ANA,
                    gid,
                    j("conditions", arr(j("type", "TAGS_ANY", "tags", arr("A", "nobody")))))))
        .containsExactly("A");
  }

  @Test
  @DisplayName("R-010-03 boundary does-not-have includes untagged and other-tag contacts")
  void R_010_03_boundary_includesUntagged() {
    contact("lastName", "A", "tags", arr("x"));
    contact("lastName", "B", "tags", arr("y"));
    contact("lastName", "C");
    assertThat(last(search(ANA, gid, j("conditions", arr(j("type", "TAG_NOT", "tag", "X"))))))
        .containsExactly("B", "C");
  }

  @Test
  @DisplayName("R-010-04 combination attribute equals needs both key and value")
  void R_010_04_combination_keyAndValue() {
    contact("lastName", "A", "attributes", arr(attr("barva", "Zelena"), attr("velikost", "M")));
    contact("lastName", "B", "attributes", arr(attr("barva", "Rdeča"), attr("velikost", "Zelena")));
    assertThat(
            last(
                search(
                    ANA,
                    gid,
                    j(
                        "conditions",
                        arr(j("type", "ATTRIBUTE_EQUALS", "key", "BARVA", "value", "zelena"))))))
        .containsExactly("A");
    assertThat(
            search(
                    ANA,
                    gid,
                    j(
                        "conditions",
                        arr(j("type", "ATTRIBUTE_EQUALS", "key", "barva", "value", "zele"))))
                .at("/total")
                .asInt())
        .isZero();
    assertThat(
            last(
                search(
                    ANA,
                    gid,
                    j(
                        "conditions",
                        arr(j("type", "ATTRIBUTE_EQUALS", "key", "barva", "value", "RDECA"))))))
        .containsExactly("B");
  }

  @Test
  @DisplayName("R-010-05 boundary key exists is not a partial match")
  void R_010_05_boundary_notPartial() {
    contact("lastName", "A", "attributes", arr(attr("kompetenca", "")));
    assertThat(
            search(ANA, gid, j("conditions", arr(j("type", "ATTRIBUTE_EXISTS", "key", "komp"))))
                .at("/total")
                .asInt())
        .isZero();
    assertThat(
            search(
                    ANA,
                    gid,
                    j("conditions", arr(j("type", "ATTRIBUTE_EXISTS", "key", "Kompetenca"))))
                .at("/total")
                .asInt())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("R-010-06 invalid country codes in a condition")
  void R_010_06_invalid_countryCondition() {
    contact("lastName", "A", "country", "SI");
    post(
            ANA,
            "/groups/" + gid + "/contacts/search",
            j("conditions", arr(j("type", "COUNTRY", "country", "Slovenija"))))
        .expect(400);
    post(
            ANA,
            "/groups/" + gid + "/contacts/search",
            j("conditions", arr(j("type", "COUNTRY", "country", "si"))))
        .expect(400);
  }

  @Test
  @DisplayName(
      "R-010-07 combination company equality is not a partial match and the suggestions follow the group")
  void R_010_07_combination_companyEquality() {
    contact("lastName", "A", "company", "Acme d.o.o.");
    contact("lastName", "B", "company", "Acme");
    assertThat(
            last(search(ANA, gid, j("conditions", arr(j("type", "COMPANY", "company", "ACME"))))))
        .containsExactly("B");
    JsonNode s = get(ANA, "/groups/" + gid + "/companies?q=acm").expect(200).json();
    assertThat(strings(s)).containsExactlyInAnyOrder("Acme d.o.o.", "Acme");
  }

  @Test
  @DisplayName("R-010-08 combination three conditions and a search text")
  void R_010_08_combination_threeAndText() {
    contact("lastName", "Novak", "country", "SI", "company", "Acme", "tags", arr("x"));
    contact("lastName", "Novak", "country", "SI", "company", "Acme");
    contact("lastName", "Novak", "country", "AT", "company", "Acme", "tags", arr("x"));
    contact("lastName", "Horvat", "country", "SI", "company", "Acme", "tags", arr("x"));
    JsonNode r =
        search(
            ANA,
            gid,
            j(
                "q",
                "novak",
                "conditions",
                arr(
                    j("type", "COUNTRY", "country", "SI"),
                    j("type", "COMPANY", "company", "acme"),
                    j("type", "TAGS_ALL", "tags", arr("x")))));
    assertThat(r.at("/total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-010-09 invalid condition types and malformed conditions are refused")
  void R_010_09_invalid_conditionTypes() {
    for (ObjectNode c :
        List.of(
            j("type", "PHONE_EQUALS", "value", "1"),
            j("tags", arr("x")),
            j("type", "TAGS_ALL"),
            j("type", "ATTRIBUTE_EXISTS"),
            j("type", "COMPANY"))) {
      post(ANA, "/groups/" + gid + "/contacts/search", j("conditions", arr(c))).expect(400);
    }
  }

  @Test
  @DisplayName("R-010-10 other removing every condition returns the full total at once")
  void R_010_10_other_fullTotalWithoutConditions() {
    contact("lastName", "A", "tags", arr("x"));
    contact("lastName", "B");
    assertThat(
            search(ANA, gid, j("conditions", arr(j("type", "TAGS_ALL", "tags", arr("x")))))
                .at("/total")
                .asInt())
        .isEqualTo(1);
    assertThat(search(ANA, gid, j("conditions", arr())).at("/total").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName(
      "R-010-11 other filters are never stored: a later request without them is unfiltered")
  void R_010_11_other_noStoredFilter() {
    contact("lastName", "A", "tags", arr("x"));
    contact("lastName", "B");
    search(ANA, gid, j("conditions", arr(j("type", "TAGS_ALL", "tags", arr("x")))));
    assertThat(search(PETRA, gid, j()).at("/total").asInt()).isEqualTo(2);
    assertThat(search(ANA, gid, j()).at("/total").asInt()).isEqualTo(2);
  }

  @Test
  @DisplayName("R-011-01 combination the select-all by filter and by ids give the same rows")
  void R_011_01_combination_filterAndIds() {
    JsonNode a = contact("lastName", "A", "tags", arr("x"));
    JsonNode b = contact("lastName", "B", "tags", arr("x"));
    contact("lastName", "C");
    try (Workbook byFilter =
            new Workbook(
                export(
                        j(
                            "language",
                            "sl",
                            "filter",
                            j("conditions", arr(j("type", "TAGS_ALL", "tags", arr("x"))))))
                    .expect(200));
        Workbook byIds =
            new Workbook(
                export(
                        j(
                            "language",
                            "sl",
                            "contactIds",
                            arr(a.path("id").asText(), b.path("id").asText())))
                    .expect(200))) {
      assertThat(byFilter.row(1)).isEqualTo(byIds.row(1));
      assertThat(byFilter.row(2)).isEqualTo(byIds.row(2));
      assertThat(byFilter.dataRows()).isEqualTo(2);
    }
  }

  @Test
  @DisplayName("R-011-02 other the export follows the selection ids and not the current page")
  void R_011_02_other_selectionAcrossPages() {
    List<String> ids = bulkContacts(ANA, gid, 40, i -> j("lastName", String.format("S%02d", i)));
    JsonNode p2 = search(ANA, gid, j("page", 2));
    String last = p2.at("/items/14/id").asText();
    String first = search(ANA, gid, j("page", 1)).at("/items/0/id").asText();
    try (Workbook w =
        new Workbook(export(j("language", "sl", "contactIds", arr(first, last))).expect(200))) {
      assertThat(w.dataRows()).isEqualTo(2);
    }
    assertThat(ids).contains(first, last);
  }

  @Test
  @DisplayName("R-011-03 invalid an export without contacts is refused")
  void R_011_03_invalid_noContacts() {
    contact("lastName", "A");
    Res r = export(j("language", "sl", "contactIds", arr()));
    assertThat(r.status()).isIn(400);
    export(j("language", "de", "contactIds", arr(java.util.UUID.randomUUID().toString())))
        .expect(400);
  }

  @Test
  @DisplayName(
      "R-011-04 combination the order follows the given sort and other groups are excluded")
  void R_011_04_combination_sortAndGroup() {
    contact("lastName", "Mm", "firstName", "Z");
    contact("lastName", "Aa", "firstName", "Y");
    String other = group(ANA, "Druga");
    JsonNode foreign = newContact(ANA, other, j("lastName", "Tuja"));
    try (Workbook w =
        new Workbook(
            export(
                    j(
                        "language",
                        "sl",
                        "sort",
                        j("field", "lastName", "direction", "DESC"),
                        "filter",
                        j()))
                .expect(200))) {
      assertThat(w.row(1).get(1)).isEqualTo("Mm");
      assertThat(w.row(2).get(1)).isEqualTo("Aa");
    }
    Res stranger = export(j("language", "sl", "contactIds", arr(foreign.path("id").asText())));
    if (stranger.status() == 200) {
      try (Workbook w = new Workbook(stranger)) {
        assertThat(w.dataRows()).isZero();
      }
    } else {
      assertThat(stranger.status()).isIn(400, 404);
    }
  }

  @Test
  @DisplayName(
      "R-011-05 combination attribute columns are alphabetical and the first spelling is used")
  void R_011_05_combination_attributeColumns() {
    contact("lastName", "A", "attributes", arr(attr("Zeta", "1"), attr("beta", "2")));
    contact("lastName", "B", "attributes", arr(attr("ZETA", "3"), attr("alfa", "4")));
    try (Workbook w =
        new Workbook(
            export(
                    j(
                        "language",
                        "en",
                        "sort",
                        j("field", "lastName", "direction", "ASC"),
                        "filter",
                        j()))
                .expect(200))) {
      assertThat(w.headers().subList(12, w.headers().size())).hasSize(3);
      assertThat(w.headers().get(12)).isEqualTo("alfa");
      assertThat(w.headers().get(13)).isEqualTo("beta");
      assertThat(w.headers().get(14)).isEqualToIgnoringCase("zeta");
      assertThat(w.text(2, 14)).isEqualTo("3");
      assertThat(w.text(1, 14)).isEqualTo("1");
    }
  }

  @Test
  @DisplayName("R-011-06 boundary file name characters and a long group name")
  void R_011_06_boundary_fileNameCharacters() {
    String g = group(ANA, "a\\b/c:d*e?f\"g<h>i|j");
    JsonNode c = newContact(ANA, g, j("lastName", "A"));
    try (Workbook w =
        new Workbook(
            post(
                    ANA,
                    "/groups/" + g + "/exports",
                    j("language", "sl", "contactIds", arr(c.path("id").asText())))
                .expect(200))) {
      assertThat(w.fileName()).startsWith("a_b_c_d_e_f_g_h_i_j_").endsWith(".xlsx");
      assertThat(w.fileName()).doesNotContain("\\", "/", ":", "*", "?", "\"", "<", ">", "|");
    }
  }

  @Test
  @DisplayName("R-011-07 other the header cells are text and e-mail cells stay text")
  void R_011_07_other_textCells() {
    JsonNode c =
        contact("lastName", "A", "email", "a@x.si", "phone", "0123456789", "postalCode", "01000");
    try (Workbook w =
        new Workbook(
            export(j("language", "sl", "contactIds", arr(c.path("id").asText()))).expect(200))) {
      for (int col = 0; col < 12; col++) {
        assertThat(w.cell(0, col).getCellType()).isEqualTo(CellType.STRING);
      }
      assertThat(w.text(1, w.column("Telefon"))).isEqualTo("0123456789");
      assertThat(w.text(1, w.column("Poštna številka"))).isEqualTo("01000");
    }
  }

  @Test
  @DisplayName("R-011-08 invalid formula starts in every text column and in attribute cells")
  void R_011_08_invalid_formulaStarts() {
    JsonNode c =
        contact(
            "lastName",
            "A",
            "firstName",
            "=1",
            "email",
            "+a@x.si",
            "street",
            "@s",
            "postalCode",
            "-1",
            "attributes",
            arr(attr("k", "=SUM(1)")));
    try (Workbook w =
        new Workbook(
            export(j("language", "sl", "contactIds", arr(c.path("id").asText()))).expect(200))) {
      for (int col = 0; col < w.headers().size(); col++) {
        assertThat(w.cell(1, col) == null || w.cell(1, col).getCellType() != CellType.FORMULA)
            .as("column %s", col)
            .isTrue();
      }
      assertThat(w.text(1, 0)).isEqualTo("=1");
      assertThat(w.text(1, w.column("k"))).isEqualTo("=SUM(1)");
    }
  }

  @Test
  @DisplayName(
      "R-011-09 boundary a filter result under the limit exports and the History records the count")
  void R_011_09_boundary_historyCount() {
    contact("lastName", "A");
    contact("lastName", "B");
    export(j("language", "sl", "filter", j())).expect(200);
    List<JsonNode> e = entries(ANA, gid, "EXPORTED");
    assertThat(e).hasSize(1);
    assertThat(e.get(0).toString()).contains("2");
  }

  @Test
  @DisplayName("R-011-10 role Owner and Editor may export and a Viewer or non-member may not")
  void R_011_10_role_exportRoles() {
    JsonNode c = contact("lastName", "A");
    ObjectNode body = j("language", "sl", "contactIds", arr(c.path("id").asText()));
    post(MARKO, "/groups/" + gid + "/exports", body).expect(200);
    post(PETRA, "/groups/" + gid + "/exports", body).expectError(403, "FORBIDDEN");
    post(BORIS, "/groups/" + gid + "/exports", body).expectError(404, "NOT_FOUND");
  }

  @Test
  @DisplayName("R-011-11 other accented characters of every Slovenian letter survive")
  void R_011_11_other_utf8() {
    JsonNode c = contact("lastName", "Čšž ČŠŽ ćđ", "firstName", "ÄÖÜß", "notes", "€ – “citat”");
    try (Workbook w =
        new Workbook(
            export(j("language", "sl", "contactIds", arr(c.path("id").asText()))).expect(200))) {
      assertThat(w.text(1, 1)).isEqualTo("Čšž ČŠŽ ćđ");
      assertThat(w.text(1, 0)).isEqualTo("ÄÖÜß");
      assertThat(w.text(1, 10)).isEqualTo("€ – “citat”");
    }
  }

  @Test
  @DisplayName(
      "R-011-12 combination trashed contacts are left out by filter and by ids and restored ones return")
  void R_011_12_combination_trashedLeftOut() {
    JsonNode a = contact("lastName", "A");
    JsonNode b = contact("lastName", "B");
    trash(ANA, gid, a.path("id").asText());
    try (Workbook w = new Workbook(export(j("language", "sl", "filter", j())).expect(200))) {
      assertThat(w.dataRows()).isEqualTo(1);
    }
    post(ANA, "/groups/" + gid + "/contacts/" + a.path("id").asText() + "/restore", null)
        .expect(204);
    try (Workbook w =
        new Workbook(
            export(
                    j(
                        "language",
                        "sl",
                        "contactIds",
                        arr(a.path("id").asText(), b.path("id").asText())))
                .expect(200))) {
      assertThat(w.dataRows()).isEqualTo(2);
    }
  }

  @Test
  @DisplayName(
      "R-012-01 combination a trashed contact leaves list search filters export and duplicates")
  void R_012_01_combination_leavesEverywhere() {
    JsonNode a = contact("lastName", "A", "email", "x@x.si", "tags", arr("t"));
    contact("lastName", "B", "email", "x@x.si", "tags", arr("t"));
    trash(MARKO, gid, a.path("id").asText());
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(1);
    assertThat(
            search(ANA, gid, j("conditions", arr(j("type", "TAGS_ALL", "tags", arr("t")))))
                .at("/total")
                .asInt())
        .isEqualTo(1);
    assertThat(post(ANA, "/groups/" + gid + "/duplicates/search", null).expect(200).at("/sets"))
        .isEmpty();
    assertThat(openContact(ANA, gid, a.path("id").asText()).path("email").asText())
        .isEqualTo("x@x.si");
  }

  @Test
  @DisplayName("R-012-02 combination days left decrease with the days and never go below zero")
  void R_012_02_combination_daysLeft() {
    JsonNode a = contact("lastName", "A");
    trash(ANA, gid, a.path("id").asText());
    int[] days = {0, 1, 10, 29};
    int[] expected = {30, 29, 20, 1};
    long advanced = 0;
    for (int i = 0; i < days.length; i++) {
      clock.advance(Duration.ofDays(days[i] - advanced));
      advanced = days[i];
      assertThat(
              get(ANA, "/groups/" + gid + "/trash-contacts")
                  .expect(200)
                  .at("/items/0/daysLeft")
                  .asInt())
          .as("after %s days", days[i])
          .isEqualTo(expected[i]);
    }
    JsonNode page = get(ANA, "/groups/" + gid + "/trash-contacts").expect(200).json();
    assertThat(page.at("/total").asInt()).isEqualTo(1);
    assertThat(page.at("/items/0/trashedAt").asText()).isNotBlank();
  }

  @Test
  @DisplayName("R-012-03 combination restore of a contact not in Trash and tags are kept")
  void R_012_03_combination_notInTrash() {
    JsonNode a = contact("lastName", "A", "tags", arr("t"), "attributes", arr(attr("k", "v")));
    post(ANA, "/groups/" + gid + "/contacts/" + a.path("id").asText() + "/restore", null)
        .expectError(409, "NOT_IN_TRASH");
    trash(ANA, gid, a.path("id").asText());
    post(MARKO, "/groups/" + gid + "/contacts/" + a.path("id").asText() + "/restore", null)
        .expect(204);
    JsonNode o = openContact(ANA, gid, a.path("id").asText());
    assertThat(strings(o.path("tags"))).containsExactly("t");
    assertThat(o.path("versionNumber").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-012-04 other deleting forever a contact that is not in Trash is refused")
  void R_012_04_other_notInTrashRefused() {
    JsonNode a = contact("lastName", "A");
    delete(ANA, "/groups/" + gid + "/contacts/" + a.path("id").asText())
        .expectError(409, "NOT_IN_TRASH");
    assertThat(openContact(ANA, gid, a.path("id").asText()).path("state").asText())
        .isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("R-012-05 role Editor Viewer and non-member cannot delete forever")
  void R_012_05_role_cannotEraseForever() {
    JsonNode a = contact("lastName", "A");
    trash(ANA, gid, a.path("id").asText());
    String p = "/groups/" + gid + "/contacts/" + a.path("id").asText();
    delete(MARKO, p).expectError(403, "FORBIDDEN");
    delete(PETRA, p).expectError(403, "FORBIDDEN");
    delete(BORIS, p).expectError(404, "NOT_FOUND");
    assertThat(get(ANA, "/groups/" + gid + "/trash-contacts").expect(200).at("/total").asInt())
        .isEqualTo(1);
  }

  @Test
  @DisplayName(
      "R-012-06 combination the cleanup erases only expired contacts and keeps younger ones")
  void R_012_06_combination_onlyExpired() {
    JsonNode old = contact("lastName", "Stara");
    JsonNode young = contact("lastName", "Mlada");
    trash(ANA, gid, old.path("id").asText());
    clock.advance(Duration.ofDays(20));
    trash(ANA, gid, young.path("id").asText());
    clock.advance(Duration.ofDays(10).plusMinutes(1));
    eventually(
        () -> get(ANA, "/groups/" + gid + "/contacts/" + old.path("id").asText()).status() == 404,
        "the old contact is erased");
    assertThat(openContact(ANA, gid, young.path("id").asText()).path("state").asText())
        .isEqualTo("TRASHED");
  }

  @Test
  @DisplayName("R-012-07 other no field of a trashed contact can be changed")
  void R_012_07_other_noChange() {
    JsonNode a = contact("lastName", "A", "company", "Acme");
    JsonNode opened = openContact(ANA, gid, a.path("id").asText());
    trash(ANA, gid, a.path("id").asText());
    save(ANA, gid, opened, "tags", arr("x")).expectError(409, "CONTACT_IN_TRASH");
    save(ANA, gid, opened, "attributes", arr(attr("k", "v"))).expectError(409, "CONTACT_IN_TRASH");
    assertThat(openContact(ANA, gid, a.path("id").asText()).path("company").asText())
        .isEqualTo("Acme");
  }

  @Test
  @DisplayName("R-012-08 role a Viewer is refused delete and restore before state checks")
  void R_012_08_role_viewerRefused() {
    JsonNode a = contact("lastName", "A");
    String p = "/groups/" + gid + "/contacts/" + a.path("id").asText();
    post(PETRA, p + "/restore", null).expectError(403, "FORBIDDEN");
    post(PETRA, p + "/trash", null).expectError(403, "FORBIDDEN");
  }

  @Test
  @DisplayName("R-012-09 other deleting and restoring write entries by the acting user")
  void R_012_09_other_entriesByActor() {
    JsonNode a = contact("lastName", "A");
    trash(MARKO, gid, a.path("id").asText());
    post(ANA, "/groups/" + gid + "/contacts/" + a.path("id").asText() + "/restore", null)
        .expect(204);
    assertThat(entries(ANA, gid, "CONTACT_DELETED").get(0).at("/actor/name").asText())
        .isEqualTo("Marko Pirc");
    assertThat(entries(ANA, gid, "CONTACT_RESTORED").get(0).at("/actor/name").asText())
        .isEqualTo("Ana Novak");
  }

  @Test
  @DisplayName(
      "BR-04 combination a restore after 30 days but before the cleanup and the erasure after it")
  void BR_04_combination_thirtyDays() {
    JsonNode a = contact("lastName", "A");
    trash(ANA, gid, a.path("id").asText());
    clock.advance(Duration.ofDays(29).plusHours(23).plusMinutes(59));
    post(ANA, "/groups/" + gid + "/contacts/" + a.path("id").asText() + "/restore", null)
        .expect(204);
    trash(ANA, gid, a.path("id").asText());
    clock.advance(Duration.ofDays(30).plusMinutes(1));
    eventually(
        () -> get(ANA, "/groups/" + gid + "/contacts/" + a.path("id").asText()).status() == 404,
        "erased 30 days after the second deletion");
  }

  @Test
  @DisplayName(
      "BR-06 combination erasing deletes tags attributes versions and anonymises the History")
  void BR_06_combination_erasure() {
    JsonNode a =
        contact(
            "lastName",
            "Skrivna",
            "email",
            "skrivna@example.com",
            "tags",
            arr("t"),
            "attributes",
            arr(attr("k", "skrivnost")));
    save(ANA, gid, openContact(ANA, gid, a.path("id").asText()), "phone", "+386 40 000 000")
        .expect(200);
    trash(ANA, gid, a.path("id").asText());
    delete(ANA, "/groups/" + gid + "/contacts/" + a.path("id").asText()).expect(204);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM contact_versions", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM attributes", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM contact_tags", Integer.class)).isZero();
    String h = history(ANA, gid).expect(200).text();
    assertThat(h).doesNotContain("Skrivna", "skrivna@example.com", "skrivnost", "+386 40 000 000");
    assertThat(h).contains("CONTACT_ERASED", "Ana Novak");
  }
}
