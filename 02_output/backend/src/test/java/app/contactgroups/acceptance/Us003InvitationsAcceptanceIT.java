package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.IVAN;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** US-003 Invite people to a group: AC-003-01 to AC-003-10, EX-003-01 to EX-003-08. */
class Us003InvitationsAcceptanceIT extends AcceptanceBase {

  private Res invite(Who owner, String gid, String email, String role) {
    return post(owner, "/groups/" + gid + "/invitations", j("email", email, "role", role));
  }

  private JsonNode pending(Who owner, String gid) {
    return get(owner, "/groups/" + gid + "/invitations").expect(200).json();
  }

  private JsonNode groupsOf(Who who) {
    return get(who, "/groups").expect(200).json();
  }

  @Test
  void AC_003_01_ownerInvitesTwoPeopleAndSeesThemPendingWithRoleAndExpiry() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "marko@example.com", "EDITOR").expect(201);
    invite(ANA, gid, "petra@example.com", "VIEWER").expect(201);
    JsonNode list = pending(ANA, gid);
    assertThat(list).hasSize(2);
    for (JsonNode i : list) {
      assertThat(i.path("expiresAt").asText()).isNotBlank();
    }
    assertThat(values(list, "email"))
        .containsExactlyInAnyOrder("marko@example.com", "petra@example.com");
    assertThat(values(list, "role")).containsExactlyInAnyOrder("EDITOR", "VIEWER");
    assertThatThrownBy(() -> Class.forName("jakarta.mail.Session"))
        .as("the system has no means to send e-mail")
        .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void AC_003_02_aPendingInvitationBecomesMembershipWhenTheInvitedPersonSignsIn() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "marko@example.com", "EDITOR").expect(201);
    JsonNode groups = groupsOf(MARKO);
    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).path("name").asText()).isEqualTo("Konferenca 2026");
    assertThat(groups.get(0).path("role").asText()).isEqualTo("EDITOR");
    assertThat(pending(ANA, gid)).isEmpty();
  }

  @Test
  void AC_003_03_invitingAMemberIgnoringCaseIsRefused() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    invite(ANA, gid, "Marko@Example.com", "EDITOR").expectError(409, "ALREADY_MEMBER");
    assertThat(pending(ANA, gid)).isEmpty();
  }

  @Test
  void AC_003_04_invitingAgainWhileOnePendingIsRefused() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "petra@example.com", "VIEWER").expect(201);
    invite(ANA, gid, "petra@example.com", "VIEWER").expectError(409, "INVITATION_PENDING");
    invite(ANA, gid, "PETRA@example.com", "EDITOR").expectError(409, "INVITATION_PENDING");
    assertThat(pending(ANA, gid)).hasSize(1);
  }

  @Test
  void AC_003_05_the21stPendingInvitationIsRefused() {
    String gid = group(ANA, "Konferenca 2026");
    for (int i = 1; i <= 20; i++) {
      invite(ANA, gid, "p" + i + "@example.com", "VIEWER").expect(201);
    }
    invite(ANA, gid, "p21@example.com", "VIEWER").expectError(409, "INVITATION_LIMIT");
    assertThat(pending(ANA, gid)).hasSize(20);
  }

  @Test
  void AC_003_06_anInvitation14DaysAnd1MinuteOldGivesNoMembershipAndDoesNotCount() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "petra@example.com", "VIEWER").expect(201);
    for (int i = 1; i <= 19; i++) {
      invite(ANA, gid, "p" + i + "@example.com", "VIEWER").expect(201);
    }
    clock.advance(Duration.ofDays(14).plusMinutes(1));
    assertThat(groupsOf(PETRA)).isEmpty();
    // all 20 are expired, so none counts towards the limit of 20
    for (int i = 1; i <= 20; i++) {
      invite(ANA, gid, "q" + i + "@example.com", "VIEWER").expect(201);
    }
  }

  @Test
  void AC_003_07_aCancelledInvitationGivesNoGroup() {
    String gid = group(ANA, "Konferenca 2026");
    String id = invite(ANA, gid, "petra@example.com", "VIEWER").expect(201).at("/id").asText();
    delete(ANA, "/groups/" + gid + "/invitations/" + id).expect(204);
    assertThat(pending(ANA, gid)).isEmpty();
    assertThat(groupsOf(PETRA)).isEmpty();
  }

  @Test
  void AC_003_08_anUnverifiedPersonMatchesNoInvitationUntilVerified() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "ivan@example.com", "VIEWER").expect(201);
    get(IVAN, "/groups").expectError(403, "EMAIL_NOT_VERIFIED");
    assertThat(pending(ANA, gid)).hasSize(1);
    Who verified = new Who(IVAN.uid(), IVAN.email(), IVAN.name(), true);
    assertThat(groupsOf(verified)).hasSize(1);
  }

  @Test
  void AC_003_09_anEditorCannotInvite() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    invite(MARKO, gid, "petra@example.com", "VIEWER").expectError(403, "FORBIDDEN");
    assertThat(pending(ANA, gid)).isEmpty();
  }

  @Test
  void AC_003_10_anInvalidEmailIsRefusedAndNothingIsCreated() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "marko@@example", "EDITOR").expectFieldError("email", "INVALID_FORMAT");
    assertThat(pending(ANA, gid)).isEmpty();
  }

  @Test
  void EX_003_01_theTwentiethPendingInvitationIsAccepted() {
    String gid = group(ANA, "Konferenca 2026");
    for (int i = 1; i <= 19; i++) {
      invite(ANA, gid, "p" + i + "@example.com", "VIEWER").expect(201);
    }
    invite(ANA, gid, "p20@example.com", "VIEWER").expect(201);
    assertThat(pending(ANA, gid)).hasSize(20);
  }

  @Test
  void EX_003_02_anExpiredInvitationDoesNotCountTowardsTheTwenty() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "old@example.com", "VIEWER").expect(201);
    clock.advance(Duration.ofDays(1));
    for (int i = 1; i <= 19; i++) {
      invite(ANA, gid, "p" + i + "@example.com", "VIEWER").expect(201);
    }
    clock.advance(Duration.ofDays(13).plusHours(1));
    // "old" is 14 days 1 hour old and expired; 19 are pending
    invite(ANA, gid, "new@example.com", "VIEWER").expect(201);
    invite(ANA, gid, "one-too-many@example.com", "VIEWER").expectError(409, "INVITATION_LIMIT");
  }

  @Test
  void EX_003_03_13Days23HoursAfterCreationTheMembershipIsGranted() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "petra@example.com", "VIEWER").expect(201);
    clock.advance(Duration.ofDays(13).plusHours(23));
    assertThat(groupsOf(PETRA)).hasSize(1);
  }

  @Test
  void EX_003_04_14DaysAfterCreationGiveNoMembership() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "petra@example.com", "VIEWER").expect(201);
    clock.advance(Duration.ofDays(14));
    assertThat(groupsOf(PETRA)).isEmpty();
  }

  @Test
  void EX_003_05_anInvitationForAPersonWithoutAnAccountStaysPendingUntilTheyAppear() {
    String gid = group(ANA, "Konferenca 2026");
    invite(ANA, gid, "nova@example.com", "EDITOR").expect(201);
    clock.advance(Duration.ofDays(5));
    assertThat(pending(ANA, gid)).hasSize(1);
    Who nova = new Who("uid-nova", "nova@example.com", "Nova Oseba", true);
    assertThat(groupsOf(nova)).hasSize(1);
  }

  @Test
  void EX_003_06_aRemovedMemberCanBeInvitedAgain() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    invite(ANA, gid, "marko@example.com", "VIEWER").expect(201);
  }

  @Test
  void EX_003_07_aPersonInvitedToTwoGroupsSeesBoth() {
    String a = group(ANA, "Konferenca 2026");
    String b = group(BORIS, "Poroka");
    invite(ANA, a, "petra@example.com", "VIEWER").expect(201);
    invite(BORIS, b, "petra@example.com", "EDITOR").expect(201);
    assertThat(groupsOf(PETRA)).hasSize(2);
  }

  @Test
  void EX_003_08_anOwnerInvitingTheirOwnAddressIsRefusedAsAlreadyMember() {
    String gid = group(ANA, "Konferenca 2026");
    get(ANA, "/me").expect(200);
    invite(ANA, gid, "ana.novak@example.com", "EDITOR").expectError(409, "ALREADY_MEMBER");
  }
}
