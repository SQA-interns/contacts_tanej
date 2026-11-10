package app.contactgroups.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * One token bucket per key (user id, or remote address when there is no user) that holds the limit
 * and is filled again one minute after it was started (R-017-08, SR-09). Time comes from the
 * injected clock (AR-11).
 */
class RateLimiter {

  private static final int PURGE_ABOVE = 10_000;

  private final int limit;
  private final TimeMeter meter;
  private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  RateLimiter(int limit, Clock clock) {
    this.limit = limit;
    this.meter =
        new TimeMeter() {
          @Override
          public long currentTimeNanos() {
            return TimeUnit.MILLISECONDS.toNanos(clock.millis());
          }

          @Override
          public boolean isWallClockBased() {
            return true;
          }
        };
  }

  /** Takes one request of the key: 0 when it may pass, else the seconds to wait (at least 1). */
  long secondsToWait(String key) {
    if (buckets.size() > PURGE_ABOVE) {
      buckets.values().removeIf(b -> b.getAvailableTokens() >= limit);
    }
    var probe = buckets.computeIfAbsent(key, k -> newBucket()).tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return 0;
    }
    return Math.max(
        1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() + 999_999_999));
  }

  private Bucket newBucket() {
    return Bucket.builder()
        .withCustomTimePrecision(meter)
        .addLimit(
            Bandwidth.builder()
                .capacity(limit)
                .refillIntervally(limit, Duration.ofMinutes(1))
                .build())
        .build();
  }
}
