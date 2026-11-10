package app.contactgroups.domain;

import java.util.List;

/** The role of a member in one group (glossary: Owner, Editor, Viewer). */
public enum Role {
  OWNER,
  EDITOR,
  VIEWER;

  /** The role named in a request body; 400 {@code INVALID_VALUE} on the field {@code role}. */
  public static Role parse(String raw) {
    for (Role role : values()) {
      if (role.name().equals(raw)) {
        return role;
      }
    }
    throw new ApiException(
        ErrorCode.VALIDATION_FAILED,
        "The request is not valid.",
        List.of(new FieldProblem("role", "INVALID_VALUE", "The role is Owner, Editor or Viewer.")));
  }
}
