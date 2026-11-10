package app.contactgroups.domain;

import app.contactgroups.domain.Contact.Attribute;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * The rules of the attributes of one contact (R-008-02 to R-008-04, R-008-06): a key of 1 to 50
 * characters after trimming, a value of at most 255 characters that may be empty and is never
 * interpreted, no key twice ignoring case (BR-07), at most 50 attributes. Every refused attribute
 * is named in one 400.
 */
public final class Attributes {

  public static final int KEY_MAX = 50;
  public static final int VALUE_MAX = 255;
  public static final int MAX = 50;

  private Attributes() {}

  /** The attributes in their stored form, or a 400 naming every refused attribute. */
  public static List<Attribute> validate(List<Attribute> raw) {
    Objects.requireNonNull(raw);
    List<FieldProblem> problems = new ArrayList<>();
    List<Attribute> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < raw.size(); i++) {
      String key = raw.get(i) == null || raw.get(i).key() == null ? "" : raw.get(i).key().strip();
      String value = raw.get(i) == null || raw.get(i).value() == null ? "" : raw.get(i).value();
      String at = "attributes[" + i + "]";
      if (key.isEmpty()) {
        problems.add(new FieldProblem(at + ".key", "REQUIRED", "An attribute needs a key."));
      } else if (length(key) > KEY_MAX) {
        problems.add(
            new FieldProblem(
                at + ".key", "TOO_LONG", "A key has at most " + KEY_MAX + " characters."));
      } else if (!seen.add(key.toLowerCase(Locale.ROOT))) {
        problems.add(
            new FieldProblem(at + ".key", "DUPLICATE_KEY", "The key " + key + " is used twice."));
      }
      if (length(value) > VALUE_MAX) {
        problems.add(
            new FieldProblem(
                at + ".value", "TOO_LONG", "A value has at most " + VALUE_MAX + " characters."));
      }
      out.add(new Attribute(key, value));
    }
    if (raw.size() > MAX) {
      problems.add(
          new FieldProblem(
              "attributes", "TOO_MANY", "A contact has at most " + MAX + " attributes."));
    }
    if (!problems.isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "The request is not valid.", problems);
    }
    return List.copyOf(out);
  }

  private static int length(String text) {
    return text.codePointCount(0, text.length());
  }
}
