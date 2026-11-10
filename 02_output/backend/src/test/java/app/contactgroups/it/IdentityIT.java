package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.IVAN;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import app.contactgroups.acceptance.Who;
import org.junit.jupiter.api.Test;

/**
 * T-03 identity: the user record of every request (ADR-02, D-20), {@code GET /me}, the verified
 * e-mail check (R-017-03, BR-03) and the refusal of a deleted account (R-016-06).
 */
class IdentityIT extends AcceptanceBase {

  private int users() {
    return jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);
  }

  @Test
  void meReturnsTheUserAndCreatesTheRecord() {
    Res r = get(ANA, "/me").expect(200);
    assertThat(r.at("/id").asText()).isEqualTo(ANA.uid());
    assertThat(r.at("/name").asText()).isEqualTo("Ana Novak");
    assertThat(r.at("/email").asText()).isEqualTo("ana.novak@example.com");
    assertThat(users()).isEqualTo(1);
  }

  @Test
  void theEmailIsStoredInLowercaseAndTheRecordFollowsTheToken() {
    get(ANA.withEmail("Ana.Novak@Example.com"), "/me").expect(200);
    assertThat(jdbc.queryForObject("SELECT email FROM users", String.class))
        .isEqualTo("ana.novak@example.com");
    get(ANA.withName("Ana Horvat"), "/me").expect(200);
    assertThat(jdbc.queryForObject("SELECT name FROM users", String.class)).isEqualTo("Ana Horvat");
    assertThat(users()).isEqualTo(1);
  }

  @Test
  void aTokenWithoutANameUsesTheEmailPart() {
    Res r = get(ANA.withName(""), "/me").expect(200);
    assertThat(r.at("/name").asText()).isEqualTo("ana.novak");
  }

  @Test
  void anOverLongNameIsCutTo100Characters() {
    Res r = get(ANA.withName("x".repeat(150)), "/me").expect(200);
    assertThat(r.at("/name").asText()).hasSize(100);
  }

  @Test
  void anUnverifiedEmailGets403AndNoRecordIsCreated() {
    get(IVAN, "/me").expectError(403, "EMAIL_NOT_VERIFIED");
    assertThat(users()).isZero();
  }

  @Test
  void aDeletedAccountGets401WithItsOldToken() {
    get(ANA, "/me").expect(200);
    jdbc.update("INSERT INTO deleted_accounts (user_id, deleted_at) VALUES (?, now())", ANA.uid());
    jdbc.update("DELETE FROM users WHERE id = ?", ANA.uid());
    get(ANA, "/me").expectError(401, "UNAUTHENTICATED");
    assertThat(users()).isZero();
    Who other = Who.person("p1");
    get(other, "/me").expect(200);
  }
}
