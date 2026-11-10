package app.contactgroups.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The rules of the tag names of one contact (R-006-02 to R-006-04): 1 to 50 letters, digits, "-" or
 * "_"; the same name typed twice (ignoring case, BR-07) is one tag with the first spelling; at most
 * 50 tags. Every refused tag is named in one 400.
 */
public final class TagNames {

  public static final int NAME_MAX = 50;
  public static final int TAGS_MAX = 50;
  private static final Pattern CHARACTERS = Pattern.compile("[\\p{L}\\p{Nd}_-]*");

  private TagNames() {}

  /** The tags in their stored form, without repeats, or a 400 naming every refused tag. */
  public static List<String> validate(List<String> raw) {
    Objects.requireNonNull(raw);
    List<FieldProblem> problems = new ArrayList<>();
    Map<String, String> byKey = new LinkedHashMap<>();
    for (int i = 0; i < raw.size(); i++) {
      String name = raw.get(i) == null ? "" : raw.get(i).strip();
      if (check(problems, "tags[" + i + "]", name)) {
        byKey.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
      }
    }
    if (byKey.size() > TAGS_MAX) {
      problems.add(
          new FieldProblem("tags", "TOO_MANY", "A contact has at most " + TAGS_MAX + " tags."));
    }
    if (!problems.isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "The request is not valid.", problems);
    }
    return List.copyOf(byKey.values());
  }

  /** The new name of a tag (R-007-04): the same rules as one tag of a contact, field "name". */
  public static String validateName(String raw) {
    String name = raw == null ? "" : raw.strip();
    List<FieldProblem> problems = new ArrayList<>();
    if (!check(problems, "name", name)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "The request is not valid.", problems);
    }
    return name;
  }

  private static boolean check(List<FieldProblem> problems, String field, String name) {
    String code = null;
    String message = null;
    if (name.isEmpty()) {
      code = "TOO_SHORT";
      message = "A tag has at least 1 character.";
    } else if (name.codePointCount(0, name.length()) > NAME_MAX) {
      code = "TOO_LONG";
      message = "A tag has at most " + NAME_MAX + " characters.";
    } else if (!CHARACTERS.matcher(name).matches()) {
      code = "INVALID_CHARACTERS";
      message = "The tag " + name + " may have letters, digits, - and _ only.";
    }
    if (code != null) {
      problems.add(new FieldProblem(field, code, message));
    }
    return code == null;
  }
}
