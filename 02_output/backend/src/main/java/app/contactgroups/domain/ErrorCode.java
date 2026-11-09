package app.contactgroups.domain;

/**
 * Machine-readable code of every failure (error model of the specification, R-017-07, R-017-10).
 * The web app maps each code to a text in the active language (EX-018-02).
 */
public enum ErrorCode {
  UNAUTHENTICATED(401),
  EMAIL_NOT_VERIFIED(403),
  RECENT_LOGIN_REQUIRED(403),
  FORBIDDEN(403),
  NOT_FOUND(404),
  METHOD_NOT_ALLOWED(405),
  CONFLICT(409),
  ALREADY_IN_TRASH(409),
  NOT_IN_TRASH(409),
  CONTACT_IN_TRASH(409),
  GROUP_IN_TRASH(409),
  LAST_OWNER(409),
  SOLE_OWNER(409),
  GROUP_NAME_TAKEN(409),
  ALREADY_MEMBER(409),
  INVITATION_PENDING(409),
  INVITATION_LIMIT(409),
  TAG_NAME_TAKEN(409),
  NOTHING_TO_CHANGE(409),
  PAYLOAD_TOO_LARGE(413),
  RATE_LIMITED(429),
  VALIDATION_FAILED(400),
  INTERNAL_ERROR(500),
  UNAVAILABLE(503);

  private final int status;

  ErrorCode(int status) {
    this.status = status;
  }

  public int status() {
    return status;
  }
}
