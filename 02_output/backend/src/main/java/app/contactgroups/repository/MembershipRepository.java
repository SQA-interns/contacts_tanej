package app.contactgroups.repository;

import app.contactgroups.domain.Role;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The {@code memberships} table (ADR-03). */
@Repository
public class MembershipRepository {

  private final JdbcClient jdbc;

  public MembershipRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(UUID groupId, String userId, Role role, Instant now) {
    jdbc.sql("INSERT INTO memberships (group_id, user_id, role, created_at) VALUES (?, ?, ?, ?)")
        .params(groupId, userId, role.name(), Timestamp.from(now))
        .update();
  }

  /** Adds the membership unless the user is already a member, whose role then stays (D-20). */
  public void insertIfAbsent(UUID groupId, String userId, Role role, Instant now) {
    jdbc.sql(
            "INSERT INTO memberships (group_id, user_id, role, created_at) VALUES (?, ?, ?, ?)"
                + " ON CONFLICT (group_id, user_id) DO NOTHING")
        .params(groupId, userId, role.name(), Timestamp.from(now))
        .update();
  }

  /** True when a member of the group has this (lowercase) e-mail address (R-003-03). */
  public boolean hasMemberWithEmail(UUID groupId, String email) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM memberships m JOIN users u ON u.id = m.user_id"
                + " WHERE m.group_id = ? AND lower(u.email) = ?)")
        .params(groupId, email)
        .query(Boolean.class)
        .single();
  }

  /** The ids of the Owners of the group, sorted: the order in which their locks are taken. */
  public List<String> ownerIds(UUID groupId) {
    return jdbc.sql(
            "SELECT user_id FROM memberships WHERE group_id = ? AND role = 'OWNER'"
                + " ORDER BY user_id")
        .param(groupId)
        .query(String.class)
        .list();
  }
}
