package app.contactgroups.api;

import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldProblem;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The one JSON format of every failure (R-017-10, SR-10): status, code, short message and, for a
 * validation failure, one entry per refused field. Never holds a stack trace, class name or SQL.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, ErrorCode code, String message, List<FieldProblem> errors) {

  public static ErrorResponse of(ErrorCode code, String message) {
    return new ErrorResponse(code.status(), code, message, null);
  }

  public static ErrorResponse of(ErrorCode code, String message, List<FieldProblem> errors) {
    return new ErrorResponse(code.status(), code, message, errors.isEmpty() ? null : errors);
  }
}
