package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/** SR-05 and SR-06: security headers on every answer, CORS only for the one allowed origin. */
class SecurityHeadersIT extends AcceptanceBase {

  private HttpResponse<String> send(String path, String origin, String token) throws Exception {
    HttpRequest.Builder b =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
    if (origin != null) {
      b.header("Origin", origin);
    }
    if (token != null) {
      b.header("Authorization", "Bearer " + token);
    }
    return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void everyAnswerCarriesTheSecurityHeaders() {
    for (Res r : new Res[] {get(null, "/health"), get(null, "/me"), get(ANA, "/me")}) {
      var h = r.headers();
      assertThat(h.firstValue("X-Content-Type-Options")).hasValue("nosniff");
      assertThat(h.firstValue("X-Frame-Options")).hasValue("DENY");
      assertThat(h.firstValue("Referrer-Policy")).hasValue("no-referrer");
      assertThat(h.firstValue("Content-Security-Policy").orElse(""))
          .contains("default-src 'none'")
          .contains("frame-ancestors 'none'");
      assertThat(h.firstValue("Permissions-Policy").orElse("")).contains("camera=()");
    }
  }

  @Test
  void onlyTheAllowedOriginGetsCorsHeaders() throws Exception {
    var allowed = send("/health", "http://localhost:5173", null);
    assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin"))
        .hasValue("http://localhost:5173");
    var other = send("/health", "https://evil.example", null);
    assertThat(other.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
  }

  /** D-56 (c), CVE-2026-41707: DPoP is not active, so a DPoP-scheme credential is refused. */
  @Test
  void aDpopAuthorizationHeaderIsRefused() throws Exception {
    var r =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/me"))
                    .header("Authorization", "DPoP " + token(ANA))
                    .header("DPoP", "eyJhbGciOiJFUzI1NiJ9.e30.c2ln")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(r.statusCode()).isEqualTo(401);
  }

  @Test
  void theErrorAnswersAreJsonInUtf8() throws Exception {
    var r = send("/me", null, null);
    assertThat(r.statusCode()).isEqualTo(401);
    assertThat(r.headers().firstValue("Content-Type").orElse("")).contains("application/json");
    assertThat(r.headers().firstValue("Content-Type").orElse("").toLowerCase()).contains("utf-8");
  }
}
