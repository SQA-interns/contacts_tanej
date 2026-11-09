# ADR-06: Error model, validation and request limits

- Status: accepted · Phase 2 · Drivers: R-017-02, R-017-03, R-017-05..10, R-017-15, SR-03, SR-09, SR-10, SR-11, EX-017-07, EX-018-02, F-16

## Context

Every failure uses one JSON format with a code and a short message and no internals (SR-10). The status codes are fixed by the stories (401, 403 `EMAIL_NOT_VERIFIED`, 404 identical for non-member and missing, 400 with the rule code and field, 409 `CONFLICT`, 413, 429 with `Retry-After`, 405). Server errors shown to users are translated by the web app (EX-018-02).

## Options

1. Spring's default `ProblemDetail` (RFC 9457). Standard but has no `code`/field list the stories require.
2. Own `ErrorResponse {status, code, message, errors[], groups?, current?}`, built in one `@RestControllerAdvice`, `ErrorCode` enum with the HTTP status, and a `FieldError {field, code, message}`.

## Decision

Option 2 (`docs/02_contracts/schemas.yaml`, `ErrorResponse`).

- **One exception type** `ApiException(ErrorCode, message, fieldErrors…)` is thrown by services and domain rules; `ApiExceptionHandler` maps it, Bean Validation and JSON parse failures (400 `VALIDATION_FAILED`), `HttpRequestMethodNotSupportedException` (405), and any other exception (500 `INTERNAL_ERROR`, a fixed message, logged by class name only) to the format. The security entry points (401, 403) use the same writer (`SecurityConfig`).
- **404 sameness (SR-03).** `NOT_FOUND` has one fixed message and no `errors`; the same builder serves the non-member, missing group, malformed id and group in Trash cases, so bodies and headers are equal. A test compares the raw responses.
- **Validation is the domain's, not the controller's.** The rules of BR-05, US-006, US-008 are plain Java in `domain` (`ContactRules`, `TagName`, `EmailAddress`, `CountryCodes`) returning every broken field at once (EX-005-10), so one save names all failed fields. Controllers only check types. Field codes: `REQUIRED_ONE_OF`, `TOO_LONG`, `INVALID_FORMAT`, `INVALID_CHARACTERS`, `UNKNOWN_COUNTRY`, `TOO_MANY`, `DUPLICATE_KEY`, … (contract `FieldErrorCode`). The field name is the API's, for example `email`, `tags[2]`, `attributes[0].key`.
- **E-mail format (D-20).** One `@`, a non-empty local part, a domain with at least one dot and no empty label, no spaces, at most 255 characters; lowercase stored. The same rule serves contacts and invitations.
- **Request limits.** `BodySizeLimitFilter` answers 413 `PAYLOAD_TOO_LARGE` for `Content-Length` over 1 MB and wraps the stream so chunked bodies are cut at 1 MB (EX-017-05: exactly 1 MB passes). `RateLimitFilter` (Bucket4j, one bucket per user id, refilled each minute) answers 429 `RATE_LIMITED` with `Retry-After` (R-017-08, EX-017-04: the 100th passes, the 101st is refused). Both run after authentication so the key is the user, and before controllers.
- **Logging (SR-11).** A `RequestLogFilter` logs method, path template (never query or body), status and user id for 401, 403, 404, 429 and failed token checks. No body, header or token is logged; a test puts a known secret value in a request and finds it absent from the log output.
- **Language.** The server's `message` is short English and for developers; the web app shows `error.<code>` from its language files (NFR-10).
- F-16 (rules that join statements): each clause of such a rule has its own test and its own row in the traceability table.

## Consequences

- Controllers stay thin; all rules are unit-testable without Spring.
- Adding an error code needs the enum, the contract, both language files and a test (the locale test fails if a code has no text).
