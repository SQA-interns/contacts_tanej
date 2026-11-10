package app.contactgroups.repository;

import app.contactgroups.domain.DeletedGroup;
import app.contactgroups.domain.GroupAccess;
import app.contactgroups.domain.GroupSummary;
import app.contactgroups.domain.Role;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The {@code groups} table (ADR-03). Every read of a group goes through the membership of the
 * caller, so a non-member finds nothing (AR-02, SR-02).
 */
@Repository
public class GroupRepository {

  private static final String ACTIVE_OF_MEMBER =
      "SELECT g.id, g.name, m.role FROM groups g JOIN memberships m ON m.group_id = g.id"
          + " WHERE m.user_id = ? AND g.state = 'ACTIVE'";

  private final JdbcClient jdbc;

  public GroupRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(UUID id, String name, String creatorId, Instant now) {
    jdbc.sql(
            "INSERT INTO groups (id, name, state, created_at, created_by)"
                + " VALUES (?, ?, 'ACTIVE', ?, ?)")
        .params(id, name, Timestamp.from(now), creatorId)
        .update();
  }

  /** The groups of the user that are not in Trash, with the user's role, in Slovenian order. */
  public List<GroupSummary> findActiveOfMember(String userId) {
    return jdbc.sql(
            ACTIVE_OF_MEMBER + " ORDER BY g.name COLLATE \"sl-SI-x-icu\", g.created_at, g.id")
        .param(userId)
        .query((rs, n) -> summary(rs))
        .list();
  }

  /** The group with the user's role, only when the user is a member and it is not in Trash. */
  public Optional<GroupSummary> findActiveOfMember(UUID groupId, String userId) {
    return jdbc.sql(ACTIVE_OF_MEMBER + " AND g.id = ?")
        .params(userId, groupId)
        .query((rs, n) -> summary(rs))
        .optional();
  }

  /**
   * Like {@link #findActiveOfMember(UUID, String)} and locks the group row until the end of the
   * transaction (ADR-03), so that membership and name changes of the group are serialised.
   */
  public Optional<GroupSummary> lockActiveOfMember(UUID groupId, String userId) {
    return jdbc.sql(ACTIVE_OF_MEMBER + " AND g.id = ? FOR UPDATE OF g")
        .params(userId, groupId)
        .query((rs, n) -> summary(rs))
        .optional();
  }

  /**
   * The group in any state with the user's role, only when the user is a member, and locks its row
   * until the end of the transaction. Only delete and restore may see a group in Trash (D-17).
   */
  public Optional<GroupAccess> lockAnyOfMember(UUID groupId, String userId) {
    return jdbc.sql(
            "SELECT g.id, g.name, g.state, m.role FROM groups g JOIN memberships m"
                + " ON m.group_id = g.id WHERE m.user_id = ? AND g.id = ? FOR UPDATE OF g")
        .params(userId, groupId)
        .query(
            (rs, n) ->
                new GroupAccess(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    Role.valueOf(rs.getString("role")),
                    "TRASHED".equals(rs.getString("state"))))
        .optional();
  }

  public void trash(UUID groupId, Instant now) {
    jdbc.sql("UPDATE groups SET state = 'TRASHED', trashed_at = ? WHERE id = ?")
        .params(Timestamp.from(now), groupId)
        .update();
  }

  public void restore(UUID groupId) {
    jdbc.sql("UPDATE groups SET state = 'ACTIVE', trashed_at = NULL WHERE id = ?")
        .param(groupId)
        .update();
  }

  /** The groups in Trash of which the user is an Owner, oldest deletion first (R-015-04). */
  public List<DeletedGroup> findTrashedOfOwner(String userId, Instant now) {
    return jdbc.sql(
            "SELECT g.id, g.name, g.trashed_at FROM groups g JOIN memberships m"
                + " ON m.group_id = g.id WHERE m.user_id = ? AND m.role = 'OWNER'"
                + " AND g.state = 'TRASHED' ORDER BY g.trashed_at, g.id")
        .param(userId)
        .query(
            (rs, n) ->
                DeletedGroup.of(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    rs.getTimestamp("trashed_at").toInstant(),
                    now))
        .list();
  }

  /** The ids of the groups in Trash since {@code cutoff} or earlier (R-015-06, EX-015-02). */
  public List<UUID> expired(Instant cutoff) {
    return jdbc.sql(
            "SELECT id FROM groups WHERE state = 'TRASHED' AND trashed_at <= ?"
                + " ORDER BY trashed_at, id")
        .param(Timestamp.from(cutoff))
        .query(UUID.class)
        .list();
  }

  /** Locks the group if it is in Trash since {@code cutoff} or earlier; empty otherwise. */
  public Optional<UUID> lockExpired(UUID groupId, Instant cutoff) {
    return jdbc.sql(
            "SELECT id FROM groups WHERE id = ? AND state = 'TRASHED' AND trashed_at <= ?"
                + " FOR UPDATE")
        .params(groupId, Timestamp.from(cutoff))
        .query(UUID.class)
        .optional();
  }

  /** Deletes the group; the database cascades to everything that belongs to it (ADR-08). */
  public void delete(UUID groupId) {
    jdbc.sql("DELETE FROM groups WHERE id = ?").param(groupId).update();
  }

  public void rename(UUID groupId, String name) {
    jdbc.sql("UPDATE groups SET name = ? WHERE id = ?").params(name, groupId).update();
  }

  /** Waits for, and holds until the end of the transaction, the name-rule lock of one Owner. */
  public void lockOwnerNames(String ownerId) {
    jdbc.sql("SELECT pg_advisory_xact_lock(hashtext(?))").param(ownerId).query().singleRow();
  }

  /**
   * True when the user owns a group other than {@code exceptGroupId}, not in Trash, whose name
   * equals the given one ignoring case (R-002-03).
   */
  public boolean ownerHasName(String ownerId, String name, UUID exceptGroupId) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM groups g JOIN memberships m ON m.group_id = g.id"
                + " WHERE m.user_id = ? AND m.role = 'OWNER' AND g.state = 'ACTIVE'"
                + " AND lower(g.name) = lower(?) AND g.id <> ?)")
        .params(ownerId, name, exceptGroupId)
        .query(Boolean.class)
        .single();
  }

  private static GroupSummary summary(ResultSet rs) throws SQLException {
    return new GroupSummary(
        rs.getObject("id", UUID.class), rs.getString("name"), Role.valueOf(rs.getString("role")));
  }
}
