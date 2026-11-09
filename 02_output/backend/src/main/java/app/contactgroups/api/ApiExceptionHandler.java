package app.contactgroups.api;

import app.contactgroups.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns every failure into the error response format (SR-10). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorResponse> methodNotAllowed() {
    return respond(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception e) {
    LOG.error("Unexpected failure: {}", e.getClass().getSimpleName());
    return respond(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Something went wrong."));
  }

  private static ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
    return ResponseEntity.status(body.status()).body(body);
  }
}
