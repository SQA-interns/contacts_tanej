package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** R-002-02 and EX-002-01 to EX-002-03: the group name rule. */
class GroupNameTest {

  private static void assertRefused(String raw, String code) {
    assertThatThrownBy(() -> GroupName.of(raw))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
              assertThat(e.fields()).extracting(FieldProblem::field).containsExactly("name");
              assertThat(e.fields()).extracting(FieldProblem::code).containsExactly(code);
            });
  }

  @Test
  void aMissingNameIsRequired() {
    assertRefused(null, "REQUIRED");
  }

  @Test
  void anEmptyNameAndANameOfOnlySpacesAreRequired() {
    assertRefused("", "REQUIRED");
    assertRefused("   ", "REQUIRED");
    assertRefused("\t  ", "REQUIRED");
  }

  @Test
  void spacesAroundTheNameAreRemoved() {
    assertThat(GroupName.of("  Konferenca 2026 ")).isEqualTo("Konferenca 2026");
  }

  @Test
  void aNameOfExactly100CharactersIsAccepted() {
    assertThat(GroupName.of("a".repeat(100))).hasSize(100);
  }

  @Test
  void aNameOf101CharactersIsTooLong() {
    assertRefused("a".repeat(101), "TOO_LONG");
  }

  @Test
  void theLimitCountsCharactersAndNotCodeUnits() {
    String emoji = "😀".repeat(100);
    assertThat(GroupName.of(emoji)).isEqualTo(emoji);
    assertRefused(emoji + "a", "TOO_LONG");
  }

  @Test
  void spacesDoNotCountTowardsTheLimit() {
    assertThat(GroupName.of("  " + "a".repeat(100) + "  ")).hasSize(100);
  }

  @Test
  void accentsAndSymbolsAreKeptUnchanged() {
    assertThat(GroupName.of("Čebelarji & Co")).isEqualTo("Čebelarji & Co");
  }
}
