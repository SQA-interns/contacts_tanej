package app.contactgroups.domain;

/**
 * The name shown for a user (D-20): the name of the ID token; if it is empty, the part of the
 * e-mail before the {@code @}; cut to 100 characters, the limit of the user record.
 */
public final class DisplayName {

  static final int MAX_LENGTH = 100;

  private DisplayName() {}

  public static String of(String tokenName, String email) {
    String name = tokenName == null ? "" : tokenName.strip();
    if (name.isEmpty()) {
      int at = email == null ? -1 : email.indexOf('@');
      name = at > 0 ? email.substring(0, at).strip() : "";
    }
    if (name.isEmpty()) {
      name = "User";
    }
    return name.codePointCount(0, name.length()) > MAX_LENGTH
        ? name.substring(0, name.offsetByCodePoints(0, MAX_LENGTH))
        : name;
  }
}
