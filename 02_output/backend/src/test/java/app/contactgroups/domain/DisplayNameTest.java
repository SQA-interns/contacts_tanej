package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** D-20 (2): the name of a user comes from the token, else from the e-mail, at most 100 chars. */
class DisplayNameTest {

  @Test
  void usesTheNameOfTheToken() {
    assertThat(DisplayName.of("Ana Novak", "ana@example.com")).isEqualTo("Ana Novak");
  }

  @Test
  void fallsBackToThePartOfTheEmailBeforeTheAt() {
    assertThat(DisplayName.of(null, "ana.novak@example.com")).isEqualTo("ana.novak");
    assertThat(DisplayName.of("   ", "ana.novak@example.com")).isEqualTo("ana.novak");
  }

  @Test
  void cutsAnOverLongNameTo100Characters() {
    assertThat(DisplayName.of("x".repeat(150), "a@example.com")).hasSize(100);
    assertThat(DisplayName.of("x".repeat(100), "a@example.com")).hasSize(100);
  }

  @Test
  void cutsAnOverLongEmailPartToo() {
    assertThat(DisplayName.of("", "y".repeat(120) + "@example.com")).hasSize(100);
  }

  @Test
  void neverReturnsAnEmptyName() {
    assertThat(DisplayName.of(null, "@example.com")).isNotBlank();
  }
}
