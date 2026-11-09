package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** US-004 Manage members and roles: AC-004-01 to AC-004-08, EX-004-01 to EX-004-05. */
class Us004MembersAcceptanceIT extends AcceptanceBase {

  private Res role(Who owner, String gid, Who member, String role) {
    return put(owner, "/groups/" + gid + "/members/" + member.uid(), j("role", role));
  }

  private String roleOf(Who viewer, String gid, Who member) {
    for (JsonNode m : get(viewer, "/groups/" + gid + "/members").expect(200).json()) {
      if (member.uid().equals(m.path("userId").asText())) {
        return m.path("role").asText();
      }
    }
    return null;
  }

  @Test
  void AC_004_01_aChangedRoleAppliesAtTheNextRequest() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    newContact(MARKO, gid, "Ana", "Novak");
    role(ANA, gid, MARKO, "VIEWER").expect(200);
    assertThat(roleOf(ANA, gid, MARKO)).isEqualTo("VIEWER");
    post(MARKO, "/groups/" + gid + "/contacts", j("firstName", "Eva"))
        .expectError(403, "FORBIDDEN");
    assertThat(get(MARKO, "/groups/" + gid).at("/role").asText()).isEqualTo("VIEWER");
  }

  @Test
  void AC_004_02_aRemovedMemberGetsNotFoundAndHisContactsAndHistoryStay() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    JsonNode contact = newContact(MARKO, gid, "Ana", "Novak");
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    get(MARKO, "/groups/" + gid).expectError(404, "NOT_FOUND");
    assertThat(get(MARKO, "/groups").expect(200).json()).isEmpty();
    assertThat(openContact(ANA, gid, contact.path("id").asText()).path("lastName").asText())
        .isEqualTo("Novak");
    JsonNode created = entries(ANA, gid, "CONTACT_CREATED").get(0);
    assertThat(created.at("/actor/name").asText()).isEqualTo("Marko Pirc");
  }

  @Test
  void AC_004_03_theOnlyOwnerCannotLeaveDemoteOrRemoveSelf() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    post(ANA, "/groups/" + gid + "/leave", null).expectError(409, "LAST_OWNER");
    role(ANA, gid, ANA, "EDITOR").expectError(409, "LAST_OWNER");
    delete(ANA, "/groups/" + gid + "/members/" + ANA.uid()).expectError(409, "LAST_OWNER");
    assertThat(roleOf(ANA, gid, ANA)).isEqualTo("OWNER");
  }

  @Test
  void AC_004_04_anOwnerCanHandOverAndLeave() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    role(ANA, gid, MARKO, "OWNER").expect(200);
    post(ANA, "/groups/" + gid + "/leave", null).expect(204);
    get(ANA, "/groups/" + gid).expectError(404, "NOT_FOUND");
    assertThat(roleOf(MARKO, gid, MARKO)).isEqualTo("OWNER");
  }

  @Test
  void AC_004_05_aViewerCanLeaveAndTheGroupLeavesHisList() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, PETRA, "VIEWER");
    post(PETRA, "/groups/" + gid + "/leave", null).expect(204);
    assertThat(get(PETRA, "/groups").expect(200).json()).isEmpty();
  }

  @Test
  void AC_004_06_twoOwnersDemotingThemselvesAtOnceLeaveAnOwner() throws Exception {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, BORIS, "OWNER");
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Callable<Res>> calls =
          List.of(() -> role(ANA, gid, ANA, "EDITOR"), () -> role(BORIS, gid, BORIS, "EDITOR"));
      int refused = 0;
      for (Future<Res> f : pool.invokeAll(calls)) {
        if (f.get().status() == 409) {
          refused++;
        }
      }
      assertThat(refused)
          .as("at least one of the two requests is refused")
          .isGreaterThanOrEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
    List<String> roles = values(get(ANA, "/groups/" + gid + "/members").expect(200).json(), "role");
    assertThat(roles).contains("OWNER");
  }

  @Test
  void AC_004_07_anEditorCannotChangeARole() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    role(MARKO, gid, PETRA, "EDITOR").expectError(403, "FORBIDDEN");
    assertThat(roleOf(ANA, gid, PETRA)).isEqualTo("VIEWER");
  }

  @Test
  void AC_004_08_aNonMemberCannotListTheMembers() {
    String gid = group(ANA, "Konferenca 2026");
    get(BORIS, "/groups/" + gid + "/members").expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_004_01_anOwnerCanRemoveAnotherOwnerWhileOneRemains() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, BORIS, "OWNER");
    delete(ANA, "/groups/" + gid + "/members/" + BORIS.uid()).expect(204);
    assertThat(roleOf(ANA, gid, BORIS)).isNull();
  }

  @Test
  void EX_004_02_changingToTheRoleAlreadyHeldChangesNothingAndIsNoError() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    role(ANA, gid, MARKO, "EDITOR").expect(200);
    assertThat(roleOf(ANA, gid, MARKO)).isEqualTo("EDITOR");
  }

  @Test
  void EX_004_03_aRemovedMembersNextActionIsNotFound() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    post(MARKO, "/groups/" + gid + "/contacts/search", j("q", "")).expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_004_04_aRemovedMemberSigningInAgainSeesNoGroup() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    assertThat(get(MARKO, "/me").expect(200).json()).isNotNull();
    assertThat(get(MARKO, "/groups").expect(200).json()).isEmpty();
  }

  @Test
  void EX_004_05_theLastOwnerCannotDeleteTheAccount() {
    group(ANA, "Konferenca 2026");
    delete(ANA, "/me").expectError(409, "SOLE_OWNER");
    assertThat(get(ANA, "/groups").expect(200).json()).hasSize(1);
  }
}
