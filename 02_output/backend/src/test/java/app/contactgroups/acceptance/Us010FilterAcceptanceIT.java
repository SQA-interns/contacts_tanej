package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * US-010 Filter contacts: AC-010-01 to AC-010-10 (API part), EX-010-01 to EX-010-06 and EX-010-08.
 */
class Us010FilterAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
  }

  private JsonNode contact(Object... pairs) {
    return newContact(ANA, gid, j(pairs));
  }

  private JsonNode filter(ObjectNode... conditions) {
    return search(ANA, gid, j("conditions", arr((Object[]) conditions)));
  }

  private static ObjectNode tagsAll(String... tags) {
    return j("type", "TAGS_ALL", "tags", arr((Object[]) tags));
  }

  private static ObjectNode tagsAny(String... tags) {
    return j("type", "TAGS_ANY", "tags", arr((Object[]) tags));
  }

  private static ObjectNode tagNot(String tag) {
    return j("type", "TAG_NOT", "tag", tag);
  }

  private List<String> lastNames(JsonNode page) {
    return rowValues(page, "lastName");
  }

  @Test
  void AC_010_01_hasATagAndDoesNotHaveAnotherListsTwoContacts() {
    contact("lastName", "A", "tags", arr("udelezenec2026"));
    contact("lastName", "B", "tags", arr("udelezenec2026", "neObvescaj"));
    contact("lastName", "C", "tags", arr("udelezenec2026"));
    contact("lastName", "D");
    contact("lastName", "E", "tags", arr("drugo"));
    JsonNode r = filter(tagsAll("udelezenec2026"), tagNot("neObvescaj"));
    assertThat(r.at("/total").asInt()).isEqualTo(2);
    assertThat(lastNames(r)).containsExactlyInAnyOrder("A", "C");
  }

  @Test
  void AC_010_02_allOfAndAnyOfTags() {
    contact("lastName", "A", "tags", arr("a"));
    contact("lastName", "B", "tags", arr("b"));
    contact("lastName", "C", "tags", arr("a", "b"));
    assertThat(lastNames(filter(tagsAll("a", "b")))).containsExactly("C");
    assertThat(lastNames(filter(tagsAny("a", "b")))).containsExactlyInAnyOrder("A", "B", "C");
  }

  @Test
  void AC_010_03_anAttributeEqualsIgnoresCaseOfKeyAndCaseAndAccentsOfValue() {
    contact("lastName", "Novak", "attributes", arr(attr("kompetenca", "spletnoProgramiranje")));
    contact("lastName", "Horvat", "attributes", arr(attr("kompetenca", "java")));
    JsonNode r =
        filter(j("type", "ATTRIBUTE_EQUALS", "key", "Kompetenca", "value", "SPLETNOPROGRAMIRANJE"));
    assertThat(lastNames(r)).containsExactly("Novak");
  }

  @Test
  void AC_010_04_keyExistsListsAContactWithAnEmptyValueToo() {
    contact("lastName", "A", "attributes", arr(attr("kompetenca", "x")));
    contact("lastName", "B", "attributes", arr(attr("kompetenca", "y")));
    contact("lastName", "C", "attributes", arr(attr("leto", "")));
    assertThat(lastNames(filter(j("type", "ATTRIBUTE_EXISTS", "key", "LETO"))))
        .containsExactly("C");
  }

  @Test
  void AC_010_05_aCountryConditionListsOnlyThatCountry() {
    contact("lastName", "A", "country", "SI");
    contact("lastName", "B", "country", "SI");
    contact("lastName", "C", "country", "AT");
    assertThat(lastNames(filter(j("type", "COUNTRY", "country", "SI"))))
        .containsExactlyInAnyOrder("A", "B");
  }

  @Test
  void AC_010_06_aCompanyConditionIgnoresCaseAndAccents() {
    contact("lastName", "A", "company", "Acme");
    contact("lastName", "B", "company", "Šola");
    assertThat(lastNames(filter(j("type", "COMPANY", "company", "acme")))).containsExactly("A");
    assertThat(lastNames(filter(j("type", "COMPANY", "company", "sola")))).containsExactly("B");
  }

  @Test
  void AC_010_07_aFilterAndASearchTextCombineWithAnd() {
    contact("lastName", "Novak", "tags", arr("x"));
    contact("lastName", "Novak", "firstName", "Eva");
    contact("lastName", "Horvat", "tags", arr("x"));
    JsonNode r = search(ANA, gid, j("q", "novak", "conditions", arr(tagsAll("x"))));
    assertThat(r.at("/total").asInt()).isEqualTo(1);
    assertThat(lastNames(r)).containsExactly("Novak");
  }

  @Test
  void AC_010_08_removingTheFilterReturnsTheFullList() {
    contact("lastName", "A");
    assertThat(filter(tagsAll("nobody")).at("/total").asInt()).isZero();
    assertThat(filter().at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, gid, j()).at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void AC_010_09_theActiveConditionsOfARequestAreAppliedOneByOne() {
    contact("lastName", "A", "tags", arr("x"), "country", "SI");
    contact("lastName", "B", "tags", arr("x"), "country", "AT");
    assertThat(filter(tagsAll("x")).at("/total").asInt()).isEqualTo(2);
    assertThat(filter(tagsAll("x"), j("type", "COUNTRY", "country", "AT")).at("/total").asInt())
        .isEqualTo(1);
  }

  @Test
  void AC_010_10_onlyTheSevenConditionTypesExist() {
    contact("lastName", "A");
    for (String type : List.of("NAME_STARTS_WITH", "SAVED_FILTER", "PHONE_CONTAINS", "NOTES")) {
      post(
              ANA,
              "/groups/" + gid + "/contacts/search",
              j("conditions", arr(j("type", type, "value", "A"))))
          .expectError(400, "VALIDATION_FAILED");
    }
  }

  @Test
  void EX_010_01_doesNotHaveATagNobodyHasListsEveryContact() {
    contact("lastName", "A");
    contact("lastName", "B", "tags", arr("x"));
    assertThat(filter(tagNot("nobody")).at("/total").asInt()).isEqualTo(2);
  }

  @Test
  void EX_010_02_hasAllOfAnUnknownTagListsNothing() {
    contact("lastName", "A", "tags", arr("x"));
    JsonNode r = filter(tagsAll("x", "unknown"));
    assertThat(r.at("/total").asInt()).isZero();
    assertThat(r.path("items")).isEmpty();
  }

  @Test
  void EX_010_03_aKeyNoContactHasListsNothing() {
    contact("lastName", "A", "attributes", arr(attr("kompetenca", "x")));
    assertThat(filter(j("type", "ATTRIBUTE_EXISTS", "key", "nikoli")).at("/total").asInt())
        .isZero();
  }

  @Test
  void EX_010_04_aContactInTrashIsNotListedEvenIfItSatisfiesTheFilter() {
    JsonNode c = contact("lastName", "A", "tags", arr("x"));
    trash(ANA, gid, c.path("id").asText());
    assertThat(filter(tagsAll("x")).at("/total").asInt()).isZero();
  }

  @Test
  void EX_010_05_aTagOfAnotherGroupMatchesNothingAndRevealsNothing() {
    String other = group(BORIS, "Poroka");
    newContact(BORIS, other, j("lastName", "Tuj", "tags", arr("samoB")));
    contact("lastName", "A", "tags", arr("x"));
    JsonNode r = filter(tagsAll("samoB"));
    assertThat(r.at("/total").asInt()).isZero();
    assertThat(r.toString()).doesNotContain("Tuj");
  }

  @Test
  void EX_010_06_aValueWithPercentOrQuoteIsPlainText() {
    contact("lastName", "A", "attributes", arr(attr("k", "50%")));
    contact("lastName", "B", "attributes", arr(attr("k", "it's")));
    contact("lastName", "C", "attributes", arr(attr("k", "500")));
    assertThat(lastNames(filter(j("type", "ATTRIBUTE_EQUALS", "key", "k", "value", "50%"))))
        .containsExactly("A");
    assertThat(lastNames(filter(j("type", "ATTRIBUTE_EQUALS", "key", "k", "value", "it's"))))
        .containsExactly("B");
  }

  @Test
  void EX_010_08_aContactWithAnEmptyCountryNeverMatchesACountryCondition() {
    contact("lastName", "A");
    contact("lastName", "B", "country", "SI");
    assertThat(lastNames(filter(j("type", "COUNTRY", "country", "SI")))).containsExactly("B");
  }
}
