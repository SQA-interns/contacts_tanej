package app.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** R-017-08, EX-017-04: the limit applies per key and minute; the limit-th request passes. */
class RateLimiterTest {

  /** A clock that the test moves. */
  private static final class Moving extends Clock {
    private Instant now = Instant.parse("2026-11-10T08:00:00Z");

    void advance(Duration d) {
      now = now.plus(d);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  @Test
  void theLimitThRequestPassesAndTheNextIsRefusedWithAWait() {
    RateLimiter limiter = new RateLimiter(3, new Moving());
    assertThat(limiter.secondsToWait("ana")).isZero();
    assertThat(limiter.secondsToWait("ana")).isZero();
    assertThat(limiter.secondsToWait("ana")).isZero();
    assertThat(limiter.secondsToWait("ana")).isBetween(1L, 60L);
  }

  @Test
  void everyKeyHasItsOwnBucket() {
    RateLimiter limiter = new RateLimiter(1, new Moving());
    assertThat(limiter.secondsToWait("ana")).isZero();
    assertThat(limiter.secondsToWait("ana")).isPositive();
    assertThat(limiter.secondsToWait("boris")).isZero();
  }

  @Test
  void theBucketIsFullAgainAfterAMinute() {
    Moving clock = new Moving();
    RateLimiter limiter = new RateLimiter(2, clock);
    limiter.secondsToWait("ana");
    limiter.secondsToWait("ana");
    assertThat(limiter.secondsToWait("ana")).isPositive();
    clock.advance(Duration.ofSeconds(61));
    assertThat(limiter.secondsToWait("ana")).isZero();
    assertThat(limiter.secondsToWait("ana")).isZero();
    assertThat(limiter.secondsToWait("ana")).isPositive();
  }
}
