package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.DeletedGroup;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.FieldProblem;
import app.contactgroups.domain.GroupAccess;
import app.contactgroups.domain.GroupName;
import app.contactgroups.domain.GroupSummary;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.repository.GroupRepository;
import app.contactgroups.repository.MembershipRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create, list, open and rename a group, with the name rule per Owner (US-002, R-002-01 to
 * R-002-08). The name rule is checked under the advisory locks of ADR-03.
 */
@Service
public class GroupService {

  /** The group to leave out of the name check when the group does not exist yet. */
  private static final UUID NO_GROUP = new UUID(0, 0);

  private final GroupRepository groups;
  private final MembershipRepository memberships;
  private final AccessService access;
  private final HistoryRecorder history;
  private final Clock clock;

  public GroupService(
      GroupRepository groups,
      MembershipRepository memberships,
      AccessService access,
      HistoryRecorder history,
      Clock clock) {
    this.groups = groups;
    this.memberships = memberships;
    this.access = access;
    this.history = history;
    this.clock = clock;
  }

  /** The user becomes the Owner of a new group (R-002-01). No History entry (D-22). */
  @Transactional
  public GroupSummary create(String userId, String rawName) {
    String name = GroupName.of(rawName);
    groups.lockOwnerNames(userId);
    if (groups.ownerHasName(userId, name, NO_GROUP)) {
      throw nameTaken();
    }
    UUID id = UUID.randomUUID();
    var now = clock.instant();
    groups.insert(id, name, userId, now);
    memberships.insert(id, userId, Role.OWNER, now);
    return new GroupSummary(id, name, Role.OWNER);
  }

  /** The groups of the user, not in Trash, with the user's role (R-002-04). */
  @Transactional(readOnly = true)
  public List<GroupSummary> listFor(String userId) {
    return groups.findActiveOfMember(userId);
  }

  /** One group; 404 unless the user is a member (R-002-07). */
  @Transactional(readOnly = true)
  public GroupSummary get(UUID groupId, String userId) {
    return access.requireMember(groupId, userId);
  }

  /**
   * An Owner gives the group a new name (R-002-08). The rule of R-002-03 is checked for every Owner
   * of the group, the group itself not counting. The same name again changes nothing: it is no
   * rename, so no rule is applied and no entry is written.
   */
  @Transactional
  public GroupSummary rename(UUID groupId, String userId, String rawName) {
    GroupSummary group = access.requireOwnerLocked(groupId, userId);
    String name = GroupName.of(rawName);
    if (name.equals(group.name())) {
      return group;
    }
    requireNameFreeForOwners(groupId, name);
    groups.rename(groupId, name);
    history.record(
        ChangeSet.of(
            groupId,
            userId,
            ChangeSet.Event.group(
                HistoryEventType.GROUP_RENAMED,
                List.of(new FieldChange("name", group.name(), name)))));
    return group.withName(name);
  }

  /**
   * An Owner moves the group to Trash after typing its exact name (R-015-01, R-015-02, EX-015-04).
   * A non-Owner gets 403 for an active group and 404 for one in Trash (R-015-03, R-015-07); an
   * Owner who meets a group already in Trash gets 409 (EX-015-05).
   */
  @Transactional
  public void trash(UUID groupId, String userId, String typedName) {
    GroupAccess group = lockAny(groupId, userId);
    if (group.trashed()) {
      throw group.role() == Role.OWNER ? groupInTrash() : AccessService.notFound();
    }
    if (group.role() != Role.OWNER) {
      throw AccessService.forbidden();
    }
    if (!group.name().equals(typedName == null ? "" : typedName.strip())) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          "The request is not valid.",
          List.of(new FieldProblem("name", "NAME_MISMATCH", "The name does not match.")));
    }
    groups.trash(groupId, clock.instant());
    recordEvent(groupId, userId, HistoryEventType.GROUP_DELETED);
  }

  /**
   * An Owner brings a group in Trash back (R-015-05). Everyone else, and a group not in Trash, get
   * 404 (D-17). The name rule applies for every Owner of the group (R-015-10).
   */
  @Transactional
  public void restore(UUID groupId, String userId) {
    GroupAccess group = lockAny(groupId, userId);
    if (!group.trashed() || group.role() != Role.OWNER) {
      throw AccessService.notFound();
    }
    requireNameFreeForOwners(groupId, group.name());
    groups.restore(groupId);
    recordEvent(groupId, userId, HistoryEventType.GROUP_RESTORED);
  }

  /** The groups in Trash of which the user is an Owner (R-015-04). */
  @Transactional(readOnly = true)
  public List<DeletedGroup> deletedFor(String userId) {
    return groups.findTrashedOfOwner(userId, clock.instant());
  }

  /**
   * Deletes the group with everything in it if it is still in Trash since {@code cutoff} or
   * earlier; a group restored or purged meanwhile is left alone (R-015-06).
   */
  @Transactional
  public boolean purgeExpired(UUID groupId, Instant cutoff) {
    if (groups.lockExpired(groupId, cutoff).isEmpty()) {
      return false;
    }
    groups.delete(groupId);
    return true;
  }

  private GroupAccess lockAny(UUID groupId, String userId) {
    return groups.lockAnyOfMember(groupId, userId).orElseThrow(AccessService::notFound);
  }

  private void requireNameFreeForOwners(UUID groupId, String name) {
    List<String> owners = memberships.ownerIds(groupId);
    owners.forEach(groups::lockOwnerNames);
    for (String owner : owners) {
      if (groups.ownerHasName(owner, name, groupId)) {
        throw nameTaken();
      }
    }
  }

  private void recordEvent(UUID groupId, String userId, HistoryEventType type) {
    history.record(ChangeSet.of(groupId, userId, ChangeSet.Event.group(type, List.of())));
  }

  private static ApiException groupInTrash() {
    return new ApiException(ErrorCode.GROUP_IN_TRASH, "The group is in Trash.");
  }

  private static ApiException nameTaken() {
    return new ApiException(ErrorCode.GROUP_NAME_TAKEN, "You already have a group with this name.");
  }
}
