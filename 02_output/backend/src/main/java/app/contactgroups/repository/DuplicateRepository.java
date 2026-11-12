package app.contactgroups.repository;

import app.contactgroups.domain.DuplicateCandidate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Duplicate detection input and the {@code duplicate_dismissals} table (ADR-11). */
@Repository
public class DuplicateRepository {

  private final JdbcClient jdbc;

  public DuplicateRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The active contacts of the group with names, company, e-mail folded in SQL (BR-07). A contact
   * with no e-mail, first name, last name and company is left out (R-021-05).
   */
  public List<DuplicateCandidate> candidates(UUID groupId) {
    return jdbc.sql(
            "SELECT id, f_fold(coalesce(first_name, '')) AS first_name,"
                + " f_fold(coalesce(last_name, '')) AS last_name,"
                + " f_fold(coalesce(email, '')) AS email,"
                + " f_fold(coalesce(company, '')) AS company, coalesce(phone, '') AS phone"
                + " FROM contacts WHERE group_id = ? AND state = 'ACTIVE'"
                + " AND (coalesce(btrim(email), '') <> '' OR coalesce(btrim(first_name), '') <> ''"
                + " OR coalesce(btrim(last_name), '') <> ''"
                + " OR coalesce(btrim(company), '') <> '') ORDER BY created_at, id")
        .param(groupId)
        .query(
            (rs, n) ->
                new DuplicateCandidate(
                    rs.getObject("id", UUID.class),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("company"),
                    rs.getString("phone")))
        .list();
  }

  /** The creation time of each given contact. */
  public Map<UUID, Instant> createdAt(List<UUID> ids) {
    Map<UUID, Instant> out = new HashMap<>();
    jdbc.sql("SELECT id, created_at FROM contacts WHERE id = ANY (?)")
        .param(ids.toArray(UUID[]::new))
        .query(
            rs -> {
              out.put(rs.getObject("id", UUID.class), rs.getTimestamp("created_at").toInstant());
            });
    return out;
  }

  /** How many of the given ids are active contacts of the group. */
  public int countActive(UUID groupId, List<UUID> ids) {
    return jdbc.sql(
            "SELECT count(*) FROM contacts WHERE group_id = ? AND state = 'ACTIVE'"
                + " AND id = ANY (?)")
        .params(groupId, ids.toArray(UUID[]::new))
        .query(Integer.class)
        .single();
  }

  /** The keys of all dismissed sets of the group. */
  public Set<String> dismissedKeys(UUID groupId) {
    return new HashSet<>(
        jdbc.sql("SELECT set_key FROM duplicate_dismissals WHERE group_id = ?")
            .param(groupId)
            .query(String.class)
            .list());
  }

  /** Stores the mark; marking the same set twice changes nothing. */
  public void dismiss(
      UUID groupId, String setKey, List<UUID> contactIds, String userId, Instant now) {
    jdbc.sql(
            "INSERT INTO duplicate_dismissals (id, group_id, set_key, contact_ids, user_id,"
                + " created_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (group_id, set_key)"
                + " DO NOTHING")
        .params(
            UUID.randomUUID(),
            groupId,
            setKey,
            contactIds.toArray(UUID[]::new),
            userId,
            Timestamp.from(now))
        .update();
  }
}
