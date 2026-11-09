package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * US-011 Export (API part): AC-011-01 to AC-011-04, AC-011-06, AC-011-08 to AC-011-13, EX-011-02 to
 * EX-011-08. The tick boxes, the disabled button and the selection (AC-011-05, AC-011-07) are in
 * the browser acceptance tests; the 10,000 limit is in LargeGroupAcceptanceIT.
 */
class Us011ExportAcceptanceIT extends AcceptanceBase {

  private static final ZoneId LJUBLJANA = ZoneId.of("Europe/Ljubljana");
  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private Res export(Who who, ObjectNode body) {
    return post(who, "/groups/" + gid + "/exports", body);
  }

  private Res exportIds(Who who, String language, List<String> ids) {
    return export(who, j("language", language, "contactIds", arr(ids.toArray())));
  }

  private String id(JsonNode c) {
    return c.path("id").asText();
  }

  private String today() {
    return ZonedDateTime.ofInstant(clock.instant(), LJUBLJANA).toLocalDate().toString();
  }

  @Test
  void AC_011_01_aFilteredSelectionIsDownloadedWithAHeaderAndItsRowsAndTheDatedFileName() {
    for (String l : List.of("A", "B", "C")) {
      newContact(ANA, gid, j("lastName", l, "tags", arr("udelezenec2026")));
    }
    newContact(ANA, gid, j("lastName", "D", "tags", arr("udelezenec2026", "neObvescaj")));
    newContact(ANA, gid, j("lastName", "E"));
    ObjectNode filter =
        j(
            "conditions",
            arr(
                j("type", "TAGS_ALL", "tags", arr("udelezenec2026")),
                j("type", "TAG_NOT", "tag", "neObvescaj")));
    Res r = export(MARKO, j("language", "sl", "filter", filter)).expect(200);
    try (Workbook w = new Workbook(r)) {
      assertThat(w.fileName()).isEqualTo("Konferenca 2026_" + today() + ".xlsx");
      assertThat(w.sheets()).isEqualTo(1);
      assertThat(w.dataRows()).isEqualTo(3);
    }
  }

  @Test
  void AC_011_02_columnsAreTheElevenFieldsTagsAndTheAttributeKeysOfTheSelection() {
    JsonNode ana =
        newContact(
            ANA,
            gid,
            j(
                "firstName",
                "Ana",
                "lastName",
                "Novak",
                "email",
                "ana.novak@example.com",
                "tags",
                arr("udelezenec2025"),
                "attributes",
                arr(attr("kompetenca", "spletnoProgramiranje"))));
    JsonNode other = newContact(ANA, gid, j("firstName", "Eva", "lastName", "Horvat"));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(ana), id(other))).expect(200))) {
      assertThat(w.headers())
          .containsExactly(
              "Ime",
              "Priimek",
              "E-pošta",
              "Telefon",
              "Podjetje",
              "Delovno mesto",
              "Ulica in hišna številka",
              "Poštna številka",
              "Kraj",
              "Država",
              "Opombe",
              "Oznake",
              "kompetenca");
      // default order is the list sort: last name, so Horvat before Novak
      assertThat(w.row(1).get(1)).isEqualTo("Horvat");
      assertThat(w.row(1).get(11)).isEmpty();
      assertThat(w.row(1).get(12)).isEmpty();
      assertThat(w.row(2).get(1)).isEqualTo("Novak");
      assertThat(w.row(2).get(11)).isEqualTo("udelezenec2025");
      assertThat(w.row(2).get(12)).isEqualTo("spletnoProgramiranje");
    }
  }

  @Test
  void AC_011_03_theTagsCellListsTheTagsSeparatedByCommaAndSpace() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "tags", arr("b", "a")));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(c))).expect(200))) {
      assertThat(w.text(1, 11)).isEqualTo("a, b");
    }
  }

  @Test
  void AC_011_04_theHeadersFollowTheLanguageOfTheRequest() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "country", "SI"));
    try (Workbook w = new Workbook(exportIds(ANA, "en", List.of(id(c))).expect(200))) {
      assertThat(w.headers())
          .containsExactly(
              "First name",
              "Last name",
              "E-mail",
              "Phone",
              "Company",
              "Job title",
              "Street and house number",
              "Postal code",
              "City",
              "Country",
              "Notes",
              "Tags");
      assertThat(w.text(1, 9)).isEqualTo("Slovenia");
    }
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(c))).expect(200))) {
      assertThat(w.text(1, 9)).isEqualTo("Slovenija");
    }
  }

  @Test
  void AC_011_06_aSelectionFromTwoPagesGivesAllItsRows() {
    List<String> ids =
        bulkContacts(
            ANA, gid, 60, i -> j("firstName", "Ime", "lastName", String.format("P%02d", i)));
    List<String> picked = new ArrayList<>();
    JsonNode p1 = search(ANA, gid, j("page", 1));
    JsonNode p2 = search(ANA, gid, j("page", 2));
    for (int i = 0; i < 3; i++) {
      picked.add(p1.at("/items/" + i + "/id").asText());
    }
    for (int i = 0; i < 2; i++) {
      picked.add(p2.at("/items/" + i + "/id").asText());
    }
    assertThat(ids).containsAll(picked);
    try (Workbook w = new Workbook(exportIds(ANA, "sl", picked).expect(200))) {
      assertThat(w.dataRows()).isEqualTo(5);
    }
  }

  @Test
  void AC_011_08_valuesThatLookLikeFormulasAreWrittenAsPlainText() {
    JsonNode c =
        newContact(
            ANA,
            gid,
            j(
                "lastName", "Novak",
                "notes", "=HYPERLINK(\"http://x\")",
                "company", "+evil",
                "jobTitle", "-1",
                "city", "@x"));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(c))).expect(200))) {
      for (int col :
          new int[] {
            w.column("Opombe"), w.column("Podjetje"), w.column("Delovno mesto"), w.column("Kraj")
          }) {
        assertThat(w.cell(1, col).getCellType()).isEqualTo(CellType.STRING);
      }
      assertThat(w.text(1, w.column("Opombe"))).isEqualTo("=HYPERLINK(\"http://x\")");
      assertThat(w.text(1, w.column("Podjetje"))).isEqualTo("+evil");
      assertThat(w.text(1, w.column("Delovno mesto"))).isEqualTo("-1");
      assertThat(w.text(1, w.column("Kraj"))).isEqualTo("@x");
    }
  }

  @Test
  void AC_011_10_aViewerCannotExport() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    exportIds(PETRA, "sl", List.of(id(c))).expectError(403, "FORBIDDEN");
  }

  @Test
  void AC_011_11_emailAndPhoneAreTextAndAccentsAreKept() {
    JsonNode c =
        newContact(
            ANA,
            gid,
            j(
                "firstName",
                "Žiga",
                "lastName",
                "Čuk",
                "email",
                "ziga.cuk@example.com",
                "phone",
                "+386 41 555 123"));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(c))).expect(200))) {
      assertThat(w.cell(1, w.column("E-pošta")).getCellType()).isEqualTo(CellType.STRING);
      assertThat(w.cell(1, w.column("Telefon")).getCellType()).isEqualTo(CellType.STRING);
      assertThat(w.text(1, w.column("Telefon"))).isEqualTo("+386 41 555 123");
      assertThat(w.text(1, w.column("Ime"))).isEqualTo("Žiga");
      assertThat(w.text(1, w.column("Priimek"))).isEqualTo("Čuk");
    }
  }

  @Test
  void AC_011_12_aContactInTrashIsNotExportedBySelectAll() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A"));
    newContact(ANA, gid, j("lastName", "B"));
    trash(ANA, gid, id(a));
    try (Workbook w = new Workbook(export(ANA, j("language", "sl", "filter", j())).expect(200))) {
      assertThat(w.dataRows()).isEqualTo(1);
      assertThat(w.text(1, 1)).isEqualTo("B");
    }
  }

  @Test
  void AC_011_13_theHeaderRowIsBoldAndFrozen() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(c))).expect(200))) {
      assertThat(w.sheet().getPaneInformation()).isNotNull();
      assertThat(w.sheet().getPaneInformation().isFreezePane()).isTrue();
      assertThat(w.sheet().getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
      var font = w.sheet().getWorkbook().getFontAt(w.cell(0, 0).getCellStyle().getFontIndex());
      assertThat(font.getBold()).isTrue();
    }
  }

  @Test
  void EX_011_02_charactersNotAllowedInFileNamesBecomeUnderscores() {
    String slash = group(ANA, "A/B:C");
    JsonNode c = newContact(ANA, slash, j("lastName", "Novak"));
    Res r =
        post(ANA, "/groups/" + slash + "/exports", j("language", "sl", "contactIds", arr(id(c))))
            .expect(200);
    try (Workbook w = new Workbook(r)) {
      assertThat(w.fileName()).isEqualTo("A_B_C_" + today() + ".xlsx");
    }
  }

  @Test
  void EX_011_03_keysThatDifferOnlyInCaseGiveOneColumn() {
    JsonNode a =
        newContact(ANA, gid, j("lastName", "A", "attributes", arr(attr("Kompetenca", "x"))));
    JsonNode b =
        newContact(ANA, gid, j("lastName", "B", "attributes", arr(attr("kompetenca", "y"))));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(a), id(b))).expect(200))) {
      assertThat(w.headers()).hasSize(13);
      assertThat(w.headers().get(12)).isEqualToIgnoringCase("kompetenca");
      assertThat(w.text(1, 12)).isEqualTo("x");
      assertThat(w.text(2, 12)).isEqualTo("y");
    }
  }

  @Test
  void EX_011_04_noAttributesInTheSelectionGiveNoAttributeColumns() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A"));
    newContact(ANA, gid, j("lastName", "B", "attributes", arr(attr("kompetenca", "y"))));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(a))).expect(200))) {
      assertThat(w.headers()).hasSize(12);
    }
  }

  @Test
  void EX_011_05_theFileNameDateIsTheDateInLjubljanaNotInUtc() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    Instant target = Instant.parse("2026-07-01T22:30:00Z");
    clock.advance(Duration.between(clock.instant(), target));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(c))).expect(200))) {
      assertThat(w.fileName()).isEqualTo("Konferenca 2026_2026-07-02.xlsx");
    }
  }

  @Test
  void EX_011_06_onlyValuesStartingWithTheFourListedCharactersAreNeutralised() {
    JsonNode c =
        newContact(
            ANA,
            gid,
            j(
                "lastName",
                "Novak",
                "company",
                "a=b",
                "jobTitle",
                "1+2",
                "city",
                "=1+1",
                "street",
                "x@y"));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(c))).expect(200))) {
      assertThat(w.text(1, w.column("Podjetje"))).isEqualTo("a=b");
      assertThat(w.text(1, w.column("Delovno mesto"))).isEqualTo("1+2");
      assertThat(w.text(1, w.column("Kraj"))).isEqualTo("=1+1");
      assertThat(w.cell(1, w.column("Kraj")).getCellType()).isEqualTo(CellType.STRING);
      assertThat(w.text(1, w.column("Ulica in hišna številka"))).isEqualTo("x@y");
    }
  }

  @Test
  void EX_011_07_emptyFieldsGiveEmptyCells() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(c))).expect(200))) {
      assertThat(w.row(1).get(0)).isEmpty();
      assertThat(w.text(1, w.column("Telefon"))).isEmpty();
      assertThat(w.text(1, w.column("Opombe"))).isEmpty();
    }
  }

  @Test
  void EX_011_08_aSelectedContactTrashedMeanwhileIsLeftOut() {
    JsonNode a = newContact(ANA, gid, j("lastName", "A"));
    JsonNode b = newContact(ANA, gid, j("lastName", "B"));
    trash(MARKO, gid, id(b));
    try (Workbook w = new Workbook(exportIds(ANA, "sl", List.of(id(a), id(b))).expect(200))) {
      assertThat(w.dataRows()).isEqualTo(1);
      assertThat(w.text(1, 1)).isEqualTo("A");
    }
  }
}
