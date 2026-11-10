package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContactRulesTest {

  private static ContactInput with(String first, String last, String email, String company) {
    return new ContactInput(first, last, email, "", company, "", "", "", "", "", "");
  }

  private static List<FieldProblem> problems(ContactInput input) {
    try {
      ContactRules.validate(input);
    } catch (ApiException e) {
      assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
      return e.fields();
    }
    throw new AssertionError("The input was accepted");
  }

  @Test
  @DisplayName("R-005-02 AC-005-02 EX-005-02: only spaces and a phone is refused as one-of-four")
  void oneOfFourIsRequired() {
    ContactInput onlyPhone =
        new ContactInput("   ", "", "", "+386 40 123 456", "", "", "", "", "", "", "");
    assertThat(problems(onlyPhone))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("firstName", "REQUIRED_ONE_OF"));
  }

  @Test
  @DisplayName("R-005-02 EX-005-01: a company alone is enough")
  void companyAloneIsEnough() {
    assertThat(ContactRules.validate(with("", "", "", "Acme")).company()).isEqualTo("Acme");
  }

  @Test
  @DisplayName("R-005-04 AC-005-03: the e-mail is stored in lowercase")
  void emailIsLowercase() {
    assertThat(ContactRules.validate(with("", "Novak", "ANA.Novak@Example.com", "")).email())
        .isEqualTo("ana.novak@example.com");
  }

  @Test
  @DisplayName("R-005-03 AC-005-04: an invalid e-mail names the e-mail field")
  void invalidEmail() {
    assertThat(problems(with("", "Novak", "ana.novak@", "")))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("email", "INVALID_FORMAT"));
  }

  @Test
  @DisplayName("R-005-03 EX-005-03 EX-005-04: 255 characters pass, 256 are refused")
  void textFieldLimit() {
    String ok = "x".repeat(255);
    assertThat(ContactRules.validate(with(ok, ok, "", ok)).company()).hasSize(255);
    assertThat(problems(with("", "Novak", "", "x".repeat(256))))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("company", "TOO_LONG"));
  }

  @Test
  @DisplayName("R-005-03 EX-005-05: notes of 5000 pass, 5001 are refused")
  void notesLimit() {
    ContactInput ok = with("", "Novak", "", "").withNotes("n".repeat(5000));
    assertThat(ContactRules.validate(ok).notes()).hasSize(5000);
    assertThat(problems(ok.withNotes("n".repeat(5001))))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("notes", "TOO_LONG"));
  }

  @Test
  @DisplayName("R-005-03 EX-005-06 EX-005-07: letters in a phone are refused, 040/123-456 is kept")
  void phoneCharacters() {
    ContactInput base = with("", "Novak", "", "");
    assertThat(problems(base.withPhone("abc")))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("phone", "INVALID_CHARACTERS"));
    assertThat(ContactRules.validate(base.withPhone("040/123-456")).phone())
        .isEqualTo("040/123-456");
  }

  @Test
  @DisplayName("R-005-03 a phone longer than 30 characters is refused")
  void phoneLength() {
    assertThat(problems(with("", "Novak", "", "").withPhone("1".repeat(31))))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("phone", "TOO_LONG"));
  }

  @Test
  @DisplayName("R-005-03 AC-005-10 EX-005-08: a country outside the ISO list is refused")
  void country() {
    ContactInput base = with("", "Novak", "", "");
    assertThat(ContactRules.validate(base.withCountry("SI")).country()).isEqualTo("SI");
    assertThat(problems(base.withCountry("Slovenija")))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("country", "UNKNOWN_COUNTRY"));
    assertThat(problems(base.withCountry("XX")))
        .extracting(FieldProblem::code)
        .containsExactly("UNKNOWN_COUNTRY");
  }

  @Test
  @DisplayName("R-005-03 EX-005-10: every broken field is named in one refusal")
  void allProblemsTogether() {
    assertThat(problems(with("", "Novak", "ana.novak@", "").withCountry("Slovenija")))
        .extracting(FieldProblem::field)
        .containsExactlyInAnyOrder("email", "country");
  }
}
