package app.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** A group in Trash as its Owner sees it, with the days left (contract {@code DeletedGroup}). */
public record DeletedGroup(UUID id, String name, Instant trashedAt, int daysLeft) {

  /** The group with the days left counted by the Trash rule of BR-04 (R-015-04). */
  public static DeletedGroup of(UUID id, String name, Instant trashedAt, Instant now) {
    return new DeletedGroup(id, name, trashedAt, TrashedContact.daysLeft(trashedAt, now));
  }
}
