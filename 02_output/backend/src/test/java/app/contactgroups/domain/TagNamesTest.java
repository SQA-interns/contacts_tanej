package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** R-006-02 to R-006-04, EX-006-01 to EX-006-06. */
class TagNamesTest {

  private static List<FieldProblem> refused(List<String> tags) {
    Throwable thrown = catchThrowable(() -> TagNames.validate(tags));
    assertThat(thrown).isInstanceOf(ApiException.class);
    return ((ApiException) thrown).fields();
  }

  @Test
  void acceptsLettersDigitsHyphenUnderscoreAndAccents() {
    assertThat(TagNames.validate(List.of("a-b_c9", "čaj2026", "Šola")))
        .containsExactly("a-b_c9", "čaj2026", "Šola");
  }

  @Test
  void acceptsFiftyCharactersAndRefusesFiftyOne() {
    assertThat(TagNames.validate(List.of("a".repeat(50)))).hasSize(1);
    assertThat(refused(List.of("a".repeat(51))))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(tuple("tags[0]", "TOO_LONG"));
  }

  @Test
  void refusesEmptyAndBlankAsTooShort() {
    assertThat(refused(List.of(""))).extracting(FieldProblem::code).containsExactly("TOO_SHORT");
    assertThat(refused(List.of("  "))).extracting(FieldProblem::code).containsExactly("TOO_SHORT");
  }

  @Test
  void refusesOtherCharactersAndNamesTheTag() {
    for (String bad : List.of("a.b", "a b", "a/b")) {
      List<FieldProblem> problems = refused(List.of("ok", bad));
      assertThat(problems).hasSize(1);
      assertThat(problems.get(0).field()).isEqualTo("tags[1]");
      assertThat(problems.get(0).code()).isEqualTo("INVALID_CHARACTERS");
      assertThat(problems.get(0).message()).contains(bad);
    }
  }

  @Test
  void keepsTheFirstSpellingWhenTheSameTagIsTypedTwiceIgnoringCase() {
    assertThat(TagNames.validate(List.of("Vip", "vip", "VIP"))).containsExactly("Vip");
  }

  @Test
  void acceptsFiftyTagsAndRefusesFiftyOne() {
    List<String> tags = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      tags.add("t" + i);
    }
    assertThat(TagNames.validate(tags)).hasSize(50);
    tags.add("t51");
    assertThat(refused(tags))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(tuple("tags", "TOO_MANY"));
  }

  @Test
  void countsDuplicatesOnceTowardsTheLimit() {
    List<String> tags = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      tags.add("t" + i);
    }
    tags.add("T1");
    assertThat(TagNames.validate(tags)).hasSize(50);
  }

  @Test
  void aNullListIsRefusedByTheCallerNotHere() {
    assertThatThrownBy(() -> TagNames.validate(null)).isInstanceOf(NullPointerException.class);
  }
}
