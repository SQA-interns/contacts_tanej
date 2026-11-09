package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-016 Delete my account: AC-016-01 to AC-016-04 and AC-016-06, EX-016-01 to EX-016-05. */
class Us016AccountAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
  }

  private boolean listsMember(Who viewer, Who member) {
    for (JsonNode m : get(viewer, "/groups/" + gid + "/members").expect(200).json()) {
      if (member.uid().equals(m.path("userId").asText())) {
        return true;
      }
    }
    return false;
  }

  @Test
  void AC_016_01_deletingAnAccountRemovesTheMemberKeepsTheNameInHistoryAndTheContacts() {
    JsonNode c =
        newContact(
            ANA, gid, j("firstName", "Ana", "lastName", "Novak", "phone", "+386 40 123 456"));
    save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText()), "phone", "+386 41 555 123")
        .expect(200);
    delete(MARKO, "/me").expect(204);
    assertThat(listsMember(ANA, MARKO)).isFalse();
    JsonNode entries = contactHistory(ANA, gid, c.path("id").asText()).expect(200).json();
    assertThat(entries.toString()).contains("Marko Pirc").doesNotContain("marko@example.com");
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("phone").asText())
        .isEqualTo("+386 41 555 123");
  }

  @Test
  void AC_016_02_theOnlyOwnerIsRefusedAndTheGroupIsNamed() {
    Res r = delete(ANA, "/me").expectError(409, "SOLE_OWNER");
    assertThat(r.at("/groups/0/name").asText()).isEqualTo("Konferenca 2026");
    assertThat(r.at("/groups/0/id").asText()).isEqualTo(gid);
    assertThat(get(ANA, "/groups").expect(200).json()).hasSize(1);
  }

  @Test
  void AC_016_03_afterHandingOverTheOwnershipTheAccountCanBeDeleted() {
    put(ANA, "/groups/" + gid + "/members/" + MARKO.uid(), j("role", "OWNER")).expect(200);
    delete(ANA, "/me").expect(204);
    assertThat(get(MARKO, "/groups/" + gid).expect(200).at("/role").asText()).isEqualTo("OWNER");
    assertThat(listsMember(MARKO, ANA)).isFalse();
  }

  @Test
  void AC_016_04_anOldTokenOfADeletedAccountIsRefused() {
    delete(MARKO, "/me").expect(204);
    get(MARKO, "/groups").expectError(401, "UNAUTHENTICATED");
    get(MARKO, "/me").expectError(401, "UNAUTHENTICATED");
  }

  @Test
  void AC_016_06_aSignInOlderThanFiveMinutesIsAskedToSignInAgain() {
    Instant now = clock.instant();
    String old =
        token(
            MARKO,
            PROJECT,
            now.minusSeconds(3600),
            now.plus(Duration.ofDays(1)),
            now.minusSeconds(600));
    request("DELETE", "/me", "Bearer " + old, null).expectError(403, "RECENT_LOGIN_REQUIRED");
    assertThat(listsMember(ANA, MARKO)).isTrue();
    String recent =
        token(
            MARKO,
            PROJECT,
            now.minusSeconds(3600),
            now.plus(Duration.ofDays(1)),
            now.minusSeconds(120));
    request("DELETE", "/me", "Bearer " + recent, null).expect(204);
  }

  @Test
  void EX_016_01_theOnlyOwnerOfAGroupInTrashStillCounts() {
    String solo = group(BORIS, "Poroka");
    post(BORIS, "/groups/" + solo + "/trash", j("name", "Poroka")).expect(204);
    Res r = delete(BORIS, "/me").expectError(409, "SOLE_OWNER");
    assertThat(r.at("/groups/0/name").asText()).isEqualTo("Poroka");
  }

  @Test
  void EX_016_02_aViewerOnlyMayDelete() {
    join(gid, ANA, PETRA, "VIEWER");
    delete(PETRA, "/me").expect(204);
    assertThat(listsMember(ANA, PETRA)).isFalse();
  }

  @Test
  void EX_016_03_aPendingInvitationForTheDeletedAddressStaysForANewAccount() {
    String other = group(BORIS, "Poroka");
    get(PETRA, "/me").expect(200);
    post(
            BORIS,
            "/groups/" + other + "/invitations",
            j("email", "petra@example.com", "role", "EDITOR"))
        .expect(201);
    delete(PETRA, "/me").expect(204);
    assertThat(get(BORIS, "/groups/" + other + "/invitations").expect(200).json()).hasSize(1);
    Who again = new Who("uid-petra-2", "petra@example.com", "Petra Kos", true);
    assertThat(get(again, "/groups").expect(200).json()).hasSize(1);
  }

  @Test
  void EX_016_04_aUserWithoutAGroupMayDelete() {
    get(PETRA, "/me").expect(200);
    delete(PETRA, "/me").expect(204);
  }

  @Test
  void EX_016_05_everyGroupOfASoleOwnerIsNamed() {
    String a = group(BORIS, "Poroka");
    String b = group(BORIS, "Krst");
    Res r = delete(BORIS, "/me").expectError(409, "SOLE_OWNER");
    assertThat(values(r.at("/groups"), "name")).containsExactlyInAnyOrder("Poroka", "Krst");
    assertThat(values(r.at("/groups"), "id")).containsExactlyInAnyOrder(a, b);
  }
}
