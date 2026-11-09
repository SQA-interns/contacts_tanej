package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * US-009 List and search contacts: AC-009-01 to AC-009-11, EX-009-01 to EX-009-07 and EX-009-09.
 */
class Us009ListAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private void many(int n) {
    bulkContacts(
        ANA, gid, n, i -> j("firstName", "Ime", "lastName", String.format("Priimek%02d", i)));
  }

  private List<String> lastNames(JsonNode page) {
    return rowValues(page, "lastName");
  }

  @Test
  void AC_009_01_aGroupOf60ContactsShows25PerPageSortedWithTheTotal() {
    many(60);
    JsonNode p1 = search(PETRA, gid, j("page", 1));
    assertThat(p1.at("/total").asInt()).isEqualTo(60);
    assertThat(p1.at("/pageSize").asInt()).isEqualTo(25);
    assertThat(p1.path("items")).hasSize(25);
    assertThat(lastNames(p1)).isSorted();
    assertThat(lastNames(p1).get(0)).isEqualTo("Priimek00");
    JsonNode p3 = search(PETRA, gid, j("page", 3));
    assertThat(p3.path("items")).hasSize(10);
    assertThat(lastNames(p3).get(0)).isEqualTo("Priimek50");
  }

  @Test
  void AC_009_02_aSearchIgnoresAccents() {
    newContact(ANA, gid, "Ana", "Škerlj");
    newContact(ANA, gid, "Eva", "Horvat");
    JsonNode r = search(ANA, gid, "skerlj");
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    assertThat(lastNames(r)).containsExactly("Škerlj");
  }

  @Test
  void AC_009_03_everyWordMustBePartOfAField() {
    newContact(ANA, gid, j("firstName", "Ana", "lastName", "Novak", "company", "Acme"));
    assertThat(search(ANA, gid, "ana no").at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, gid, "ana xx").at("/total").asInt()).isZero();
  }

  @Test
  void AC_009_04_aSearchFindsTagNamesAndAttributeValues() {
    newContact(
        ANA,
        gid,
        j(
            "firstName",
            "Ana",
            "lastName",
            "Novak",
            "tags",
            arr("udelezenec2025"),
            "attributes",
            arr(attr("kompetenca", "spletnoProgramiranje"))));
    assertThat(search(ANA, gid, "udelez").at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, gid, "spletno").at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void AC_009_05_phoneAndNotesAreNotSearched() {
    newContact(
        ANA,
        gid,
        j("firstName", "Ana", "lastName", "Novak", "phone", "+386 41 555 123", "notes", "vip"));
    assertThat(search(ANA, gid, "555").at("/total").asInt()).isZero();
    assertThat(search(ANA, gid, "vip").at("/total").asInt()).isZero();
  }

  @Test
  void AC_009_06_aSearchWithoutAMatchGivesAnEmptyPageAndTheFullListReturnsWithoutIt() {
    newContact(ANA, gid, "Ana", "Novak");
    JsonNode none = search(ANA, gid, "zzzz");
    assertThat(none.at("/total").asInt()).isZero();
    assertThat(none.path("items")).isEmpty();
    assertThat(search(ANA, gid, j("q", "")).at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void AC_009_07_aNewGroupHasNoContactsForAnyRole() {
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
    assertThat(search(PETRA, gid, "").at("/total").asInt()).isZero();
    assertThat(get(ANA, "/groups/" + gid).at("/role").asText()).isEqualTo("OWNER");
    assertThat(get(PETRA, "/groups/" + gid).at("/role").asText()).isEqualTo("VIEWER");
  }

  @Test
  void AC_009_08_aContactInTrashIsNeitherListedNorFound() {
    JsonNode c = newContact(ANA, gid, "Ana", "Novak");
    trash(MARKO, gid, c.path("id").asText());
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
    assertThat(search(ANA, gid, "novak").at("/total").asInt()).isZero();
  }

  @Test
  void AC_009_09_slovenianSortingPutsCSZAfterCSZ() {
    for (String l : List.of("Zupan", "Šuster", "Sever", "Čuk", "Cerar")) {
      newContact(ANA, gid, "Ana", l);
    }
    assertThat(lastNames(search(ANA, gid, j("sort", j("field", "lastName", "direction", "ASC")))))
        .containsExactly("Cerar", "Čuk", "Sever", "Šuster", "Zupan");
    assertThat(lastNames(search(ANA, gid, j("sort", j("field", "lastName", "direction", "DESC")))))
        .containsExactly("Zupan", "Šuster", "Sever", "Čuk", "Cerar");
  }

  @Test
  void AC_009_10_searchAndSortWorkTogetherOnALaterPage() {
    for (int i = 0; i < 30; i++) {
      newContact(
          ANA,
          gid,
          j("lastName", "Novak", "firstName", "N" + i, "city", String.format("Mesto%02d", 29 - i)));
    }
    newContact(ANA, gid, j("lastName", "Horvat", "city", "Aaa"));
    JsonNode p2 =
        search(
            ANA, gid, j("q", "novak", "sort", j("field", "city", "direction", "ASC"), "page", 2));
    assertThat(p2.at("/total").asInt()).isEqualTo(30);
    assertThat(p2.at("/page").asInt()).isEqualTo(2);
    assertThat(p2.path("items")).hasSize(5);
    assertThat(rowValues(p2, "city"))
        .containsExactly("Mesto25", "Mesto26", "Mesto27", "Mesto28", "Mesto29");
    assertThat(lastNames(p2)).containsOnly("Novak");
  }

  @Test
  void AC_009_11_aViewerGetsTheSameResultAsAnEditor() {
    many(30);
    var request = j("q", "priimek", "sort", j("field", "lastName", "direction", "DESC"), "page", 2);
    assertThat(search(PETRA, gid, request).toString())
        .isEqualTo(search(MARKO, gid, request).toString());
  }

  @Test
  void EX_009_01_25ContactsAreOnePageAnd26AreTwo() {
    many(25);
    assertThat(search(ANA, gid, j("page", 1)).path("items")).hasSize(25);
    assertThat(search(ANA, gid, j("page", 2)).at("/page").asInt()).isEqualTo(1);
    newContact(ANA, gid, "Ana", "Zadnja");
    JsonNode p2 = search(ANA, gid, j("page", 2));
    assertThat(p2.at("/total").asInt()).isEqualTo(26);
    assertThat(p2.path("items")).hasSize(1);
  }

  @Test
  void EX_009_02_anEmptySearchTextGivesTheFullList() {
    many(3);
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(3);
    assertThat(search(ANA, gid, j()).at("/total").asInt()).isEqualTo(3);
  }

  @Test
  void EX_009_03_percentUnderscoreAndSqlTextAreSearchedAsPlainText() {
    newContact(ANA, gid, j("lastName", "Novak", "company", "100% Pure"));
    newContact(ANA, gid, j("lastName", "Horvat", "company", "a_b"));
    newContact(ANA, gid, j("lastName", "Kos", "company", "Acme"));
    assertThat(lastNames(search(ANA, gid, "%"))).containsExactly("Novak");
    assertThat(lastNames(search(ANA, gid, "_"))).containsExactly("Horvat");
    assertThat(search(ANA, gid, "'; DROP TABLE").at("/total").asInt()).isZero();
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(3);
  }

  @Test
  void EX_009_04_aSearchOfOnlySpacesIsTreatedAsEmpty() {
    many(3);
    assertThat(search(ANA, gid, "   ").at("/total").asInt()).isEqualTo(3);
  }

  @Test
  void EX_009_05_upperAndLowerCaseWithAccentsGiveTheSameResult() {
    newContact(ANA, gid, "Ana", "Škerlj");
    assertThat(search(ANA, gid, "ŠKERLJ").toString())
        .isEqualTo(search(ANA, gid, "škerlj").toString());
    assertThat(search(ANA, gid, "ŠKERLJ").at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void EX_009_06_theOrderOfEqualNamesIsStableBetweenPages() {
    for (int i = 0; i < 30; i++) {
      newContact(
          ANA, gid, j("firstName", "Ana", "lastName", "Novak", "email", "n" + i + "@example.com"));
    }
    List<String> first = new ArrayList<>(rowValues(search(ANA, gid, j("page", 1)), "id"));
    first.addAll(rowValues(search(ANA, gid, j("page", 2)), "id"));
    Set<String> distinct = new HashSet<>(first);
    assertThat(distinct).hasSize(30);
    List<String> again = new ArrayList<>(rowValues(search(ANA, gid, j("page", 1)), "id"));
    again.addAll(rowValues(search(ANA, gid, j("page", 2)), "id"));
    assertThat(again).isEqualTo(first);
  }

  @Test
  void EX_009_07_aContactWithoutALastNameSortsLastAscending() {
    newContact(ANA, gid, j("firstName", "Brez"));
    newContact(ANA, gid, "Ana", "Zupan");
    newContact(ANA, gid, "Eva", "Cerar");
    JsonNode asc = search(ANA, gid, j("sort", j("field", "lastName", "direction", "ASC")));
    assertThat(asc.at("/items/2/firstName").asText()).isEqualTo("Brez");
    assertThat(asc.at("/items/0/lastName").asText()).isEqualTo("Cerar");
  }

  @Test
  void EX_009_09_aPageBeyondTheLastShowsTheLastPage() {
    many(30);
    JsonNode beyond = search(ANA, gid, j("page", 9));
    assertThat(beyond.at("/page").asInt()).isEqualTo(2);
    assertThat(beyond.path("items")).hasSize(5);
  }
}
