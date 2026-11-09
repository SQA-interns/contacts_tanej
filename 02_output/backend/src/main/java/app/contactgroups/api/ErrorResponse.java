package app.contactgroups.api;

import app.contactgroups.domain.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The one JSON format of every failure (R-017-10, SR-10): status, code, short message and, for a
 * validation failure, one entry per refused field. Never holds a stack trace, class name or SQL.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, ErrorCode code, String message, List<FieldError> errors) {

  /** One refused field of a request (R-017-07, EX-005-10). */
  public record FieldError(String field, String code, String message) {}

  public static ErrorResponse of(ErrorCode code, String message) {
    return new ErrorResponse(code.status(), code, message, null);
  }
}
