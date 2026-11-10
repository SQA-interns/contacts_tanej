package app.contactgroups.domain;

import java.util.UUID;

/**
 * A group as its member sees it whatever its state, for the two operations that may meet a group in
 * Trash: delete and restore (D-17).
 */
public record GroupAccess(UUID id, String name, Role role, boolean trashed) {

  public GroupSummary summary() {
    return new GroupSummary(id, name, role);
  }
}
