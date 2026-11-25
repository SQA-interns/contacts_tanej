package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** SR-11: refusals are logged with method, path template, status and user; secrets never. */
@ExtendWith(OutputCaptureExtension.class)
class RequestLogIT extends AcceptanceBase {

  private static final String SECRET = "s3cr3t-value-7731";

  @Test
  void aRefusalIsLoggedWithoutBodyQueryOrTokenAndASuccessIsNot(CapturedOutput output) {
    String token = token(ANA);
    request(
            "GET",
            "/groups/" + java.util.UUID.randomUUID() + "/nothing?q=" + SECRET,
            "Bearer " + token,
            null)
        .expect(404);
    request("POST", "/me?q=" + SECRET, "Bearer " + token, "{\"password\":\"" + SECRET + "\"}")
        .expect(405);
    request("GET", "/me", "Bearer " + token, null).expect(200);
    request("GET", "/me", "Bearer broken-" + SECRET, null).expect(401);

    // Post-run: the filter logs after the chain (finally), when a refusal is already sent to the
    // client; wait up to 5 s for both lines instead of reading the log at once (flaky on CI).
    String log = output.getAll();
    for (int i = 0; i < 50 && !(log.contains("status=404") && log.contains("status=401")); i++) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        break;
      }
      log = output.getAll();
    }
    assertThat(log).contains("status=404").contains("status=401");
    assertThat(log).contains("method=GET");
    assertThat(log).contains("user=" + ANA.uid());
    assertThat(log).doesNotContain(SECRET).doesNotContain(token);
    assertThat(log).doesNotContain("status=200 ");
  }
}
