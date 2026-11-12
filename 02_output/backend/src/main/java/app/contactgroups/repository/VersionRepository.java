package app.contactgroups.repository;

import app.contactgroups.domain.ContactVersion;
import app.contactgroups.domain.VersionSummary;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The {@code contact_versions} table (ADR-05): versions are only ever added. */
@Repository
public class VersionRepository {

  private final JdbcClient jdbc;

  public VersionRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(ContactVersion v) {
    jdbc.sql(
            "INSERT INTO contact_versions (contact_id, number, snapshot, user_id, user_name,"
                + " created_at) VALUES (?, ?, ?::jsonb, ?, ?, ?)")
        .params(
            v.contactId(),
            v.number(),
            v.snapshot(),
            v.userId(),
            v.userName(),
            Timestamp.from(v.createdAt()))
        .update();
  }

  /** The versions of a contact, newest first. */
  public List<VersionSummary> list(UUID contactId) {
    return jdbc.sql(
            "SELECT number, user_name, created_at FROM contact_versions WHERE contact_id = ?"
                + " ORDER BY number DESC")
        .param(contactId)
        .query(
            (rs, row) ->
                new VersionSummary(
                    rs.getInt("number"),
                    rs.getString("user_name"),
                    rs.getTimestamp("created_at").toInstant()))
        .list();
  }

  /** One version of the contact with its snapshot, if it exists. */
  public Optional<ContactVersion> find(UUID contactId, int number) {
    return jdbc.sql(
            "SELECT number, snapshot::text AS snapshot, user_id, user_name, created_at FROM"
                + " contact_versions WHERE contact_id = ? AND number = ?")
        .params(contactId, number)
        .query(
            (rs, row) ->
                new ContactVersion(
                    contactId,
                    rs.getInt("number"),
                    rs.getString("snapshot"),
                    rs.getString("user_id"),
                    rs.getString("user_name"),
                    rs.getTimestamp("created_at").toInstant()))
        .optional();
  }

  public int count(UUID contactId) {
    return jdbc.sql("SELECT count(*) FROM contact_versions WHERE contact_id = ?")
        .param(contactId)
        .query(Integer.class)
        .single();
  }
}
