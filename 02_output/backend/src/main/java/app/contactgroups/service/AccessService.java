package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.GroupSummary;
import app.contactgroups.domain.Role;
import app.contactgroups.repository.GroupRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The access check of every group request (ADR-01, BR-01, SR-16). The role is read from the
 * database at every request. It holds the one place where the 404 is built so that a non-member, a
 * missing group and a group in Trash look the same (SR-03); a malformed id is turned into the same
 * 404 by the error mapping.
 */
@Service
public class AccessService {

  private final GroupRepository groups;

  public AccessService(GroupRepository groups) {
    this.groups = groups;
  }

  /** The one 404 of the API. */
  public static ApiException notFound() {
    return ApiException.notFound();
  }

  /** The 403 for a member whose role does not allow the operation. */
  public static ApiException forbidden() {
    return new ApiException(ErrorCode.FORBIDDEN, "Your role does not allow this.");
  }

  /** The group with the user's role; 404 for a non-member and for a group that is in Trash. */
  public GroupSummary requireMember(UUID groupId, String userId) {
    return groups.findActiveOfMember(groupId, userId).orElseThrow(AccessService::notFound);
  }

  /** Like {@link #requireMember}, and 403 unless the user is an Owner. */
  public GroupSummary requireOwner(UUID groupId, String userId) {
    return ownerOnly(requireMember(groupId, userId));
  }

  /**
   * Like {@link #requireOwner} and locks the group row until the end of the transaction, so that
   * the role is the one that holds while the change is made (ADR-03).
   */
  public GroupSummary requireOwnerLocked(UUID groupId, String userId) {
    return ownerOnly(
        groups.lockActiveOfMember(groupId, userId).orElseThrow(AccessService::notFound));
  }

  private static GroupSummary ownerOnly(GroupSummary group) {
    if (group.role() != Role.OWNER) {
      throw forbidden();
    }
    return group;
  }
}
