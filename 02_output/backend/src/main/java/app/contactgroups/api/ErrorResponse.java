package app.contactgroups.api;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldProblem;
import app.contactgroups.domain.GroupRef;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The one JSON format of every failure (R-017-10, SR-10): status, code, short message and, for a
 * validation failure, one entry per refused field; for a conflict, the current contact (R-017-15).
 * Never holds a stack trace, class name or SQL.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    int status,
    ErrorCode code,
    String message,
    List<FieldProblem> errors,
    Contact current,
    List<GroupRef> groups) {

  public static ErrorResponse of(ErrorCode code, String message) {
    return new ErrorResponse(code.status(), code, message, null, null, null);
  }

  public static ErrorResponse of(ErrorCode code, String message, List<FieldProblem> errors) {
    return new ErrorResponse(
        code.status(), code, message, errors.isEmpty() ? null : errors, null, null);
  }

  public static ErrorResponse of(ApiException e) {
    return new ErrorResponse(
        e.code().status(),
        e.code(),
        e.getMessage(),
        e.fields().isEmpty() ? null : e.fields(),
        e.current(),
        e.groups().isEmpty() ? null : e.groups());
  }
}
