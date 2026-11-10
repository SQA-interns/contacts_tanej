package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * T-38 and T-39, own tests of US-015: what an Owner may do with a group in Trash (D-17), the name
 * rule at restore (R-015-10), the History entries (R-015-09) and the purge with its cascade.
 */
class GroupTrashIT extends AcceptanceBase {

  private static final String NAME = "Konferenca 2026";
  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, NAME);
  }

  private void trashIt() {
    post(ANA, "/groups/" + gid + "/trash", j("name", NAME)).expect(204);
  }

  private int count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }

  @Test
  void anOwnerGetsNotFoundForEverythingExceptDeletedGroupsAndRestore() {
    trashIt();
    get(ANA, "/groups/" + gid).expectError(404, "NOT_FOUND");
    get(ANA, "/groups/" + gid + "/members").expectError(404, "NOT_FOUND");
    post(ANA, "/groups/" + gid + "/contacts/search", j()).expectError(404, "NOT_FOUND");
    patch(ANA, "/groups/" + gid, j("name", "Novo")).expectError(404, "NOT_FOUND");
  }

  @Test
  void deletingAGroupAlreadyInTrashIsRefusedForAnOwnerAndNotFoundForOthers() {
    join(gid, ANA, MARKO, "EDITOR");
    trashIt();
    post(ANA, "/groups/" + gid + "/trash", j("name", NAME)).expectError(409, "GROUP_IN_TRASH");
    post(MARKO, "/groups/" + gid + "/trash", j("name", NAME)).expectError(404, "NOT_FOUND");
  }

  @Test
  void aNonMemberGetsNotFoundForDeleteAndRestore() {
    post(BORIS, "/groups/" + gid + "/trash", j("name", NAME)).expectError(404, "NOT_FOUND");
    post(BORIS, "/groups/" + gid + "/restore", null).expectError(404, "NOT_FOUND");
  }

  @Test
  void restoringAGroupThatIsNotInTrashIsNotFound() {
    post(ANA, "/groups/" + gid + "/restore", null).expectError(404, "NOT_FOUND");
  }

  @Test
  void aMemberWhoIsNotAnOwnerCannotRestore() {
    join(gid, ANA, MARKO, "EDITOR");
    trashIt();
    post(MARKO, "/groups/" + gid + "/restore", null).expectError(404, "NOT_FOUND");
    assertThat(get(ANA, "/deleted-groups").expect(200).json()).hasSize(1);
  }

  @Test
  void aMismatchedNameLeavesTheGroupActiveAndWritesNoEntry() {
    post(ANA, "/groups/" + gid + "/trash", j("name", "x"))
        .expectFieldError("name", "NAME_MISMATCH");
    assertThat(count("history_entries")).isZero();
  }

  @Test
  void restoreIsRefusedWhenAnOwnerWouldHaveTwoGroupsWithTheSameName() {
    join(gid, ANA, BORIS, "OWNER");
    trashIt();
    group(BORIS, "konferenca 2026");
    post(ANA, "/groups/" + gid + "/restore", null).expectError(409, "GROUP_NAME_TAKEN");
    assertThat(get(ANA, "/deleted-groups").expect(200).json()).hasSize(1);
    assertThat(get(ANA, "/groups").expect(200).json()).isEmpty();
  }

  @Test
  void deleteAndRestoreEachWriteOneGroupLevelEntry() {
    trashIt();
    post(ANA, "/groups/" + gid + "/restore", null).expect(204);
    assertThat(
            jdbc.queryForList(
                "SELECT event_type FROM history_entries WHERE group_id = ? ORDER BY created_at, id",
                String.class,
                UUID.fromString(gid)))
        .containsExactlyInAnyOrder("GROUP_DELETED", "GROUP_RESTORED");
  }

  @Test
  void daysLeftCountsDownWhileTheGroupIsInTrash() {
    trashIt();
    clock.advance(Duration.ofDays(5).plusHours(1));
    assertThat(get(ANA, "/deleted-groups").expect(200).json().get(0).path("daysLeft").asInt())
        .isEqualTo(25);
  }

  @Test
  void thePurgeRemovesContactsHistoryInvitationsAndMembershipsButNotAnotherGroup() {
    join(gid, ANA, MARKO, "EDITOR");
    newContact(ANA, gid, j("lastName", "Novak", "tags", arr("x")));
    post(ANA, "/groups/" + gid + "/invitations", j("email", "ivo@example.com", "role", "VIEWER"))
        .expect(201);
    String other = group(ANA, "Druga");
    trashIt();
    clock.advance(Duration.ofDays(30));
    eventually(() -> count("groups") == 1, "the group in Trash is purged");
    assertThat(count("contacts")).isZero();
    assertThat(count("invitations")).isZero();
    assertThat(count("tags")).isZero();
    assertThat(count("history_entries")).isZero();
    assertThat(count("memberships")).isEqualTo(1);
    assertThat(get(ANA, "/groups/" + other).expect(200).at("/name").asText()).isEqualTo("Druga");
  }

  @Test
  void aGroupInTrashForLessThan30DaysIsNotPurged() throws InterruptedException {
    trashIt();
    clock.advance(Duration.ofDays(29).plusHours(23));
    Thread.sleep(2500);
    assertThat(count("groups")).isEqualTo(1);
  }
}
