package app.contactgroups.domain;

import java.util.List;
import java.util.UUID;

/**
 * The entries of one save, which share one id and are shown together (R-013-07, glossary "Save
 * group", D-23). A change set is built by the service that makes the change and written by {@code
 * HistoryRecorder} in the same transaction.
 */
public record ChangeSet(UUID id, UUID groupId, String actorId, List<Event> events) {

  public ChangeSet {
    events = List.copyOf(events);
  }

  /** One entry to write: its type, the contact or member it is about and the changed fields. */
  public record Event(
      HistoryEventType type,
      UUID contactId,
      String contactLabel,
      String subjectName,
      Role role,
      List<FieldChange> changes) {

    public Event {
      changes = List.copyOf(changes);
    }

    /** An entry about the group itself (rename, delete, restore, export). */
    public static Event group(HistoryEventType type, List<FieldChange> changes) {
      return new Event(type, null, null, null, null, changes);
    }

    /** An entry about one contact. */
    public static Event contact(
        HistoryEventType type, UUID contactId, String label, List<FieldChange> changes) {
      return new Event(type, contactId, label, null, null, changes);
    }

    /** An entry about a member, by name only (D-16). */
    public static Event member(
        HistoryEventType type, String subjectName, Role role, List<FieldChange> changes) {
      return new Event(type, null, null, subjectName, role, changes);
    }
  }

  /** A change set with a new id. */
  public static ChangeSet of(UUID groupId, String actorId, Event... events) {
    return new ChangeSet(UUID.randomUUID(), groupId, actorId, List.of(events));
  }
}
