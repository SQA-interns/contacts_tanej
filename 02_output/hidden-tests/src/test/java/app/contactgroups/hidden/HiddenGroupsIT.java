package app.contactgroups.hidden;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.IVAN;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import app.contactgroups.acceptance.Who;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases for R-002, R-003 and R-004 and BR-01, BR-02, BR-03. */
class HiddenGroupsIT extends AcceptanceBase {

  private Res invite(Who owner, String gid, String email, String role) {
    return post(owner, "/groups/" + gid + "/invitations", j("email", email, "role", role));
  }

  private JsonNode members(Who who, String gid) {
    return get(who, "/groups/" + gid + "/members").expect(200).json();
  }

  private Res setRole(Who owner, String gid, Who member, String role) {
    return put(owner, "/groups/" + gid + "/members/" + member.uid(), j("role", role));
  }

  @Test
  @DisplayName("R-002-01 other creator is the only member and Owner")
  void R_002_01_other_creatorIsOnlyMemberAndOwner() {
    String gid = group(BORIS, "Poroka & več");
    JsonNode m = members(BORIS, gid);
    assertThat(m).hasSize(1);
    assertThat(m.get(0).path("role").asText()).isEqualTo("OWNER");
    assertThat(m.get(0).path("email").asText()).isEqualTo("boris@example.com");
    assertThat(m.get(0).path("name").asText()).isEqualTo("Boris Zupan");
  }

  @Test
  @DisplayName("R-002-02 boundary spaces are trimmed before the length check")
  void R_002_02_boundary_trimmedBeforeLength() {
    post(ANA, "/groups", j("name", "  " + repeat("a", 100) + "  ")).expect(201);
    Res r = post(ANA, "/groups", j("name", "  Abc  ")).expect(201);
    assertThat(r.at("/name").asText()).isEqualTo("Abc");
    post(ANA, "/groups", j("name", repeat("b", 50) + " " + repeat("b", 50)))
        .expectFieldError("name", "TOO_LONG");
    post(ANA, "/groups", j("other", "x")).expectFieldError("name", "REQUIRED");
    post(ANA, "/groups", j("name", "")).expectFieldError("name", "REQUIRED");
  }

  @Test
  @DisplayName("R-002-03 combination a promotion is never refused because of a name")
  void R_002_03_combination_promotionNeverRefused() {
    String mine = group(ANA, "Skupna");
    group(BORIS, "Skupna");
    group(BORIS, "Druga");
    invite(ANA, mine, "boris@example.com", "OWNER").expect(201);
    assertThat(get(BORIS, "/groups").expect(200).json()).hasSize(3);
    // the group of Boris called "skupna" now clashes for him; a new one with that name is refused
    post(BORIS, "/groups", j("name", "SKUPNA")).expectError(409, "GROUP_NAME_TAKEN");
  }

  @Test
  @DisplayName("R-002-03 boundary names differing by one character or by accents are different")
  void R_002_03_boundary_accentsDifferent() {
    group(ANA, "Cebelarji");
    post(ANA, "/groups", j("name", "Čebelarji")).expect(201);
    post(ANA, "/groups", j("name", "Cebelarji ")).expectError(409, "GROUP_NAME_TAKEN");
  }

  @Test
  @DisplayName("R-002-04 other the list shows each role and no group in Trash")
  void R_002_04_other_listShowsEachRole() {
    String a = group(ANA, "G1");
    String b = group(BORIS, "G2");
    String c = group(BORIS, "G3");
    invite(BORIS, b, "ana.novak@example.com", "VIEWER").expect(201);
    invite(BORIS, c, "ana.novak@example.com", "EDITOR").expect(201);
    post(BORIS, "/groups/" + c + "/trash", j("name", "G3")).expect(204);
    JsonNode list = get(ANA, "/groups").expect(200).json();
    assertThat(list).hasSize(2);
    assertThat(values(list, "role")).containsExactlyInAnyOrder("OWNER", "VIEWER");
    assertThat(values(list, "id")).containsExactlyInAnyOrder(a, b);
  }

  @Test
  @DisplayName("R-002-05 other there is no limit on the number of groups")
  void R_002_05_other_noLimit() {
    for (int i = 0; i < 30; i++) {
      post(ANA, "/groups", j("name", "Skupina " + i)).expect(201);
    }
    assertThat(get(ANA, "/groups").expect(200).json()).hasSize(30);
  }

  @Test
  @DisplayName("R-002-06 other a group shows only its own contacts")
  void R_002_06_other_onlyOwnContacts() {
    String a = group(ANA, "A");
    String b = group(ANA, "B");
    newContact(ANA, a, "Ana", "VA");
    newContact(ANA, b, "Eva", "VB");
    assertThat(rowValues(search(ANA, a, ""), "lastName")).containsExactly("VA");
    assertThat(rowValues(search(ANA, b, ""), "lastName")).containsExactly("VB");
  }

  @Test
  @DisplayName("R-002-07 role non-members get not found on every group operation")
  void R_002_07_role_nonMemberNotFound() {
    String gid = group(ANA, "Zasebna");
    for (Res r :
        List.of(
            patch(BORIS, "/groups/" + gid, j("name", "x")),
            post(BORIS, "/groups/" + gid + "/trash", j("name", "Zasebna")),
            post(BORIS, "/groups/" + gid + "/leave", null),
            get(BORIS, "/groups/" + gid + "/members"),
            get(BORIS, "/groups/" + gid + "/invitations"))) {
      r.expectError(404, "NOT_FOUND");
    }
  }

  @Test
  @DisplayName(
      "R-002-08 combination rename is checked for every Owner and records old and new name")
  void R_002_08_combination_renameCheckedForEveryOwner() {
    String shared = group(ANA, "Skupna");
    group(BORIS, "Zasedeno");
    invite(ANA, shared, "boris@example.com", "OWNER").expect(201);
    get(BORIS, "/groups").expect(200);
    patch(ANA, "/groups/" + shared, j("name", "zasedeno")).expectError(409, "GROUP_NAME_TAKEN");
    patch(ANA, "/groups/" + shared, j("name", repeat("x", 101)))
        .expectFieldError("name", "TOO_LONG");
    patch(ANA, "/groups/" + shared, j("name", "Nova")).expect(200);
    JsonNode e = entries(ANA, shared, "GROUP_RENAMED").get(0);
    assertThat(e.toString()).contains("Skupna", "Nova");
  }

  @Test
  @DisplayName("R-003-01 other any of the three roles can be invited")
  void R_003_01_other_allThreeRoles() {
    String gid = group(ANA, "G");
    for (String role : List.of("OWNER", "EDITOR", "VIEWER")) {
      Res r = invite(ANA, gid, role.toLowerCase() + "@example.com", role).expect(201);
      assertThat(r.at("/role").asText()).isEqualTo(role);
    }
    invite(ANA, gid, "x@example.com", "ADMIN").expect(400);
    assertThat(get(ANA, "/groups/" + gid + "/invitations").expect(200).json()).hasSize(3);
  }

  @Test
  @DisplayName("R-003-02 invalid e-mail formats are refused")
  void R_003_02_invalid_formats() {
    String gid = group(ANA, "G");
    for (String bad :
        List.of(
            "a b@example.com",
            "@example.com",
            "ana@",
            "ana@example",
            "ana@.com",
            "ana@example..com",
            "ana@@example.com",
            "")) {
      invite(ANA, gid, bad, "VIEWER")
          .expectFieldError("email", bad.isEmpty() ? "REQUIRED" : "INVALID_FORMAT");
    }
    invite(ANA, gid, "a.b+c@sub.example.com", "VIEWER").expect(201);
    assertThat(get(ANA, "/groups/" + gid + "/invitations").expect(200).json()).hasSize(1);
  }

  @Test
  @DisplayName(
      "R-003-03 combination a former member can be invited, a pending address in other case is refused")
  void R_003_03_combination_caseInsensitive() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "VIEWER");
    invite(ANA, gid, "MARKO@EXAMPLE.COM", "EDITOR").expectError(409, "ALREADY_MEMBER");
    invite(ANA, gid, "Ivo@Example.com", "VIEWER").expect(201);
    invite(ANA, gid, "ivo@example.COM", "EDITOR").expectError(409, "INVITATION_PENDING");
  }

  @Test
  @DisplayName("R-003-04 boundary cancelling one pending invitation frees a place")
  void R_003_04_boundary_cancelFreesAPlace() {
    String gid = group(ANA, "G");
    String first = null;
    for (int i = 1; i <= 20; i++) {
      Res r = invite(ANA, gid, "q" + i + "@example.com", "VIEWER").expect(201);
      if (i == 1) {
        first = r.at("/id").asText();
      }
    }
    invite(ANA, gid, "q21@example.com", "VIEWER").expectError(409, "INVITATION_LIMIT");
    delete(ANA, "/groups/" + gid + "/invitations/" + first).expect(204);
    invite(ANA, gid, "q21@example.com", "VIEWER").expect(201);
    String other = group(ANA, "H");
    invite(ANA, other, "q1@example.com", "VIEWER").expect(201);
  }

  @Test
  @DisplayName(
      "R-003-05 combination a matching address in other case becomes a member with the invited role")
  void R_003_05_combination_otherCaseAddress() {
    String gid = group(ANA, "G");
    invite(ANA, gid, "Petra@EXAMPLE.com", "EDITOR").expect(201);
    JsonNode g = get(PETRA, "/groups").expect(200).json();
    assertThat(g).hasSize(1);
    assertThat(g.get(0).path("role").asText()).isEqualTo("EDITOR");
    assertThat(get(ANA, "/groups/" + gid + "/invitations").expect(200).json()).isEmpty();
    get(PETRA, "/groups").expect(200);
    assertThat(members(ANA, gid)).hasSize(2);
  }

  @Test
  @DisplayName("R-003-05 other a member keeps the existing role when invited again by promotion")
  void R_003_05_other_acceptedOnMeToo() {
    String gid = group(ANA, "G");
    invite(ANA, gid, "petra@example.com", "VIEWER").expect(201);
    get(PETRA, "/me").expect(200);
    assertThat(get(PETRA, "/groups/" + gid).expect(200).at("/role").asText()).isEqualTo("VIEWER");
  }

  @Test
  @DisplayName(
      "R-003-06 boundary an expired invitation is not listed and cannot be cancelled twice")
  void R_003_06_boundary_expiredNotListed() {
    String gid = group(ANA, "G");
    invite(ANA, gid, "petra@example.com", "VIEWER").expect(201);
    JsonNode listed = get(ANA, "/groups/" + gid + "/invitations").expect(200).json();
    assertThat(
            Duration.between(
                java.time.Instant.parse(listed.get(0).path("createdAt").asText()),
                java.time.Instant.parse(listed.get(0).path("expiresAt").asText())))
        .isEqualTo(Duration.ofDays(14));
    clock.advance(Duration.ofDays(14).plusSeconds(5));
    assertThat(get(ANA, "/groups/" + gid + "/invitations").expect(200).json()).isEmpty();
  }

  @Test
  @DisplayName(
      "R-003-07 other a cancelled invitation cannot be cancelled again and an unknown id is not found")
  void R_003_07_other_cancelTwice() {
    String gid = group(ANA, "G");
    String id = invite(ANA, gid, "petra@example.com", "VIEWER").expect(201).at("/id").asText();
    delete(ANA, "/groups/" + gid + "/invitations/" + id).expect(204);
    delete(ANA, "/groups/" + gid + "/invitations/" + id).expectError(404, "NOT_FOUND");
    delete(ANA, "/groups/" + gid + "/invitations/" + UUID.randomUUID())
        .expectError(404, "NOT_FOUND");
  }

  @Test
  @DisplayName("R-003-08 role only Owners list the invitations")
  void R_003_08_role_onlyOwners() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    join(gid, ANA, BORIS, "OWNER");
    get(MARKO, "/groups/" + gid + "/invitations").expectError(403, "FORBIDDEN");
    get(PETRA, "/groups/" + gid + "/invitations").expectError(403, "FORBIDDEN");
    get(BORIS, "/groups/" + gid + "/invitations").expect(200);
  }

  @Test
  @DisplayName(
      "R-003-09 other the system has no e-mail sender and the invitation answer has no link")
  void R_003_09_other_noMail() {
    String gid = group(ANA, "G");
    Res r = invite(ANA, gid, "petra@example.com", "VIEWER").expect(201);
    assertThat(r.text()).doesNotContain("http");
    assertThat(r.json().fieldNames())
        .toIterable()
        .containsExactlyInAnyOrder("id", "email", "role", "createdAt", "expiresAt");
  }

  @Test
  @DisplayName(
      "R-003-10 combination an unverified user matches nothing even after the invitation exists")
  void R_003_10_combination_unverifiedMatchesNothing() {
    String gid = group(ANA, "G");
    invite(ANA, gid, "ivan@example.com", "EDITOR").expect(201);
    get(IVAN, "/me").expectError(403, "EMAIL_NOT_VERIFIED");
    get(IVAN, "/groups/" + gid).expectError(403, "EMAIL_NOT_VERIFIED");
    assertThat(get(ANA, "/groups/" + gid + "/invitations").expect(200).json()).hasSize(1);
    assertThat(members(ANA, gid)).hasSize(1);
  }

  @Test
  @DisplayName("R-003-11 role Editor and Viewer cannot cancel an invitation")
  void R_003_11_role_cannotCancel() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    String id = invite(ANA, gid, "ivo@example.com", "VIEWER").expect(201).at("/id").asText();
    delete(MARKO, "/groups/" + gid + "/invitations/" + id).expectError(403, "FORBIDDEN");
    assertThat(get(ANA, "/groups/" + gid + "/invitations").expect(200).json()).hasSize(1);
  }

  @Test
  @DisplayName("R-004-01 other every role lists the members with name e-mail and role")
  void R_004_01_other_everyRoleLists() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    for (Who w : List.of(ANA, MARKO, PETRA)) {
      JsonNode m = members(w, gid);
      assertThat(m).hasSize(3);
      assertThat(values(m, "email"))
          .containsExactlyInAnyOrder(
              "ana.novak@example.com", "marko@example.com", "petra@example.com");
      assertThat(values(m, "name")).contains("Petra Kos");
    }
  }

  @Test
  @DisplayName("R-004-02 combination a role change applies to the very next request")
  void R_004_02_combination_appliesAtNextRequest() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "VIEWER");
    post(MARKO, "/groups/" + gid + "/contacts", j("lastName", "A")).expectError(403, "FORBIDDEN");
    setRole(ANA, gid, MARKO, "EDITOR").expect(200);
    post(MARKO, "/groups/" + gid + "/contacts", j("lastName", "A")).expect(201);
    setRole(ANA, gid, MARKO, "OWNER").expect(200);
    get(MARKO, "/groups/" + gid + "/invitations").expect(200);
    setRole(ANA, gid, MARKO, "ADMIN").expect(400);
    setRole(ANA, gid, PETRA, "VIEWER").expectError(404, "NOT_FOUND");
  }

  @Test
  @DisplayName("R-004-03 other several Owners are allowed")
  void R_004_03_other_severalOwners() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "OWNER");
    join(gid, ANA, BORIS, "OWNER");
    assertThat(values(members(ANA, gid), "role")).containsOnly("OWNER").hasSize(3);
  }

  @Test
  @DisplayName(
      "R-004-04 other a removed member leaves no membership and an unknown member is not found")
  void R_004_04_other_removalKeepsContactsAndNames() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    String c = newContact(MARKO, gid, "Ana", "Mojca").path("id").asText();
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expectError(404, "NOT_FOUND");
    assertThat(openContact(ANA, gid, c).path("lastName").asText()).isEqualTo("Mojca");
    assertThat(members(ANA, gid)).hasSize(1);
  }

  @Test
  @DisplayName("R-004-05 other every request of a removed member is not found")
  void R_004_05_other_everyRequestNotFound() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    String c = newContact(ANA, gid, "Ana", "Mojca").path("id").asText();
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    for (Res r :
        List.of(
            get(MARKO, "/groups/" + gid),
            get(MARKO, "/groups/" + gid + "/members"),
            get(MARKO, "/groups/" + gid + "/contacts/" + c),
            post(MARKO, "/groups/" + gid + "/contacts", j("lastName", "x")),
            get(MARKO, "/groups/" + gid + "/tags"),
            get(MARKO, "/groups/" + gid + "/history"))) {
      r.expectError(404, "NOT_FOUND");
    }
  }

  @Test
  @DisplayName("R-004-06 combination leaving by an Owner while another Owner exists")
  void R_004_06_combination_ownerLeaves() {
    String gid = group(ANA, "G");
    join(gid, ANA, BORIS, "OWNER");
    post(ANA, "/groups/" + gid + "/leave", null).expect(204);
    assertThat(members(BORIS, gid)).hasSize(1);
    post(ANA, "/groups/" + gid + "/leave", null).expectError(404, "NOT_FOUND");
    List<JsonNode> removed = entries(BORIS, gid, "MEMBER_REMOVED");
    assertThat(removed).hasSize(1);
    assertThat(removed.get(0).at("/actor/name").asText()).isEqualTo("Ana Novak");
  }

  @Test
  @DisplayName("R-004-07 boundary the last Owner among several owners and a demotion to Viewer")
  void R_004_07_boundary_demotionToViewer() {
    String gid = group(ANA, "G");
    join(gid, ANA, BORIS, "OWNER");
    setRole(ANA, gid, ANA, "VIEWER").expect(200);
    setRole(BORIS, gid, BORIS, "VIEWER").expectError(409, "LAST_OWNER");
    delete(BORIS, "/groups/" + gid + "/members/" + BORIS.uid()).expectError(409, "LAST_OWNER");
    post(BORIS, "/groups/" + gid + "/leave", null).expectError(409, "LAST_OWNER");
    assertThat(values(members(BORIS, gid), "role")).contains("OWNER");
  }

  @Test
  @DisplayName("R-004-08 role Editor and Viewer cannot remove members")
  void R_004_08_role_cannotRemove() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    delete(MARKO, "/groups/" + gid + "/members/" + PETRA.uid()).expectError(403, "FORBIDDEN");
    delete(PETRA, "/groups/" + gid + "/members/" + MARKO.uid()).expectError(403, "FORBIDDEN");
    assertThat(members(ANA, gid)).hasSize(3);
  }

  @Test
  @DisplayName("R-004-09 other no endpoint lists the groups or members of other people")
  void R_004_09_other_noAdministrator() {
    String gid = group(ANA, "G");
    for (String path :
        List.of(
            "/groups/" + gid + "/members",
            "/groups/" + gid,
            "/deleted-groups",
            "/groups/" + gid + "/history")) {
      Res r = get(BORIS, path);
      assertThat(r.text()).doesNotContain("ana.novak@example.com", "Ana Novak");
    }
    assertThat(get(BORIS, "/groups").expect(200).json()).isEmpty();
  }

  @Test
  @DisplayName("BR-01 combination a group in Trash counts as not existing for non-Owner members")
  void BR_01_combination_trashHidesGroup() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, BORIS, "OWNER");
    post(ANA, "/groups/" + gid + "/trash", j("name", "G")).expect(204);
    get(MARKO, "/groups/" + gid + "/members").expectError(404, "NOT_FOUND");
    assertThat(get(MARKO, "/groups").expect(200).json()).isEmpty();
    assertThat(get(BORIS, "/deleted-groups").expect(200).json()).hasSize(1);
  }

  @Test
  @DisplayName("BR-02 role the permission matrix for contact operations at each role")
  void BR_02_role_matrixOfContactOperations() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    String c = newContact(ANA, gid, "Ana", "Mojca").path("id").asText();
    for (Who w : List.of(ANA, MARKO, PETRA)) {
      get(w, "/groups/" + gid + "/contacts/" + c).expect(200);
      post(w, "/groups/" + gid + "/contacts/search", j()).expect(200);
      get(w, "/groups/" + gid + "/members").expect(200);
    }
    for (Who w : List.of(ANA, MARKO)) {
      get(w, "/groups/" + gid + "/history").expect(200);
      get(w, "/groups/" + gid + "/trash-contacts").expect(200);
      post(w, "/groups/" + gid + "/duplicates/search", null).expect(200);
      get(w, "/groups/" + gid + "/contacts/" + c + "/versions").expect(200);
    }
    for (Res r :
        List.of(
            get(PETRA, "/groups/" + gid + "/history"),
            get(PETRA, "/groups/" + gid + "/trash-contacts"),
            post(PETRA, "/groups/" + gid + "/duplicates/search", null),
            get(PETRA, "/groups/" + gid + "/contacts/" + c + "/versions"))) {
      r.expectError(403, "FORBIDDEN");
    }
    get(MARKO, "/groups/" + gid + "/invitations").expectError(403, "FORBIDDEN");
    delete(MARKO, "/groups/" + gid + "/contacts/" + c).expectError(403, "FORBIDDEN");
  }

  @Test
  @DisplayName("BR-03 combination an unverified user has no access whatever the path")
  void BR_03_combination_unverifiedNoAccess() {
    String gid = group(ANA, "G");
    String c = newContact(ANA, gid, "Ana", "Mojca").path("id").asText();
    Who unverified = ANA.unverified();
    for (Res r :
        List.of(
            get(unverified, "/groups"),
            get(unverified, "/groups/" + gid),
            get(unverified, "/groups/" + gid + "/contacts/" + c),
            post(unverified, "/groups/" + gid + "/contacts/search", j()),
            post(unverified, "/groups", j("name", "x")),
            get(unverified, "/me"))) {
      r.expectError(403, "EMAIL_NOT_VERIFIED");
    }
  }
}
