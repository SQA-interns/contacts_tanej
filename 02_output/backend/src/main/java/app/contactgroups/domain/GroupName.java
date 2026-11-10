package app.contactgroups.domain;

import java.util.List;

/** The group name rule (R-002-02): trimmed, 1 to 100 characters. */
public final class GroupName {

  public static final int MAX_LENGTH = 100;

  private GroupName() {}

  /**
   * The trimmed name, or a 400 naming the field {@code name} with {@code REQUIRED}/{@code
   * TOO_LONG}.
   */
  public static String of(String raw) {
    String name = raw == null ? "" : raw.replaceAll("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$", "");
    if (name.isEmpty()) {
      throw refused("REQUIRED", "The name is required.");
    }
    if (name.codePointCount(0, name.length()) > MAX_LENGTH) {
      throw refused("TOO_LONG", "The name is longer than " + MAX_LENGTH + " characters.");
    }
    return name;
  }

  private static ApiException refused(String code, String message) {
    return new ApiException(
        ErrorCode.VALIDATION_FAILED,
        "The request is not valid.",
        List.of(new FieldProblem("name", code, message)));
  }
}
