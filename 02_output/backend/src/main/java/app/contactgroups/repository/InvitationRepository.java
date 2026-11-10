package app.contactgroups.repository;

import app.contactgroups.domain.Role;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The {@code invitations} table (ADR-03). A row is pending while its {@code created_at} is later
 * than the cut-off the caller passes (now minus 14 days, ADR-08); expired rows are ignored here and
 * erased by the daily cleanup.
 */
@Repository
public class InvitationRepository {

  /** A pending invitation row. */
  public record Row(UUID id, UUID groupId, String email, Role role, Instant createdAt) {}

  private static final String COLUMNS = "id, group_id, email, role, created_at";

  private final JdbcClient jdbc;

  public InvitationRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(Row row, String createdBy) {
    jdbc.sql(
            "INSERT INTO invitations (id, group_id, email, role, created_at, created_by)"
                + " VALUES (?, ?, ?, ?, ?, ?)")
        .params(
            row.id(),
            row.groupId(),
            row.email(),
            row.role().name(),
            Timestamp.from(row.createdAt()),
            createdBy)
        .update();
  }

  /** The pending invitations of the group, oldest first. */
  public List<Row> findPending(UUID groupId, Instant cutoff) {
    return jdbc.sql(
            "SELECT "
                + COLUMNS
                + " FROM invitations WHERE group_id = ? AND created_at > ?"
                + " ORDER BY created_at, id")
        .params(groupId, Timestamp.from(cutoff))
        .query((rs, n) -> row(rs.getObject("id", UUID.class), rs))
        .list();
  }

  public int countPending(UUID groupId, Instant cutoff) {
    return jdbc.sql("SELECT count(*) FROM invitations WHERE group_id = ? AND created_at > ?")
        .params(groupId, Timestamp.from(cutoff))
        .query(Integer.class)
        .single();
  }

  public boolean existsPending(UUID groupId, String email, Instant cutoff) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM invitations WHERE group_id = ? AND email = ?"
                + " AND created_at > ?)")
        .params(groupId, email, Timestamp.from(cutoff))
        .query(Boolean.class)
        .single();
  }

  /** Erases an expired row of the same address so that the unique index lets a new one in. */
  public void deleteExpired(UUID groupId, String email, Instant cutoff) {
    jdbc.sql("DELETE FROM invitations WHERE group_id = ? AND email = ? AND created_at <= ?")
        .params(groupId, email, Timestamp.from(cutoff))
        .update();
  }

  /**
   * Removes the invitation of the group and returns its role; empty when it does not exist
   * (R-003-07).
   */
  public Optional<Role> delete(UUID groupId, UUID invitationId) {
    return jdbc.sql("DELETE FROM invitations WHERE group_id = ? AND id = ? RETURNING role")
        .params(groupId, invitationId)
        .query((rs, n) -> Role.valueOf(rs.getString("role")))
        .optional();
  }

  /** Removes the invitation, whatever its group, once it has been accepted. */
  public void deleteById(UUID invitationId) {
    jdbc.sql("DELETE FROM invitations WHERE id = ?").param(invitationId).update();
  }

  /** Locks and returns the pending invitations of an address in all groups (R-003-05). */
  public List<Row> lockPendingForEmail(String email, Instant cutoff) {
    return jdbc.sql(
            "SELECT "
                + COLUMNS
                + " FROM invitations WHERE email = ? AND created_at > ?"
                + " ORDER BY created_at, id FOR UPDATE")
        .params(email, Timestamp.from(cutoff))
        .query((rs, n) -> row(rs.getObject("id", UUID.class), rs))
        .list();
  }

  private static Row row(UUID id, java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Row(
        id,
        rs.getObject("group_id", UUID.class),
        rs.getString("email"),
        Role.valueOf(rs.getString("role")),
        rs.getTimestamp("created_at").toInstant());
  }
}
