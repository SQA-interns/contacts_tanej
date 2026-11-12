package app.contactgroups.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A History entry as the API shows it (contract HistoryEntry); null fields are left out. */
public record HistoryEntryView(
    UUID id,
    UUID changeSetId,
    HistoryEventType eventType,
    UUID contactId,
    String contactLabel,
    Actor actor,
    String subjectName,
    Role role,
    Instant createdAt,
    boolean erased,
    List<Change> changes,
    UUID linkedFrom) {

  /** The same entry flagged as belonging to a merged-away contact (R-022-06). */
  public HistoryEntryView linkedFrom(UUID mergedId) {
    return new HistoryEntryView(
        id,
        changeSetId,
        eventType,
        contactId,
        contactLabel,
        actor,
        subjectName,
        role,
        createdAt,
        erased,
        changes,
        mergedId);
  }

  /** Who made the change; the e-mail is absent after the account deletion (D-16). */
  public record Actor(String id, String name, String email) {}

  /** One changed field; an old or new value that never existed reads empty, not missing. */
  public record Change(String field, String oldValue, String newValue) {}

  /** The view of a stored entry. Values of an erased contact are absent (BR-06). */
  public static HistoryEntryView of(HistoryEntry e) {
    List<Change> changes =
        e.changes().stream()
            .map(
                c ->
                    e.erased()
                        ? new Change(c.field(), null, null)
                        : new Change(c.field(), orEmpty(c.oldValue()), orEmpty(c.newValue())))
            .toList();
    return new HistoryEntryView(
        e.id(),
        e.changeSetId(),
        e.type(),
        e.contactId(),
        e.contactLabel(),
        new Actor(e.actorId(), e.actorName(), e.actorEmail()),
        e.subjectName(),
        e.role(),
        e.createdAt(),
        e.erased(),
        changes,
        null);
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
