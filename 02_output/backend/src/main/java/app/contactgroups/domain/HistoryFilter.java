package app.contactgroups.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * The filters of the group-wide History, combined with AND (R-014-05). The days are days in the
 * project time zone, both ends inclusive (R-014-04).
 */
public record HistoryFilter(
    String actorId, List<HistoryEventType> types, LocalDate from, LocalDate to) {

  private static final ZoneId PROJECT_ZONE = ZoneId.of("Europe/Ljubljana");

  /** Checks the range; 400 {@code INVALID_DATE_RANGE} on the field {@code to} (EX-014-02). */
  public HistoryFilter {
    if (from != null && to != null && to.isBefore(from)) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          "The request is not valid.",
          List.of(
              new FieldProblem(
                  "to", "INVALID_DATE_RANGE", "The last day is before the first day.")));
    }
    types = types == null ? List.of() : List.copyOf(types);
    actorId = actorId == null || actorId.isBlank() ? null : actorId;
  }

  /** The first instant of the first day, or null without a start. */
  public Instant start() {
    return from == null ? null : from.atStartOfDay(PROJECT_ZONE).toInstant();
  }

  /** The first instant after the last day, or null without an end. */
  public Instant endExclusive() {
    return to == null ? null : to.plusDays(1).atStartOfDay(PROJECT_ZONE).toInstant();
  }
}
