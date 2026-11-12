package app.contactgroups.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The {@code contact_merges} table: merged-away contact to survivor (R-022-06, ADR-05). */
@Repository
public class MergeRepository {

  private final JdbcClient jdbc;

  public MergeRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Links a merged-away contact to its survivor. */
  public void link(UUID mergedId, UUID survivorId, Instant now) {
    jdbc.sql("INSERT INTO contact_merges (merged_id, survivor_id, merged_at) VALUES (?, ?, ?)")
        .params(mergedId, survivorId, Timestamp.from(now))
        .update();
  }
}
