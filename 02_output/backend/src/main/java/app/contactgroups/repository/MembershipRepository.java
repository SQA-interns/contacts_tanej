package app.contactgroups.repository;

import app.contactgroups.domain.Member;
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

/** The {@code memberships} table (ADR-03). */
@Repository
public class MembershipRepository {

  private static final String MEMBER_SELECT =
      "SELECT m.user_id, u.name, u.email, m.role FROM memberships m"
          + " JOIN users u ON u.id = m.user_id";

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

  /** The members of the group, by name and then id (R-004-01). */
  public List<Member> findMembers(UUID groupId) {
    return jdbc.sql(MEMBER_SELECT + " WHERE m.group_id = ? ORDER BY lower(u.name), u.id")
        .param(groupId)
        .query((rs, n) -> member(rs))
        .list();
  }

  public Optional<Member> findMember(UUID groupId, String userId) {
    return jdbc.sql(MEMBER_SELECT + " WHERE m.group_id = ? AND m.user_id = ?")
        .params(groupId, userId)
        .query((rs, n) -> member(rs))
        .optional();
  }

  public void updateRole(UUID groupId, String userId, Role role) {
    jdbc.sql("UPDATE memberships SET role = ? WHERE group_id = ? AND user_id = ?")
        .params(role.name(), groupId, userId)
        .update();
  }

  public void delete(UUID groupId, String userId) {
    jdbc.sql("DELETE FROM memberships WHERE group_id = ? AND user_id = ?")
        .params(groupId, userId)
        .update();
  }

  public int countOwners(UUID groupId) {
    return jdbc.sql("SELECT count(*) FROM memberships WHERE group_id = ? AND role = 'OWNER'")
        .param(groupId)
        .query(Integer.class)
        .single();
  }

  private static Member member(ResultSet rs) throws SQLException {
    return new Member(
        rs.getString("user_id"),
        rs.getString("name"),
        rs.getString("email"),
        Role.valueOf(rs.getString("role")));
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
