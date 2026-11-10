package app.contactgroups.domain;

import java.util.List;

/**
 * The one exception that services and domain rules throw for a refused request. The API turns it
 * into the error response (ADR-06).
 */
public class ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The one text of every 404, so that all its causes look the same (SR-03). */
  public static final String NOT_FOUND_MESSAGE = "Not found.";

  private final ErrorCode code;
  private final transient List<FieldProblem> fields;
  private transient Contact current;
  private final transient List<GroupRef> groups;

  public ApiException(ErrorCode code, String message, List<FieldProblem> fields) {
    this(code, message, fields, List.of());
  }

  private ApiException(
      ErrorCode code, String message, List<FieldProblem> fields, List<GroupRef> groups) {
    super(message);
    this.code = code;
    this.fields = List.copyOf(fields);
    this.groups = List.copyOf(groups);
  }

  public ApiException(ErrorCode code, String message) {
    this(code, message, List.of());
  }

  private ApiException(ErrorCode code, String message, Contact current) {
    this(code, message, List.of());
    this.current = current;
  }

  /** The 409 of a stale save; it carries the contact as it is now (R-005-08, R-017-15). */
  public static ApiException conflict(Contact current) {
    return new ApiException(
        ErrorCode.CONFLICT, "This contact was changed by someone else.", current);
  }

  /** The 409 of an account deletion by the only Owner of these groups (R-016-02). */
  public static ApiException soleOwner(List<GroupRef> groups) {
    return new ApiException(
        ErrorCode.SOLE_OWNER,
        "You are the only Owner of these groups. Hand them to another member or delete them.",
        List.of(),
        groups);
  }

  /** The 404 for a non-member, a missing group, a malformed id and a trashed group (SR-03). */
  public static ApiException notFound() {
    return new ApiException(ErrorCode.NOT_FOUND, NOT_FOUND_MESSAGE);
  }

  public ErrorCode code() {
    return code;
  }

  public List<FieldProblem> fields() {
    return fields;
  }

  /** The groups named by a SOLE_OWNER refusal, else empty. */
  public List<GroupRef> groups() {
    return groups;
  }

  /** The current contact of a CONFLICT, else null. */
  public Contact current() {
    return current;
  }
}
