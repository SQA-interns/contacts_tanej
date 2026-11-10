package app.contactgroups.repository;

import app.contactgroups.domain.User;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The {@code users} and {@code deleted_accounts} tables (ADR-02, ADR-03). */
@Repository
public class UserRepository {

  private final JdbcClient jdbc;

  public UserRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Creates the user or brings name and e-mail up to date with the token. */
  public void upsert(User user, Instant now) {
    jdbc.sql(
            "INSERT INTO users (id, name, email, created_at) VALUES (?, ?, ?, ?)"
                + " ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email")
        .params(user.id(), user.name(), user.email(), Timestamp.from(now))
        .update();
  }

  public Optional<User> find(String userId) {
    return jdbc.sql("SELECT id, name, email FROM users WHERE id = ?")
        .param(userId)
        .query(User.class)
        .optional();
  }

  /** True when the account was deleted, so its old token must be refused (R-016-06). */
  public boolean isDeleted(String userId) {
    return jdbc.sql("SELECT EXISTS (SELECT 1 FROM deleted_accounts WHERE user_id = ?)")
        .param(userId)
        .query(Boolean.class)
        .single();
  }
}
