package app.contactgroups.repository;

import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.HistoryEntry;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The {@code history_entries} and {@code history_changes} tables (ADR-05): insert and read. */
@Repository
public class HistoryRepository {

  private final JdbcClient jdbc;

  public HistoryRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Appends the entry and its changes. */
  public void insert(HistoryEntry e) {
    jdbc.sql(
            "INSERT INTO history_entries (id, group_id, contact_id, contact_label, change_set_id,"
                + " event_type, actor_id, actor_name, actor_email, subject_name, role, erased,"
                + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            e.id(),
            e.groupId(),
            e.contactId(),
            e.contactLabel(),
            e.changeSetId(),
            e.type().name(),
            e.actorId(),
            e.actorName(),
            e.actorEmail(),
            e.subjectName(),
            e.role() == null ? null : e.role().name(),
            e.erased(),
            Timestamp.from(e.createdAt()))
        .update();
    for (FieldChange c : e.changes()) {
      jdbc.sql(
              "INSERT INTO history_changes (entry_id, field_name, old_value, new_value)"
                  + " VALUES (?, ?, ?, ?)")
          .params(e.id(), c.field(), c.oldValue(), c.newValue())
          .update();
    }
  }

  /** Marks the entries of the contact erased and removes its label and values (BR-06). */
  public void anonymiseContact(UUID contactId) {
    jdbc.sql(
            "UPDATE history_changes SET old_value = NULL, new_value = NULL WHERE entry_id IN"
                + " (SELECT id FROM history_entries WHERE contact_id = ?)")
        .param(contactId)
        .update();
    jdbc.sql("UPDATE history_entries SET erased = true, contact_label = NULL WHERE contact_id = ?")
        .param(contactId)
        .update();
  }

  /** The entries of one group, newest first, with their changes in the order they were written. */
  public List<HistoryEntry> findByGroup(UUID groupId) {
    return jdbc
        .sql("SELECT * FROM history_entries WHERE group_id = ? ORDER BY created_at DESC, id")
        .param(groupId)
        .query((rs, n) -> entry(rs))
        .list()
        .stream()
        .map(e -> e.withChanges(changesOf(e.id())))
        .toList();
  }

  private List<FieldChange> changesOf(UUID entryId) {
    return jdbc.sql(
            "SELECT field_name, old_value, new_value FROM history_changes WHERE entry_id = ?"
                + " ORDER BY id")
        .param(entryId)
        .query(
            (rs, n) ->
                new FieldChange(
                    rs.getString("field_name"),
                    rs.getString("old_value"),
                    rs.getString("new_value")))
        .list();
  }

  private static HistoryEntry entry(ResultSet rs) throws SQLException {
    String role = rs.getString("role");
    return new HistoryEntry(
        rs.getObject("id", UUID.class),
        rs.getObject("group_id", UUID.class),
        rs.getObject("contact_id", UUID.class),
        rs.getString("contact_label"),
        rs.getObject("change_set_id", UUID.class),
        HistoryEventType.valueOf(rs.getString("event_type")),
        rs.getString("actor_id"),
        rs.getString("actor_name"),
        rs.getString("actor_email"),
        rs.getString("subject_name"),
        role == null ? null : Role.valueOf(role),
        rs.getBoolean("erased"),
        rs.getTimestamp("created_at").toInstant(),
        List.of());
  }
}
