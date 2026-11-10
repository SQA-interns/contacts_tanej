package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExportLayoutTest {

  private static Contact withAttributes(Contact.Attribute... attributes) {
    return new Contact(
        UUID.randomUUID(),
        UUID.randomUUID(),
        ContactState.ACTIVE,
        null,
        null,
        1,
        1,
        new ContactInput("", "", "", "", "", "", "", "", "", "", ""),
        List.of(),
        List.of(attributes));
  }

  @Test
  void theFileNameReplacesCharactersThatAreNotAllowed() {
    assertThat(ExportLayout.fileName("A/B:C", LocalDate.of(2026, 7, 2)))
        .isEqualTo("A_B_C_2026-07-02.xlsx");
    assertThat(ExportLayout.fileName("a\\b*c?\"d<e>f|g\th", LocalDate.of(2026, 1, 1)))
        .isEqualTo("a_b_c__d_e_f_g_h_2026-01-01.xlsx");
  }

  @Test
  void theFileNameKeepsLettersWithAccents() {
    assertThat(ExportLayout.fileName("Čebelarji", LocalDate.of(2026, 1, 1)))
        .isEqualTo("Čebelarji_2026-01-01.xlsx");
  }

  @Test
  void headersAreTheElevenFieldsAndTagsFollowedByTheAttributeKeys() {
    assertThat(ExportLayout.headers("en", List.of("kompetenca")))
        .hasSize(13)
        .startsWith("First name")
        .contains("Tags")
        .endsWith("kompetenca");
    assertThat(ExportLayout.headers("sl", List.of())).hasSize(12).startsWith("Ime");
  }

  @Test
  void countryNamesFollowTheLanguage() {
    assertThat(ExportLayout.countryName("SI", "sl")).isEqualTo("Slovenija");
    assertThat(ExportLayout.countryName("SI", "en")).isEqualTo("Slovenia");
    assertThat(ExportLayout.countryName("", "en")).isEmpty();
  }

  @Test
  void tagsAreSortedBySlovenianRulesAndJoinedByCommaAndSpace() {
    assertThat(ExportLayout.tagsCell(List.of("b", "a"))).isEqualTo("a, b");
    assertThat(ExportLayout.tagsCell(List.of("š", "s", "z", "č", "c"))).isEqualTo("c, č, s, š, z");
    assertThat(ExportLayout.tagsCell(List.of())).isEmpty();
  }

  @Test
  void attributeKeysDifferingInCaseAreOneColumnInTheSpellingOfTheFirstContact() {
    var first = withAttributes(new Contact.Attribute("Kompetenca", "x"));
    var second =
        withAttributes(
            new Contact.Attribute("kompetenca", "y"), new Contact.Attribute("avto", "z"));
    assertThat(ExportLayout.attributeKeys(List.of(first, second)))
        .containsExactly("avto", "Kompetenca");
  }

  @Test
  void onlyTheFourListedCharactersLookLikeAFormula() {
    for (String text : List.of("=1", "+1", "-1", "@x")) {
      assertThat(ExportLayout.looksLikeFormula(text)).as(text).isTrue();
    }
    for (String text : List.of("", "a=b", " =1", "\t=1", "1+2")) {
      assertThat(ExportLayout.looksLikeFormula(text)).as(text).isFalse();
    }
  }
}
