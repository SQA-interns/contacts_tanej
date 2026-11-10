package app.contactgroups.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Turns the conditions of a request into {@link Condition}s, or refuses it (R-010-09). */
public final class Conditions {

  private static final Pattern COUNTRY = Pattern.compile("[A-Z]{2}");

  private Conditions() {}

  /** All conditions, in order; one 400 names every broken field as {@code conditions[i].x}. */
  public static List<Condition> parse(List<ConditionInput> raw) {
    List<Condition> parsed = new ArrayList<>();
    List<FieldProblem> problems = new ArrayList<>();
    List<ConditionInput> inputs = raw == null ? List.of() : raw;
    for (int i = 0; i < inputs.size(); i++) {
      Condition condition = parseOne(inputs.get(i), "conditions[" + i + "]", problems);
      if (condition != null) {
        parsed.add(condition);
      }
    }
    if (!problems.isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "The request is not valid.", problems);
    }
    return parsed;
  }

  private static Condition parseOne(ConditionInput in, String at, List<FieldProblem> problems) {
    if (in == null || in.type() == null) {
      problems.add(new FieldProblem(at + ".type", "REQUIRED", "A condition needs a type."));
      return null;
    }
    int before = problems.size();
    Condition condition =
        switch (in.type()) {
          case "TAGS_ALL" -> new Condition.TagsAll(tags(in, at, problems));
          case "TAGS_ANY" -> new Condition.TagsAny(tags(in, at, problems));
          case "TAG_NOT" -> new Condition.TagNot(text(in.tag(), at + ".tag", problems));
          case "ATTRIBUTE_EQUALS" ->
              new Condition.AttributeEquals(
                  text(in.key(), at + ".key", problems), value(in, at, problems));
          case "ATTRIBUTE_EXISTS" ->
              new Condition.AttributeExists(text(in.key(), at + ".key", problems));
          case "COUNTRY" -> new Condition.Country(country(in.country(), at, problems));
          case "COMPANY" -> new Condition.Company(text(in.company(), at + ".company", problems));
          default -> {
            problems.add(
                new FieldProblem(at + ".type", "INVALID_VALUE", "This condition does not exist."));
            yield null;
          }
        };
    return problems.size() == before ? condition : null;
  }

  private static List<String> tags(ConditionInput in, String at, List<FieldProblem> problems) {
    List<String> tags = new ArrayList<>();
    if (in.tags() == null || in.tags().isEmpty()) {
      problems.add(new FieldProblem(at + ".tags", "REQUIRED", "Name at least one tag."));
      return tags;
    }
    for (String tag : in.tags()) {
      if (tag == null || tag.isBlank()) {
        problems.add(new FieldProblem(at + ".tags", "REQUIRED", "A tag name is empty."));
        return tags;
      }
      tags.add(tag.strip());
    }
    return tags;
  }

  private static String text(String raw, String field, List<FieldProblem> problems) {
    if (raw == null || raw.isBlank()) {
      problems.add(new FieldProblem(field, "REQUIRED", "This field is required."));
      return "";
    }
    return raw.strip();
  }

  private static String value(ConditionInput in, String at, List<FieldProblem> problems) {
    if (in.value() == null) {
      problems.add(new FieldProblem(at + ".value", "REQUIRED", "This field is required."));
      return "";
    }
    return in.value().strip();
  }

  private static String country(String raw, String at, List<FieldProblem> problems) {
    if (raw == null || raw.isBlank()) {
      problems.add(new FieldProblem(at + ".country", "REQUIRED", "Choose a country."));
      return "";
    }
    if (!COUNTRY.matcher(raw).matches()) {
      problems.add(new FieldProblem(at + ".country", "INVALID_FORMAT", "Use a country code."));
    }
    return raw;
  }
}
