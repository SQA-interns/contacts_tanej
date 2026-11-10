package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.Member;
import app.contactgroups.domain.Role;
import app.contactgroups.repository.MembershipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * List members, change roles, remove members and leave (US-004). Every change runs under the group
 * row lock and reads the actor's role again after it, so two parallel requests are serialised and
 * the group always keeps an Owner (R-004-07, ADR-03).
 */
@Service
public class MemberService {

  private final MembershipRepository memberships;
  private final AccessService access;

  public MemberService(MembershipRepository memberships, AccessService access) {
    this.memberships = memberships;
    this.access = access;
  }

  /** The members of the group, for every member (R-004-01). */
  @Transactional(readOnly = true)
  public List<Member> list(UUID groupId, String userId) {
    access.requireMember(groupId, userId);
    return memberships.findMembers(groupId);
  }

  /** Sets a member's role, Owner only; the same role is a no-op (R-004-02, R-004-08). */
  @Transactional
  public Member changeRole(UUID groupId, String actorId, String targetId, String rawRole) {
    requireOwnerUnderLock(groupId, actorId);
    Role role = Role.parse(rawRole);
    Member target = memberships.findMember(groupId, targetId).orElseThrow(AccessService::notFound);
    if (target.role() == role) {
      return target;
    }
    if (target.role() == Role.OWNER) {
      requireAnotherOwner(groupId);
    }
    memberships.updateRole(groupId, targetId, role);
    return new Member(target.userId(), target.name(), target.email(), role);
  }

  /** Removes a member, Owner only; contacts and History stay (R-004-04, R-004-08). */
  @Transactional
  public void remove(UUID groupId, String actorId, String targetId) {
    requireOwnerUnderLock(groupId, actorId);
    Member target = memberships.findMember(groupId, targetId).orElseThrow(AccessService::notFound);
    delete(groupId, target);
  }

  /** The member leaves: a removal of oneself by oneself (R-004-06). */
  @Transactional
  public void leave(UUID groupId, String userId) {
    access.requireMemberLocked(groupId, userId);
    Member self = memberships.findMember(groupId, userId).orElseThrow(AccessService::notFound);
    delete(groupId, self);
  }

  private void delete(UUID groupId, Member target) {
    if (target.role() == Role.OWNER) {
      requireAnotherOwner(groupId);
    }
    memberships.delete(groupId, target.userId());
  }

  /** Locks the group and checks the Owner role as it is now, not as it was before the wait. */
  private void requireOwnerUnderLock(UUID groupId, String actorId) {
    access.requireMemberLocked(groupId, actorId);
    Member actor = memberships.findMember(groupId, actorId).orElseThrow(AccessService::notFound);
    if (actor.role() != Role.OWNER) {
      throw AccessService.forbidden();
    }
  }

  /** The change takes an Owner away: refuse it when no other Owner would be left (R-004-07). */
  private void requireAnotherOwner(UUID groupId) {
    if (memberships.countOwners(groupId) < 2) {
      throw new ApiException(
          ErrorCode.LAST_OWNER, "A group needs at least one Owner. Make someone else Owner first.");
    }
  }
}
