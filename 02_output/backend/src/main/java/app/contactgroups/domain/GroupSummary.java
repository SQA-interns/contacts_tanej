package app.contactgroups.domain;

import java.util.UUID;

/**
 * A group as one user sees it: id, name and the user's role in it (contract {@code GroupSummary}).
 */
public record GroupSummary(UUID id, String name, Role role) {

  public GroupSummary withName(String newName) {
    return new GroupSummary(id, newName, role);
  }
}
