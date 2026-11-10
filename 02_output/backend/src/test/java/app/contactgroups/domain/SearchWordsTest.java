package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchWordsTest {

  @Test
  @DisplayName("EX-009-02 EX-009-04: missing, empty or blank text gives no word")
  void blankTextGivesNoWord() {
    assertThat(SearchWords.patterns(null)).isEmpty();
    assertThat(SearchWords.patterns("")).isEmpty();
    assertThat(SearchWords.patterns("   ")).isEmpty();
  }

  @Test
  @DisplayName("R-009-03: every word of the text is its own pattern")
  void wordsAreSplitAtSpaces() {
    assertThat(SearchWords.patterns("  ana   no ")).containsExactly("%ana%", "%no%");
  }

  @Test
  @DisplayName("EX-009-03: percent, underscore and backslash match as themselves")
  void wildcardsAreEscaped() {
    assertThat(SearchWords.patterns("%")).containsExactly("%\\%%");
    assertThat(SearchWords.patterns("a_b")).containsExactly("%a\\_b%");
    assertThat(SearchWords.patterns("\\")).containsExactly("%\\\\%");
  }
}
