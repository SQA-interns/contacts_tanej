package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.GroupName;
import app.contactgroups.domain.GroupSummary;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.repository.GroupRepository;
import app.contactgroups.repository.MembershipRepository;
import java.time.Clock;
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
    List<String> owners = memberships.ownerIds(groupId);
    owners.forEach(groups::lockOwnerNames);
    for (String owner : owners) {
      if (groups.ownerHasName(owner, name, groupId)) {
        throw nameTaken();
      }
    }
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

  private static ApiException nameTaken() {
    return new ApiException(ErrorCode.GROUP_NAME_TAKEN, "You already have a group with this name.");
  }
}
