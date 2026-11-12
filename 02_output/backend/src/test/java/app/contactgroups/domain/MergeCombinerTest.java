package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.contactgroups.domain.Contact.Attribute;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The combining rules of a merge: R-022-03, R-022-04, R-022-09, EX-022-02. */
class MergeCombinerTest {

  @Test
  void tagsAreCombinedOnceIgnoringCaseWithTheFirstSpelling() {
    List<String> tags = MergeCombiner.tags(List.of(List.of("a", "Shared"), List.of("shared", "b")));
    assertThat(tags).containsExactly("a", "Shared", "b");
  }

  @Test
  void moreThanFiftyCombinedTagsAreRefusedNamingTheLimit() {
    List<String> first = new ArrayList<>();
    List<String> second = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      first.add("a" + i);
      second.add("b" + i);
    }
    assertThatThrownBy(() -> MergeCombiner.tags(List.of(first, second)))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.fields()).extracting(FieldProblem::code).contains("TOO_MANY"));
  }

  @Test
  void aKeyWithTheSameValueOnBothIsOneAttributeAndNeedsNoChoice() {
    List<Attribute> merged =
        MergeCombiner.attributes(
            List.of(
                List.of(new Attribute("kompetenca", "java")),
                List.of(new Attribute("Kompetenca", "java"))),
            List.of());
    assertThat(merged).containsExactly(new Attribute("kompetenca", "java"));
  }

  @Test
  void aKeyWithDifferentValuesNeedsAChoice() {
    var lists =
        List.of(
            List.of(new Attribute("kompetenca", "java")),
            List.of(new Attribute("Kompetenca", "python")));
    assertThatThrownBy(() -> MergeCombiner.attributes(lists, List.of()))
        .isInstanceOfSatisfying(
            ApiException.class,
            e ->
                assertThat(e.fields())
                    .extracting(FieldProblem::code)
                    .containsExactly("ATTRIBUTE_CHOICE_REQUIRED"));
  }

  @Test
  void theChosenValueWinsAndKeepsTheFirstSpellingOfTheKey() {
    var lists =
        List.of(
            List.of(new Attribute("kompetenca", "java")),
            List.of(new Attribute("Kompetenca", "python")));
    List<Attribute> merged =
        MergeCombiner.attributes(lists, List.of(new Attribute("KOMPETENCA", "python")));
    assertThat(merged).containsExactly(new Attribute("kompetenca", "python"));
  }

  @Test
  void moreThanFiftyCombinedAttributesAreRefusedNamingTheLimit() {
    List<Attribute> first = new ArrayList<>();
    List<Attribute> second = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      first.add(new Attribute("a" + i, "x"));
      second.add(new Attribute("b" + i, "x"));
    }
    assertThatThrownBy(() -> MergeCombiner.attributes(List.of(first, second), List.of()))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> assertThat(e.fields()).extracting(FieldProblem::code).contains("TOO_MANY"));
  }
}
