package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** T-16, own tests of US-004 for the rules the frozen acceptance tests do not pin. */
class MembersIT extends AcceptanceBase {

  private String groupWith() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    return gid;
  }

  @Test
  void aViewerSeesTheMemberListWithNameEmailAndRole() {
    String gid = groupWith();
    var members = get(PETRA, "/groups/" + gid + "/members").expect(200).json();
    assertThat(members).hasSize(3);
    assertThat(members.get(0).fieldNames())
        .toIterable()
        .contains("userId", "name", "email", "role");
  }

  @Test
  void anUnknownRoleIsRefusedWith400AndNothingChanges() {
    String gid = groupWith();
    put(ANA, "/groups/" + gid + "/members/" + MARKO.uid(), j("role", "BOSS"))
        .expectError(400, "VALIDATION_FAILED");
    assertThat(roleInDb(gid, MARKO.uid())).isEqualTo("EDITOR");
  }

  @Test
  void changingOrRemovingAPersonWhoIsNoMemberIsNotFound() {
    String gid = groupWith();
    put(ANA, "/groups/" + gid + "/members/" + BORIS.uid(), j("role", "VIEWER"))
        .expectError(404, "NOT_FOUND");
    delete(ANA, "/groups/" + gid + "/members/" + BORIS.uid()).expectError(404, "NOT_FOUND");
  }

  @Test
  void aNonOwnerCannotRemoveAnotherMember() {
    String gid = groupWith();
    delete(MARKO, "/groups/" + gid + "/members/" + PETRA.uid()).expectError(403, "FORBIDDEN");
    assertThat(roleInDb(gid, PETRA.uid())).isEqualTo("VIEWER");
  }

  @Test
  void aNonMemberCannotChangeRemoveOrLeave() {
    String gid = groupWith();
    put(BORIS, "/groups/" + gid + "/members/" + MARKO.uid(), j("role", "VIEWER"))
        .expectError(404, "NOT_FOUND");
    delete(BORIS, "/groups/" + gid + "/members/" + MARKO.uid()).expectError(404, "NOT_FOUND");
    post(BORIS, "/groups/" + gid + "/leave", null).expectError(404, "NOT_FOUND");
  }

  @Test
  void aSecondOwnerMayBeDemotedAndRemovedButNeverTheLast() {
    String gid = groupWith();
    put(ANA, "/groups/" + gid + "/members/" + MARKO.uid(), j("role", "OWNER")).expect(200);
    put(MARKO, "/groups/" + gid + "/members/" + ANA.uid(), j("role", "VIEWER")).expect(200);
    delete(MARKO, "/groups/" + gid + "/members/" + MARKO.uid()).expectError(409, "LAST_OWNER");
  }

  private String roleInDb(String gid, String uid) {
    return jdbc.queryForObject(
        "SELECT role FROM memberships WHERE group_id = ? AND user_id = ?",
        String.class,
        UUID.fromString(gid),
        uid);
  }
}
