package app.contactgroups.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A stored History entry (ADR-05). Contact and subject fields are null where they do not apply. */
public record HistoryEntry(
    UUID id,
    UUID groupId,
    UUID contactId,
    String contactLabel,
    UUID changeSetId,
    HistoryEventType type,
    String actorId,
    String actorName,
    String actorEmail,
    String subjectName,
    Role role,
    boolean erased,
    Instant createdAt,
    List<FieldChange> changes) {

  public HistoryEntry withChanges(List<FieldChange> newChanges) {
    return new HistoryEntry(
        id,
        groupId,
        contactId,
        contactLabel,
        changeSetId,
        type,
        actorId,
        actorName,
        actorEmail,
        subjectName,
        role,
        erased,
        createdAt,
        newChanges);
  }
}
