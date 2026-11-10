package app.contactgroups.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** One row of the Trash view (contract {@code TrashedContactPage}) with the days left (BR-04). */
public record TrashedContact(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String company,
    Instant trashedAt,
    int daysLeft) {

  /** Days in Trash before the erasure. */
  public static final int RETENTION_DAYS = 30;

  /** 30 minus the full days in Trash, never below 0 (R-012-02). */
  public static int daysLeft(Instant trashedAt, Instant now) {
    long full = Duration.between(trashedAt, now).toDays();
    return (int) Math.max(0, RETENTION_DAYS - Math.max(0, full));
  }
}
