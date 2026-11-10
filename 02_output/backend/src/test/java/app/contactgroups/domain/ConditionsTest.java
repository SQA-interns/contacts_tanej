package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The seven condition types and their validation (R-010-01 to R-010-09). */
class ConditionsTest {

  private static ConditionInput input(String type) {
    return new ConditionInput(type, null, null, null, null, null, null);
  }

  private static List<FieldProblem> problems(List<ConditionInput> raw) {
    return ((ApiException) catchThrowable(raw)).fields();
  }

  private static Throwable catchThrowable(List<ConditionInput> raw) {
    return org.assertj.core.api.Assertions.catchThrowable(() -> Conditions.parse(raw));
  }

  @Test
  void noConditionsIsAnEmptyList() {
    assertThat(Conditions.parse(null)).isEmpty();
    assertThat(Conditions.parse(List.of())).isEmpty();
  }

  @Test
  void parsesEachOfTheSevenTypes() {
    var parsed =
        Conditions.parse(
            List.of(
                new ConditionInput("TAGS_ALL", List.of(" a "), null, null, null, null, null),
                new ConditionInput("TAGS_ANY", List.of("a", "b"), null, null, null, null, null),
                new ConditionInput("TAG_NOT", null, "x", null, null, null, null),
                new ConditionInput("ATTRIBUTE_EQUALS", null, null, "k", "v", null, null),
                new ConditionInput("ATTRIBUTE_EXISTS", null, null, "k", null, null, null),
                new ConditionInput("COUNTRY", null, null, null, null, "SI", null),
                new ConditionInput("COMPANY", null, null, null, null, null, "Acme")));
    assertThat(parsed)
        .containsExactly(
            new Condition.TagsAll(List.of("a")),
            new Condition.TagsAny(List.of("a", "b")),
            new Condition.TagNot("x"),
            new Condition.AttributeEquals("k", "v"),
            new Condition.AttributeExists("k"),
            new Condition.Country("SI"),
            new Condition.Company("Acme"));
  }

  @Test
  void anUnknownTypeIsRefusedOnThatCondition() {
    assertThat(problems(List.of(input("SAVED_FILTER"))))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("conditions[0].type", "INVALID_VALUE"));
    assertThat(problems(List.of(input(null))))
        .extracting(FieldProblem::code)
        .containsExactly("REQUIRED");
  }

  @Test
  void missingOperandsAreRefused() {
    var raw =
        List.of(
            input("TAGS_ALL"),
            input("TAG_NOT"),
            input("ATTRIBUTE_EQUALS"),
            input("ATTRIBUTE_EXISTS"),
            input("COUNTRY"),
            input("COMPANY"));
    assertThat(problems(raw))
        .extracting(FieldProblem::field)
        .containsExactly(
            "conditions[0].tags",
            "conditions[1].tag",
            "conditions[2].key",
            "conditions[2].value",
            "conditions[3].key",
            "conditions[4].country",
            "conditions[5].company");
  }

  @Test
  void aCountryMustBeTwoCapitalLetters() {
    var raw = List.of(new ConditionInput("COUNTRY", null, null, null, null, "si", null));
    assertThat(problems(raw)).extracting(FieldProblem::code).containsExactly("INVALID_FORMAT");
  }

  @Test
  void aBlankTagInTheListIsRefused() {
    var raw =
        List.of(new ConditionInput("TAGS_ANY", List.of("a", " "), null, null, null, null, null));
    assertThat(problems(raw)).extracting(FieldProblem::field).containsExactly("conditions[0].tags");
  }

  @Test
  void anEmptyValueIsAllowedForAKeyThatEqualsNothing() {
    var raw = List.of(new ConditionInput("ATTRIBUTE_EQUALS", null, null, "k", "", null, null));
    assertThat(Conditions.parse(raw)).containsExactly(new Condition.AttributeEquals("k", ""));
  }

  @Test
  void theFailureIsAValidationFailure() {
    assertThatThrownBy(() -> Conditions.parse(List.of(input("NOTES"))))
        .isInstanceOfSatisfying(
            ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
  }
}
