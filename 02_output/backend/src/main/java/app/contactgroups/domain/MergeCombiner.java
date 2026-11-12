package app.contactgroups.domain;

import app.contactgroups.domain.Contact.Attribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What a merge combines by itself (R-022-03, R-022-04): the tags and the attributes of all
 * contacts, the oldest contact first. The result is checked with the rules of a save, so a limit
 * that the result breaks is named in a 400 (R-022-09).
 */
public final class MergeCombiner {

  private MergeCombiner() {}

  /** Every tag once ignoring case, in the spelling met first. */
  public static List<String> tags(List<List<String>> perContact) {
    return TagNames.validate(perContact.stream().flatMap(List::stream).toList());
  }

  /**
   * Every key once ignoring case. A key with different values needs its value in {@code choices};
   * without it the merge is refused with {@code ATTRIBUTE_CHOICE_REQUIRED}.
   */
  public static List<Attribute> attributes(
      List<List<Attribute>> perContact, List<Attribute> choices) {
    Map<String, Attribute> chosen = new LinkedHashMap<>();
    choices.forEach(c -> chosen.putIfAbsent(key(c), c));
    Map<String, Attribute> first = new LinkedHashMap<>();
    Map<String, Set<String>> values = new LinkedHashMap<>();
    perContact.stream()
        .flatMap(List::stream)
        .forEach(
            a -> {
              first.putIfAbsent(key(a), a);
              values.computeIfAbsent(key(a), k -> new LinkedHashSet<>()).add(a.value());
            });
    List<FieldProblem> missing = new ArrayList<>();
    List<Attribute> merged = new ArrayList<>();
    first.forEach(
        (k, attribute) -> {
          if (values.get(k).size() == 1) {
            merged.add(attribute);
          } else if (chosen.containsKey(k)) {
            merged.add(new Attribute(attribute.key(), chosen.get(k).value()));
          } else {
            missing.add(
                new FieldProblem(
                    "attributeChoices",
                    "ATTRIBUTE_CHOICE_REQUIRED",
                    "Choose a value for the key " + attribute.key() + "."));
          }
        });
    if (!missing.isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "The request is not valid.", missing);
    }
    return Attributes.validate(merged);
  }

  private static String key(Attribute attribute) {
    return attribute.key().strip().toLowerCase(Locale.ROOT);
  }
}
