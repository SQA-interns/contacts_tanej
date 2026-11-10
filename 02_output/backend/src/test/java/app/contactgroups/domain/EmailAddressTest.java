package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** R-003-02, BR-05, BR-07: the e-mail rule and the lowercase form. */
class EmailAddressTest {

  private static void assertRefused(String raw, String code) {
    assertThatThrownBy(() -> EmailAddress.of(raw))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
              assertThat(e.fields()).extracting(FieldProblem::field).containsExactly("email");
              assertThat(e.fields()).extracting(FieldProblem::code).containsExactly(code);
            });
  }

  @Test
  void anAddressIsStoredInLowercaseWithoutSurroundingSpaces() {
    assertThat(EmailAddress.of("  Marko@Example.COM ")).isEqualTo("marko@example.com");
  }

  @Test
  void missingOrEmptyAddressesAreInvalid() {
    assertRefused(null, "INVALID_FORMAT");
    assertRefused("", "INVALID_FORMAT");
    assertRefused("   ", "INVALID_FORMAT");
  }

  @Test
  void exactlyOneAtSignIsRequired() {
    assertRefused("marko@@example", "INVALID_FORMAT");
    assertRefused("marko.example.com", "INVALID_FORMAT");
    assertRefused("a@b@example.com", "INVALID_FORMAT");
  }

  @Test
  void theLocalPartMustNotBeEmpty() {
    assertRefused("@example.com", "INVALID_FORMAT");
  }

  @Test
  void theDomainNeedsADotAndNoEmptyLabel() {
    assertRefused("marko@example", "INVALID_FORMAT");
    assertRefused("marko@.com", "INVALID_FORMAT");
    assertRefused("marko@example.", "INVALID_FORMAT");
    assertRefused("marko@a..com", "INVALID_FORMAT");
  }

  @Test
  void spacesInsideAreInvalid() {
    assertRefused("mar ko@example.com", "INVALID_FORMAT");
    assertRefused("marko@exa mple.com", "INVALID_FORMAT");
  }

  @Test
  void anAddressOfExactly255CharactersIsAcceptedAndLongerIsTooLong() {
    String ok = "a".repeat(255 - "@e.com".length()) + "@e.com";
    assertThat(EmailAddress.of(ok)).hasSize(255);
    assertRefused("a" + ok, "TOO_LONG");
  }
}
