package app.contactgroups.repository;

import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.HistoryActor;
import app.contactgroups.domain.HistoryEntry;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.HistoryFilter;
import app.contactgroups.domain.Role;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
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

  /** Removes the e-mail address from the entries the user caused; the name stays (R-016-03). */
  public void removeActorEmail(String userId) {
    jdbc.sql("UPDATE history_entries SET actor_email = NULL WHERE actor_id = ?")
        .param(userId)
        .update();
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

  public int countByGroup(UUID groupId) {
    return count("group_id = ?", groupId);
  }

  /** The number of the group's entries that match the filter. */
  public int countByGroup(UUID groupId, HistoryFilter filter) {
    var where = Where.of(groupId, filter);
    return jdbc.sql("SELECT count(*) FROM history_entries WHERE " + where.sql())
        .params(where.args())
        .query(Integer.class)
        .single();
  }

  /** One page of the group's entries that match the filter, newest first. */
  public List<HistoryEntry> pageByGroup(UUID groupId, HistoryFilter filter, int limit, int offset) {
    var where = Where.of(groupId, filter);
    var args = new ArrayList<>(where.args());
    args.add(limit);
    args.add(offset);
    return jdbc
        .sql(
            "SELECT * FROM history_entries WHERE "
                + where.sql()
                + " ORDER BY seq DESC LIMIT ? OFFSET ?")
        .params(args)
        .query((rs, n) -> entry(rs))
        .list()
        .stream()
        .map(e -> e.withChanges(changesOf(e.id())))
        .toList();
  }

  /** The people with entries in the group, also those who left (EX-014-03), by name. */
  public List<HistoryActor> actors(UUID groupId) {
    return jdbc.sql(
            "SELECT actor_id, actor_name FROM (SELECT DISTINCT ON (actor_id) actor_id, actor_name"
                + " FROM history_entries WHERE group_id = ? AND actor_id IS NOT NULL"
                + " ORDER BY actor_id, seq DESC) a ORDER BY lower(actor_name), actor_id")
        .param(groupId)
        .query((rs, n) -> new HistoryActor(rs.getString("actor_id"), rs.getString("actor_name")))
        .list();
  }

  /** The WHERE clause and its arguments for a group and a filter. */
  private record Where(String sql, List<Object> args) {
    static Where of(UUID groupId, HistoryFilter f) {
      var sql = new StringBuilder("group_id = ?");
      var args = new ArrayList<Object>();
      args.add(groupId);
      if (f.actorId() != null) {
        sql.append(" AND actor_id = ?");
        args.add(f.actorId());
      }
      if (!f.types().isEmpty()) {
        sql.append(" AND event_type IN (")
            .append(String.join(", ", Collections.nCopies(f.types().size(), "?")))
            .append(')');
        f.types().forEach(t -> args.add(t.name()));
      }
      if (f.start() != null) {
        sql.append(" AND created_at >= ?");
        args.add(Timestamp.from(f.start()));
      }
      if (f.endExclusive() != null) {
        sql.append(" AND created_at < ?");
        args.add(Timestamp.from(f.endExclusive()));
      }
      return new Where(sql.toString(), args);
    }
  }

  public int countByContact(UUID contactId) {
    return count("contact_id = ?", contactId);
  }

  /** One page of the group's entries, newest first, with their changes. */
  public List<HistoryEntry> pageByGroup(UUID groupId, int limit, int offset) {
    return page("group_id = ?", groupId, limit, offset);
  }

  /** One page of the entries about one contact, newest first, with their changes. */
  public List<HistoryEntry> pageByContact(UUID contactId, int limit, int offset) {
    return page("contact_id = ?", contactId, limit, offset);
  }

  private int count(String where, UUID id) {
    return jdbc.sql("SELECT count(*) FROM history_entries WHERE " + where)
        .param(id)
        .query(Integer.class)
        .single();
  }

  private List<HistoryEntry> page(String where, UUID id, int limit, int offset) {
    return jdbc
        .sql("SELECT * FROM history_entries WHERE " + where + " ORDER BY seq DESC LIMIT ? OFFSET ?")
        .params(id, limit, offset)
        .query((rs, n) -> entry(rs))
        .list()
        .stream()
        .map(e -> e.withChanges(changesOf(e.id())))
        .toList();
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
