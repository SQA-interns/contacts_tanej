package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.EmailAddress;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.Invitation;
import app.contactgroups.domain.Role;
import app.contactgroups.repository.InvitationRepository;
import app.contactgroups.repository.InvitationRepository.Row;
import app.contactgroups.repository.MembershipRepository;
import app.contactgroups.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create, list and cancel invitations (Owner only) and accept them on the first request of the
 * invited person (US-003). An invitation is pending for 14 days; the product sends no e-mail.
 */
@Service
public class InvitationService {

  public static final Duration VALIDITY = Duration.ofDays(14);
  public static final int MAX_PENDING = 20;

  private final InvitationRepository invitations;
  private final MembershipRepository memberships;
  private final UserRepository users;
  private final AccessService access;
  private final Clock clock;

  public InvitationService(
      InvitationRepository invitations,
      MembershipRepository memberships,
      UserRepository users,
      AccessService access,
      Clock clock) {
    this.invitations = invitations;
    this.memberships = memberships;
    this.users = users;
    this.access = access;
    this.clock = clock;
  }

  /** The pending invitations of the group, Owner only (R-003-08, R-003-11). */
  @Transactional(readOnly = true)
  public List<Invitation> list(UUID groupId, String userId) {
    access.requireOwner(groupId, userId);
    return invitations.findPending(groupId, cutoff()).stream()
        .map(InvitationService::toInvitation)
        .toList();
  }

  /** A pending invitation for the address and role (R-003-01 to R-003-04, R-003-11). */
  @Transactional
  public Invitation create(UUID groupId, String userId, String rawEmail, String rawRole) {
    access.requireOwnerLocked(groupId, userId);
    String email = EmailAddress.of(rawEmail);
    Role role = Role.parse(rawRole);
    Instant now = clock.instant();
    Instant cutoff = now.minus(VALIDITY);
    if (memberships.hasMemberWithEmail(groupId, email)) {
      throw conflict(ErrorCode.ALREADY_MEMBER, "This person is already a member of the group.");
    }
    if (invitations.existsPending(groupId, email, cutoff)) {
      throw conflict(ErrorCode.INVITATION_PENDING, "An invitation for this address is pending.");
    }
    if (invitations.countPending(groupId, cutoff) >= MAX_PENDING) {
      throw conflict(
          ErrorCode.INVITATION_LIMIT,
          "The limit of " + MAX_PENDING + " pending invitations is reached.");
    }
    invitations.deleteExpired(groupId, email, cutoff);
    Row row = new Row(UUID.randomUUID(), groupId, email, role, now);
    invitations.insert(row, userId);
    return toInvitation(row);
  }

  /** Removes a pending invitation, Owner only (R-003-07, R-003-11). */
  @Transactional
  public void cancel(UUID groupId, String userId, UUID invitationId) {
    access.requireOwnerLocked(groupId, userId);
    if (!invitations.delete(groupId, invitationId)) {
      throw AccessService.notFound();
    }
  }

  /**
   * Makes the user a member of every group with a pending invitation for the user's e-mail address
   * and deletes those invitations (R-003-05, D-20 reading 4). A user who is already a member keeps
   * the existing role. Only called for a verified e-mail address (R-003-10, the verified-e-mail
   * filter stops other requests earlier).
   */
  @Transactional
  public void acceptFor(String userId) {
    var user = users.find(userId);
    if (user.isEmpty() || user.get().email().isEmpty()) {
      return;
    }
    Instant now = clock.instant();
    for (Row row : invitations.lockPendingForEmail(user.get().email(), now.minus(VALIDITY))) {
      memberships.insertIfAbsent(row.groupId(), userId, row.role(), now);
      invitations.deleteById(row.id());
    }
  }

  private Instant cutoff() {
    return clock.instant().minus(VALIDITY);
  }

  private static Invitation toInvitation(Row row) {
    return new Invitation(
        row.id(), row.email(), row.role(), row.createdAt(), row.createdAt().plus(VALIDITY));
  }

  private static ApiException conflict(ErrorCode code, String message) {
    return new ApiException(code, message);
  }
}
