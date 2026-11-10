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

  public ApiException(ErrorCode code, String message, List<FieldProblem> fields) {
    super(message);
    this.code = code;
    this.fields = List.copyOf(fields);
  }

  public ApiException(ErrorCode code, String message) {
    this(code, message, List.of());
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
}
