package app.contactgroups.api;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldProblem;
import app.contactgroups.service.AccessService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every failure into the error response format (SR-10, ADR-06). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  static ResponseEntity<ErrorResponse> refused(ApiException e) {
    return respond(ErrorResponse.of(e.code(), e.getMessage(), e.fields()));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorResponse> methodNotAllowed() {
    return respond(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed."));
  }

  /** A path that leads nowhere is the same 404 as a missing group (SR-03). */
  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  ResponseEntity<ErrorResponse> noHandler() {
    return notFound();
  }

  /** A malformed id in the path is the same 404 as an unknown one (EX-017-02). */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e) {
    if (e.getParameter().hasParameterAnnotation(PathVariable.class)) {
      return notFound();
    }
    return invalid(List.of(new FieldProblem(e.getName(), "INVALID_FORMAT", "Invalid value.")));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> notValid(MethodArgumentNotValidException e) {
    return invalid(
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> new FieldProblem(f.getField(), fieldCode(f.getCode()), "Invalid value."))
            .toList());
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    HttpMediaTypeNotSupportedException.class
  })
  ResponseEntity<ErrorResponse> unreadable() {
    return invalid(List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception e) {
    LOG.error("Unexpected failure: {}", e.getClass().getSimpleName());
    return respond(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Something went wrong."));
  }

  private static String fieldCode(String constraint) {
    return switch (constraint == null ? "" : constraint) {
      case "NotBlank", "NotNull", "NotEmpty" -> "REQUIRED";
      case "Size", "Max" -> "TOO_LONG";
      default -> "INVALID_FORMAT";
    };
  }

  private static ResponseEntity<ErrorResponse> notFound() {
    return refused(AccessService.notFound());
  }

  private static ResponseEntity<ErrorResponse> invalid(List<FieldProblem> fields) {
    return respond(
        ErrorResponse.of(ErrorCode.VALIDATION_FAILED, "The request is not valid.", fields));
  }

  private static ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
    return ResponseEntity.status(body.status()).body(body);
  }
}
