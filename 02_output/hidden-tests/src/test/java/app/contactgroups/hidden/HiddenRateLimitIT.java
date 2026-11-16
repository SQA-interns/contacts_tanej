package app.contactgroups.hidden;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Hidden case for R-017-08 with the configured limit of 5 requests per user and minute. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.rate-limit-per-minute=5", "app.trash-cleanup-cron=0 30 3 * * *"})
class HiddenRateLimitIT extends AcceptanceBase {

  @Test
  @DisplayName(
      "R-017-08 boundary the configured limit is per user and the Retry-After is within the minute")
  void R_017_08_boundary_configuredLimitPerUser() {
    for (int i = 0; i < 5; i++) {
      get(ANA, "/groups").expect(200);
    }
    Res limited = get(ANA, "/groups").expectError(429, "RATE_LIMITED");
    int wait = Integer.parseInt(limited.headers().firstValue("Retry-After").orElse("0"));
    assertThat(wait).isBetween(1, 60);
    get(BORIS, "/groups").expect(200);
    get(null, "/health").expect(200);
  }
}
