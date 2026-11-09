package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-015 Delete and restore a group: AC-015-01 to AC-015-08, EX-015-01 to EX-015-06. */
class Us015GroupTrashAcceptanceIT extends AcceptanceBase {

  private static final String NAME = "Konferenca 2026";
  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, NAME);
    join(gid, ANA, MARKO, "EDITOR");
  }

  private Res trashGroup(Who who, String typed) {
    return post(who, "/groups/" + gid + "/trash", j("name", typed));
  }

  private Res restoreGroup(Who who) {
    return post(who, "/groups/" + gid + "/restore", null);
  }

  private JsonNode deleted(Who who) {
    return get(who, "/deleted-groups").expect(200).json();
  }

  @Test
  void AC_015_01_deletingWithTheExactNameHidesTheGroupAndListsItInDeletedGroups() {
    trashGroup(ANA, NAME).expect(204);
    assertThat(get(MARKO, "/groups").expect(200).json()).isEmpty();
    assertThat(get(ANA, "/groups").expect(200).json()).isEmpty();
    JsonNode d = deleted(ANA);
    assertThat(d).hasSize(1);
    assertThat(d.get(0).path("name").asText()).isEqualTo(NAME);
    assertThat(d.get(0).path("daysLeft").asInt()).isEqualTo(30);
    assertThat(deleted(MARKO)).isEmpty();
  }

  @Test
  void AC_015_02_aNameThatDiffersInCaseDeletesNothing() {
    trashGroup(ANA, "konferenca 2026").expectFieldError("name", "NAME_MISMATCH");
    assertThat(get(ANA, "/groups").expect(200).json()).hasSize(1);
    assertThat(deleted(ANA)).isEmpty();
  }

  @Test
  void AC_015_03_aMemberWhoIsNotAnOwnerGetsNotFoundForAGroupInTrash() {
    trashGroup(ANA, NAME).expect(204);
    get(MARKO, "/groups/" + gid).expectError(404, "NOT_FOUND");
    get(MARKO, "/groups/" + gid + "/members").expectError(404, "NOT_FOUND");
    post(MARKO, "/groups/" + gid + "/contacts/search", j()).expectError(404, "NOT_FOUND");
  }

  @Test
  void AC_015_04_restoringBringsBackContactsRolesAndHistory() {
    newContact(ANA, gid, j("lastName", "Novak", "tags", arr("x")));
    trashGroup(ANA, NAME).expect(204);
    restoreGroup(ANA).expect(204);
    assertThat(get(MARKO, "/groups").expect(200).json()).hasSize(1);
    assertThat(get(MARKO, "/groups/" + gid).at("/role").asText()).isEqualTo("EDITOR");
    assertThat(search(MARKO, gid, "").at("/total").asInt()).isEqualTo(1);
    assertThat(entries(ANA, gid, "CONTACT_CREATED")).hasSize(1);
    assertThat(deleted(ANA)).isEmpty();
  }

  @Test
  void AC_015_05_afterThirtyDaysTheCleanupDeletesTheGroupAndAllItsData() {
    newContact(ANA, gid, j("lastName", "Novak"));
    trashGroup(ANA, NAME).expect(204);
    clock.advance(Duration.ofDays(30).plusMinutes(1));
    eventually(() -> deleted(ANA).isEmpty(), "the group is purged by the cleanup");
    restoreGroup(ANA).expectError(404, "NOT_FOUND");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM contacts", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM memberships", Integer.class)).isZero();
  }

  @Test
  void AC_015_06_anEditorCannotDeleteTheGroup() {
    trashGroup(MARKO, NAME).expectError(403, "FORBIDDEN");
    assertThat(get(ANA, "/groups").expect(200).json()).hasSize(1);
  }

  @Test
  void AC_015_07_aGroupInTrashRefusesChangesToItsContacts() {
    trashGroup(ANA, NAME).expect(204);
    post(ANA, "/groups/" + gid + "/contacts", j("lastName", "Novak")).expectError(404, "NOT_FOUND");
    post(ANA, "/groups/" + gid + "/invitations", j("email", "ivo@example.com", "role", "VIEWER"))
        .expectError(404, "NOT_FOUND");
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expectError(404, "NOT_FOUND");
  }

  @Test
  void AC_015_08_anotherOwnerSeesTheGroupInDeletedGroupsAndCanRestoreIt() {
    join(gid, ANA, BORIS, "OWNER");
    trashGroup(ANA, NAME).expect(204);
    assertThat(deleted(BORIS)).hasSize(1);
    restoreGroup(BORIS).expect(204);
    assertThat(get(BORIS, "/groups").expect(200).json()).hasSize(1);
  }

  @Test
  void EX_015_01_aGroupInTrashFor29Days23HoursIsRestorable() {
    trashGroup(ANA, NAME).expect(204);
    clock.advance(Duration.ofDays(29).plusHours(23));
    restoreGroup(ANA).expect(204);
  }

  @Test
  void EX_015_02_aGroupInTrashForExactly30DaysIsErasedByTheNextCleanup() {
    trashGroup(ANA, NAME).expect(204);
    clock.advance(Duration.ofDays(30));
    eventually(() -> deleted(ANA).isEmpty(), "the group is purged by the cleanup");
  }

  @Test
  void EX_015_03_aPendingInvitationSurvivesDeletionAndRestoration() {
    post(ANA, "/groups/" + gid + "/invitations", j("email", "petra@example.com", "role", "VIEWER"))
        .expect(201);
    trashGroup(ANA, NAME).expect(204);
    restoreGroup(ANA).expect(204);
    assertThat(get(ANA, "/groups/" + gid + "/invitations").expect(200).json()).hasSize(1);
    assertThat(get(PETRA, "/groups").expect(200).json()).hasSize(1);
  }

  @Test
  void EX_015_04_spacesAroundTheTypedNameAreTrimmed() {
    trashGroup(ANA, "  " + NAME + "  ").expect(204);
  }

  @Test
  void EX_015_05_twoOwnersDeletingAtOnceGiveOneSuccessAndOneRefusal() throws Exception {
    join(gid, ANA, BORIS, "OWNER");
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Callable<Res>> calls =
          List.of(() -> trashGroup(ANA, NAME), () -> trashGroup(BORIS, NAME));
      int ok = 0;
      int refused = 0;
      for (Future<Res> f : pool.invokeAll(calls)) {
        Res r = f.get();
        if (r.status() == 204) {
          ok++;
        } else if ((r.status() == 409 && "GROUP_IN_TRASH".equals(r.code()))
            || (r.status() == 404 && "NOT_FOUND".equals(r.code()))) {
          refused++;
        }
      }
      assertThat(ok).isEqualTo(1);
      assertThat(refused).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
    assertThat(deleted(ANA)).hasSize(1);
  }

  @Test
  void EX_015_06_aContactInTrashKeepsItsOwnThirtyDaysWhileTheGroupIsInTrash() {
    String c = newContact(ANA, gid, j("lastName", "Novak")).path("id").asText();
    trash(ANA, gid, c);
    clock.advance(Duration.ofDays(10));
    trashGroup(ANA, NAME).expect(204);
    clock.advance(Duration.ofDays(20).plusMinutes(1));
    // the contact has been in Trash for 30 days and 1 minute, the group for 20 days
    eventually(
        () -> jdbc.queryForObject("SELECT count(*) FROM contacts", Integer.class) == 0,
        "the contact is erased on its own schedule");
    restoreGroup(ANA).expect(204);
    get(ANA, "/groups/" + gid + "/contacts/" + c).expectError(404, "NOT_FOUND");
  }
}
