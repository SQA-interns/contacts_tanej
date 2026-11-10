package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import app.contactgroups.acceptance.Who;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * T-10 and T-11, own tests of US-002 for what the frozen acceptance tests cannot reach before the
 * later stories exist (members, Trash, History tab): the access check, the name rule per Owner
 * under concurrency and the History entry of a rename. Invitations (US-003) are not built yet, so
 * members are inserted directly.
 */
class GroupsIT extends AcceptanceBase {

  /** Adds the person to the group with the role, as an accepted invitation would. */
  private void member(String gid, Who who, String role) {
    get(who, "/me").expect(200);
    jdbc.update(
        "INSERT INTO memberships (group_id, user_id, role, created_at) VALUES (?, ?, ?, now())",
        UUID.fromString(gid),
        who.uid(),
        role);
  }

  private void toTrash(String gid) {
    jdbc.update(
        "UPDATE groups SET state = 'TRASHED', trashed_at = now() WHERE id = ?",
        UUID.fromString(gid));
  }

  private List<String> names(Who who) {
    List<String> out = new ArrayList<>();
    for (JsonNode g : get(who, "/groups").expect(200).json()) {
      out.add(g.path("name").asText());
    }
    return out;
  }

  private List<String> renameEntries(String gid) {
    return jdbc.queryForList(
        "SELECT e.actor_name || ':' || c.old_value || '>' || c.new_value FROM history_entries e"
            + " JOIN history_changes c ON c.entry_id = e.id WHERE e.group_id = ? AND e.event_type ="
            + " 'GROUP_RENAMED'",
        String.class,
        UUID.fromString(gid));
  }

  // ------------------------------------------------------------ create, list, open (T-10)

  @Test
  void theCreatorIsTheOnlyMemberAndOwnerAndNoHistoryEntryIsWritten() {
    String gid = group(ANA, "Konferenca 2026");
    assertThat(
            jdbc.queryForList(
                "SELECT user_id || ':' || role FROM memberships WHERE group_id = ?",
                String.class,
                UUID.fromString(gid)))
        .containsExactly(ANA.uid() + ":OWNER");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM history_entries", Integer.class)).isZero();
  }

  @Test
  void theNameIsStoredTrimmed() {
    Res r = post(ANA, "/groups", j("name", "  Poroka  ")).expect(201);
    assertThat(r.at("/name").asText()).isEqualTo("Poroka");
  }

  @Test
  void aBodyWithoutAFieldNameIsRefusedWithRequired() {
    post(ANA, "/groups", j()).expectFieldError("name", "REQUIRED");
    post(ANA, "/groups", j("name", null)).expectFieldError("name", "REQUIRED");
  }

  @Test
  void aUserBelongsToAsManyGroupsAsTheyLikeAndSeesTheirRolePerGroup() {
    String own = group(ANA, "Moja");
    String foreign = group(BORIS, "Tuja");
    member(foreign, ANA, "VIEWER");
    for (int i = 0; i < 30; i++) {
      group(ANA, "Skupina " + i);
    }
    JsonNode list = get(ANA, "/groups").expect(200).json();
    assertThat(list).hasSize(32);
    for (JsonNode g : list) {
      if (g.path("id").asText().equals(foreign)) {
        assertThat(g.path("role").asText()).isEqualTo("VIEWER");
      } else if (g.path("id").asText().equals(own)) {
        assertThat(g.path("role").asText()).isEqualTo("OWNER");
      }
    }
    assertThat(names(BORIS)).containsExactly("Tuja");
  }

  @Test
  void theListIsInSlovenianAlphabeticalOrder() {
    group(ANA, "Šola");
    group(ANA, "Cesta");
    group(ANA, "Čas");
    group(ANA, "Sonce");
    group(ANA, "Zlato");
    assertThat(names(ANA)).containsExactly("Cesta", "Čas", "Sonce", "Šola", "Zlato");
  }

  @Test
  void aGroupInTrashIsInNoListAndIsNotFoundForItsOwnersAndMembers() {
    String gid = group(ANA, "Konferenca 2026");
    member(gid, MARKO, "EDITOR");
    toTrash(gid);
    assertThat(names(ANA)).isEmpty();
    assertThat(names(MARKO)).isEmpty();
    get(ANA, "/groups/" + gid).expectError(404, "NOT_FOUND");
    get(MARKO, "/groups/" + gid).expectError(404, "NOT_FOUND");
    patch(ANA, "/groups/" + gid, j("name", "Novo")).expectError(404, "NOT_FOUND");
  }

  @Test
  void openingAGroupGivesTheRoleOfTheCaller() {
    String gid = group(ANA, "Konferenca 2026");
    member(gid, MARKO, "VIEWER");
    assertThat(get(MARKO, "/groups/" + gid).expect(200).at("/role").asText()).isEqualTo("VIEWER");
    assertThat(get(ANA, "/groups/" + gid).expect(200).at("/id").asText()).isEqualTo(gid);
  }

  @Test
  void theRoleIsReadAtEveryRequest() {
    String gid = group(ANA, "Konferenca 2026");
    member(gid, MARKO, "OWNER");
    patch(MARKO, "/groups/" + gid, j("name", "Prvo")).expect(200);
    jdbc.update("UPDATE memberships SET role = 'VIEWER' WHERE user_id = ?", MARKO.uid());
    patch(MARKO, "/groups/" + gid, j("name", "Drugo")).expectError(403, "FORBIDDEN");
    jdbc.update("DELETE FROM memberships WHERE user_id = ?", MARKO.uid());
    patch(MARKO, "/groups/" + gid, j("name", "Tretje")).expectError(404, "NOT_FOUND");
  }

  @Test
  void aNonMemberGetsTheSame404ForEveryRequestOnTheGroupWhateverTheBody() {
    String gid = group(ANA, "Poroka");
    Res read = get(BORIS, "/groups/" + gid).expectError(404, "NOT_FOUND");
    Res renamed = patch(BORIS, "/groups/" + gid, j("name", "   ")).expectError(404, "NOT_FOUND");
    assertThat(renamed.text()).isEqualTo(read.text());
    assertThat(read.text()).doesNotContain("Poroka");
    assertThat(get(ANA, "/groups/" + gid).at("/name").asText()).isEqualTo("Poroka");
  }

  @Test
  void twoParallelCreatesWithTheSameNameLetOnlyOneThrough() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(6);
    try {
      List<Future<Integer>> calls = new ArrayList<>();
      for (int i = 0; i < 6; i++) {
        calls.add(pool.submit(() -> post(ANA, "/groups", j("name", "Konferenca 2026")).status()));
      }
      int created = 0;
      for (Future<Integer> f : calls) {
        int status = f.get();
        assertThat(status).isIn(201, 409);
        created += status == 201 ? 1 : 0;
      }
      assertThat(created).isEqualTo(1);
      assertThat(names(ANA)).containsExactly("Konferenca 2026");
    } finally {
      pool.shutdownNow();
    }
  }

  // ------------------------------------------------------------ name rule and rename (T-11)

  @Test
  void aTrashedGroupOfTheSameOwnerDoesNotBlockARenameEither() {
    String old = group(ANA, "Konferenca 2026");
    toTrash(old);
    String other = group(ANA, "Poroka");
    patch(ANA, "/groups/" + other, j("name", "konferenca 2026")).expect(200);
  }

  @Test
  void aRenameIsRefusedWhenAnyOtherOwnerOfTheGroupAlreadyHasThatName() {
    String shared = group(ANA, "Skupno");
    member(shared, BORIS, "OWNER");
    group(BORIS, "Poroka");
    patch(ANA, "/groups/" + shared, j("name", "PORoka")).expectError(409, "GROUP_NAME_TAKEN");
    assertThat(get(ANA, "/groups/" + shared).at("/name").asText()).isEqualTo("Skupno");
    assertThat(renameEntries(shared)).isEmpty();
  }

  @Test
  void theSameNameIsAllowedForGroupsOfDifferentOwnersAlsoOnRename() {
    group(BORIS, "Poroka");
    String gid = group(ANA, "Svatba");
    patch(ANA, "/groups/" + gid, j("name", "Poroka")).expect(200);
  }

  @Test
  void aRenameKeepsTheCreateRuleForAnOwnerWhoWasPromotedIntoAPair() {
    String a = group(ANA, "Isto");
    String b = group(BORIS, "Isto");
    member(b, ANA, "OWNER");
    patch(ANA, "/groups/" + a, j("name", "Isto")).expect(200);
    patch(BORIS, "/groups/" + b, j("name", "Ostalo")).expect(200);
    patch(ANA, "/groups/" + a, j("name", "Ostalo")).expectError(409, "GROUP_NAME_TAKEN");
  }

  @Test
  void aRenameIsValidatedLikeACreate() {
    String gid = group(ANA, "Konferenca 2026");
    patch(ANA, "/groups/" + gid, j("name", "  ")).expectFieldError("name", "REQUIRED");
    patch(ANA, "/groups/" + gid, j("name", repeat("a", 101))).expectFieldError("name", "TOO_LONG");
    patch(ANA, "/groups/" + gid, j("name", repeat("a", 100))).expect(200);
    assertThat(renameEntries(gid)).hasSize(1);
  }

  @Test
  void aRenameWritesOneGroupEntryWithOldAndNewNameAndTheActor() {
    String gid = group(ANA, "Konferenca 2026");
    Res r = patch(ANA, "/groups/" + gid, j("name", "  Poroka ")).expect(200);
    assertThat(r.at("/name").asText()).isEqualTo("Poroka");
    assertThat(r.at("/role").asText()).isEqualTo("OWNER");
    assertThat(renameEntries(gid)).containsExactly("Ana Novak:Konferenca 2026>Poroka");
  }

  @Test
  void aChangeOfCapitalisationIsRecordedToo() {
    String gid = group(ANA, "Konferenca 2026");
    patch(ANA, "/groups/" + gid, j("name", "KONFERENCA 2026")).expect(200);
    assertThat(renameEntries(gid)).containsExactly("Ana Novak:Konferenca 2026>KONFERENCA 2026");
  }

  @Test
  void aRenameToTheSameNameChangesNothingAndWritesNoEntry() {
    String gid = group(ANA, "Konferenca 2026");
    patch(ANA, "/groups/" + gid, j("name", " Konferenca 2026 ")).expect(200);
    assertThat(renameEntries(gid)).isEmpty();
  }

  @Test
  void aRefusedRenameWritesNoEntry() {
    String gid = group(ANA, "Konferenca 2026");
    String other = group(ANA, "Poroka");
    member(gid, MARKO, "VIEWER");
    patch(ANA, "/groups/" + other, j("name", "konferenca 2026"))
        .expectError(409, "GROUP_NAME_TAKEN");
    patch(MARKO, "/groups/" + gid, j("name", "Novo")).expectError(403, "FORBIDDEN");
    patch(ANA, "/groups/" + gid, j("name", "")).expectFieldError("name", "REQUIRED");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM history_entries", Integer.class)).isZero();
  }

  @Test
  void aViewerCannotRenameEither() {
    String gid = group(ANA, "Konferenca 2026");
    member(gid, MARKO, "VIEWER");
    patch(MARKO, "/groups/" + gid, j("name", "Novo")).expectError(403, "FORBIDDEN");
  }

  @Test
  void twoParallelRenamesToTheSameNameLetOnlyOneThrough() throws Exception {
    String first = group(ANA, "Ena");
    String second = group(ANA, "Dva");
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> a =
          pool.submit(() -> patch(ANA, "/groups/" + first, j("name", "Tri")).status());
      Future<Integer> b =
          pool.submit(() -> patch(ANA, "/groups/" + second, j("name", "TRI")).status());
      assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 409);
    } finally {
      pool.shutdownNow();
    }
  }
}
