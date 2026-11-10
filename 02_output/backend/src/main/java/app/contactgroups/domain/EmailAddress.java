package app.contactgroups.domain;

import java.util.List;
import java.util.Locale;

/**
 * The e-mail rule (BR-05, specification 3.1): one {@code @}, a non-empty local part, a domain with
 * at least one dot and no empty label, no spaces, at most 255 characters. The stored form is
 * lowercase (BR-07).
 */
public final class EmailAddress {

  public static final int MAX_LENGTH = 255;

  private EmailAddress() {}

  /** The trimmed lowercase address, or a 400 naming the field {@code email}. */
  public static String of(String raw) {
    String email = raw == null ? "" : raw.strip();
    if (email.codePointCount(0, email.length()) > MAX_LENGTH) {
      throw refused("TOO_LONG", "The e-mail address is longer than " + MAX_LENGTH + " characters.");
    }
    if (!isValid(email)) {
      throw refused("INVALID_FORMAT", "The e-mail address is not valid.");
    }
    return email.toLowerCase(Locale.ROOT);
  }

  private static boolean isValid(String email) {
    int at = email.indexOf('@');
    if (at < 1 || at != email.lastIndexOf('@') || email.chars().anyMatch(Character::isWhitespace)) {
      return false;
    }
    String domain = email.substring(at + 1);
    if (domain.indexOf('.') < 0) {
      return false;
    }
    for (String label : domain.split("\\.", -1)) {
      if (label.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static ApiException refused(String code, String message) {
    return new ApiException(
        ErrorCode.VALIDATION_FAILED,
        "The request is not valid.",
        List.of(new FieldProblem("email", code, message)));
  }
}
