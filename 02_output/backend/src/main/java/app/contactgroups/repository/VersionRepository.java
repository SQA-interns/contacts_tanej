package app.contactgroups.repository;

import app.contactgroups.domain.ContactVersion;
import java.sql.Timestamp;
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

  public int count(UUID contactId) {
    return jdbc.sql("SELECT count(*) FROM contact_versions WHERE contact_id = ?")
        .param(contactId)
        .query(Integer.class)
        .single();
  }
}
