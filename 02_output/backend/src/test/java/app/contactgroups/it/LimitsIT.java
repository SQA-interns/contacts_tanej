package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * T-05 limits through the whole stack: the rate limit per user with the configured value (R-017-08,
 * "tests may set it lower") and the body size limit (R-017-09), which is checked before routing.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.rate-limit-per-minute=5", "app.trash-cleanup-cron=0 30 3 * * *"})
class LimitsIT extends AcceptanceBase {

  private static final int MB = 1024 * 1024;

  @Test
  void theSixthRequestOfAUserInAMinuteGets429WithRetryAfterAndOthersAreNotAffected() {
    for (int i = 0; i < 5; i++) {
      get(ANA, "/me").expect(200);
    }
    Res limited = get(ANA, "/me").expectError(429, "RATE_LIMITED");
    int wait = Integer.parseInt(limited.headers().firstValue("Retry-After").orElseThrow());
    assertThat(wait).isBetween(1, 60);
    get(BORIS, "/me").expect(200);
  }

  @Test
  void theUserMayRequestAgainAfterTheMinute() {
    for (int i = 0; i < 6; i++) {
      get(ANA, "/me");
    }
    get(ANA, "/me").expect(429);
    clock.advance(Duration.ofSeconds(61));
    get(ANA, "/me").expect(200);
  }

  @Test
  void aBodyOfOneMegabyteAndOneByteGets413AndExactlyOneMegabyteIsProcessed() {
    request("POST", "/me", "Bearer " + token(ANA), new byte[MB + 1])
        .expectError(413, "PAYLOAD_TOO_LARGE");
    request("POST", "/me", "Bearer " + token(ANA), new byte[MB]).expect(405);
  }

  @Test
  void anUnauthenticatedRequestIsRefusedWith401BeforeItsBodySizeMatters() {
    request("POST", "/me", null, new byte[MB + 1]).expect(401);
  }
}
