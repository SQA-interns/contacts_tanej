package app.contactgroups.repository;

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
