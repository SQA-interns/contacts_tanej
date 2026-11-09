package app.contactgroups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The skeleton starts on an empty database (NFR-13), answers the health check without a token
 * (R-017-12, EX-017-09), refuses other requests without a token (R-017-01) and sets the security
 * headers (SR-06). The migration gives the text rules the stories need (AR-15, PP-03).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SkeletonIT {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.firebase-project-id", () -> "demo-contacts");
    registry.add("app.firebase-auth-emulator-host", () -> "localhost:9099");
    registry.add("app.allowed-origin", () -> "http://localhost:5173");
  }

  @Autowired TestRestTemplate http;
  @Autowired JdbcTemplate jdbc;

  @Test
  void healthCheckNeedsNoToken() {
    ResponseEntity<String> response = http.getForEntity("/health", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("UP");
  }

  @Test
  void otherRequestsNeedAToken() {
    ResponseEntity<String> response = http.getForEntity("/groups", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody())
        .contains("\"code\":\"UNAUTHENTICATED\"")
        .doesNotContain("at app.");
  }

  @Test
  void securityHeadersAreSet() {
    var headers = http.getForEntity("/health", String.class).getHeaders();
    assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(headers.getFirst("Content-Security-Policy")).contains("frame-ancestors 'none'");
    assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("no-referrer");
  }

  @Test
  void foldingIgnoresCaseAndAccents() {
    assertThat(jdbc.queryForObject("SELECT f_fold('ŠKERLJ Čuk')", String.class))
        .isEqualTo("skerlj cuk");
  }

  @Test
  void slovenianCollationPutsCSZAfterCSZ() {
    var order =
        jdbc.queryForList(
            "SELECT n FROM (VALUES ('Zupan'), ('Šuster'), ('Sever'), ('Čuk'), ('Cerar')) t(n)"
                + " ORDER BY n COLLATE \"sl-SI-x-icu\"",
            String.class);
    assertThat(order).containsExactly("Cerar", "Čuk", "Sever", "Šuster", "Zupan");
  }

  @Test
  void historyEntriesAreAppendOnly() {
    UUID group = UUID.randomUUID();
    UUID entry = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO groups (id, name, state, created_at, created_by)"
            + " VALUES (?, 'G', 'ACTIVE', now(), 'u')",
        group);
    jdbc.update(
        "INSERT INTO history_entries (id, group_id, change_set_id, event_type, actor_name,"
            + " actor_email, created_at) VALUES (?, ?, ?, 'CONTACT_CREATED', 'Ana', 'a@x.si', now())",
        entry,
        group,
        UUID.randomUUID());
    assertThatThrownBy(
            () -> jdbc.update("UPDATE history_entries SET actor_name = 'Eve' WHERE id = ?", entry))
        .hasMessageContaining("append-only");
    // the anonymisation of an account deletion is allowed (R-016-03)
    assertThat(jdbc.update("UPDATE history_entries SET actor_email = NULL WHERE id = ?", entry))
        .isEqualTo(1);
  }
}
