package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** US-002 Create a group and list my groups: AC-002-01 to AC-002-10, EX-002-01 to EX-002-05. */
class Us002GroupsAcceptanceIT extends AcceptanceBase {

  private JsonNode groupsOf(Who who) {
    return get(who, "/groups").expect(200).json();
  }

  @Test
  void AC_002_01_creatingAGroupMakesTheCreatorItsOwnerAndOpensItEmpty() {
    Res created = post(ANA, "/groups", j("name", "Konferenca 2026")).expect(201);
    String gid = created.at("/id").asText();
    assertThat(created.at("/name").asText()).isEqualTo("Konferenca 2026");
    assertThat(created.at("/role").asText()).isEqualTo("OWNER");
    Res opened = get(ANA, "/groups/" + gid).expect(200);
    assertThat(opened.at("/role").asText()).isEqualTo("OWNER");
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
  }

  @Test
  void AC_002_02_theListShowsOnlyMyGroupsWithMyRole() {
    group(ANA, "Konferenca 2026");
    group(BORIS, "Poroka");
    JsonNode list = groupsOf(ANA);
    assertThat(list).hasSize(1);
    assertThat(list.get(0).path("name").asText()).isEqualTo("Konferenca 2026");
    assertThat(list.get(0).path("role").asText()).isEqualTo("OWNER");
  }

  @Test
  void AC_002_03_aGroupOfSomeoneElseIsNotFoundAndRevealsNothing() {
    group(ANA, "Konferenca 2026");
    String poroka = group(BORIS, "Poroka");
    Res foreign = get(ANA, "/groups/" + poroka).expectError(404, "NOT_FOUND");
    Res missing = get(ANA, "/groups/" + UUID.randomUUID()).expectError(404, "NOT_FOUND");
    assertThat(foreign.text()).isEqualTo(missing.text());
    assertThat(foreign.text()).doesNotContain("Poroka");
  }

  @Test
  void AC_002_04_aNameOfOnlySpacesIsRefusedAndNoGroupIsCreated() {
    post(ANA, "/groups", j("name", "   ")).expectFieldError("name", "REQUIRED");
    assertThat(groupsOf(ANA)).isEmpty();
  }

  @Test
  void AC_002_05_aSecondGroupWithTheSameNameIgnoringCaseIsRefused() {
    group(ANA, "Konferenca 2026");
    post(ANA, "/groups", j("name", "KONFERENCA 2026")).expectError(409, "GROUP_NAME_TAKEN");
    assertThat(groupsOf(ANA)).hasSize(1);
  }

  @Test
  void AC_002_06_anotherOwnerMayUseTheSameName() {
    group(ANA, "Konferenca 2026");
    post(BORIS, "/groups", j("name", "Konferenca 2026")).expect(201);
  }

  @Test
  void AC_002_07_aGroupInTrashDoesNotBlockTheName() {
    String gid = group(ANA, "Konferenca 2026");
    post(ANA, "/groups/" + gid + "/trash", j("name", "Konferenca 2026")).expect(204);
    post(ANA, "/groups", j("name", "Konferenca 2026")).expect(201);
  }

  @Test
  void AC_002_08_renamingToANameOfAnotherOwnGroupIgnoringCaseIsRefused() {
    group(ANA, "Konferenca 2026");
    String poroka = group(ANA, "Poroka");
    patch(ANA, "/groups/" + poroka, j("name", "konferenca 2026"))
        .expectError(409, "GROUP_NAME_TAKEN");
    assertThat(get(ANA, "/groups/" + poroka).at("/name").asText()).isEqualTo("Poroka");
  }

  @Test
  void AC_002_09_aChangeOfCapitalisationOnlyIsAllowed() {
    String gid = group(ANA, "Konferenca 2026");
    patch(ANA, "/groups/" + gid, j("name", "KONFERENCA 2026")).expect(200);
    assertThat(get(ANA, "/groups/" + gid).at("/name").asText()).isEqualTo("KONFERENCA 2026");
  }

  @Test
  void AC_002_10_anEditorCannotRenameTheGroup() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    patch(MARKO, "/groups/" + gid, j("name", "Drugo ime")).expectError(403, "FORBIDDEN");
    assertThat(get(ANA, "/groups/" + gid).at("/name").asText()).isEqualTo("Konferenca 2026");
  }

  @Test
  void EX_002_01_aNameOfExactly100CharactersIsAccepted() {
    post(ANA, "/groups", j("name", repeat("a", 100))).expect(201);
  }

  @Test
  void EX_002_02_aNameOf101CharactersIsRefusedAndNothingIsStored() {
    post(ANA, "/groups", j("name", repeat("a", 101))).expectFieldError("name", "TOO_LONG");
    assertThat(groupsOf(ANA)).isEmpty();
  }

  @Test
  void EX_002_03_accentsAndSymbolsAreKeptUnchanged() {
    Res r = post(ANA, "/groups", j("name", "Čebelarji & Co")).expect(201);
    assertThat(r.at("/name").asText()).isEqualTo("Čebelarji & Co");
    assertThat(groupsOf(ANA).get(0).path("name").asText()).isEqualTo("Čebelarji & Co");
  }

  @Test
  void EX_002_04_aUserWithAnUnverifiedEmailCannotCreateAGroup() {
    post(ANA.unverified(), "/groups", j("name", "Konferenca 2026"))
        .expectError(403, "EMAIL_NOT_VERIFIED");
    assertThat(groupsOf(ANA)).isEmpty();
  }

  @Test
  void EX_002_05_aGroupInTrashIsInNoMembersList() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    post(ANA, "/groups/" + gid + "/trash", j("name", "Konferenca 2026")).expect(204);
    assertThat(groupsOf(ANA)).isEmpty();
    assertThat(groupsOf(MARKO)).isEmpty();
  }
}
