package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** R-015-04: the days left of a group in Trash follow BR-04. */
class DeletedGroupTest {

  private static final Instant TRASHED = Instant.parse("2026-11-01T10:00:00Z");

  private static int daysLeftAfter(Duration inTrash) {
    return DeletedGroup.of(UUID.randomUUID(), "G", TRASHED, TRASHED.plus(inTrash)).daysLeft();
  }

  @Test
  void aGroupJustDeletedHasThirtyDaysLeft() {
    assertThat(daysLeftAfter(Duration.ZERO)).isEqualTo(30);
  }

  @Test
  void after29DaysAnd23HoursOneDayIsLeft() {
    assertThat(daysLeftAfter(Duration.ofDays(29).plusHours(23))).isEqualTo(1);
  }

  @Test
  void afterThirtyDaysNoDayIsLeft() {
    assertThat(daysLeftAfter(Duration.ofDays(31))).isZero();
  }
}
