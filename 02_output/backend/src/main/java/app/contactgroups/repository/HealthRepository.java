package app.contactgroups.repository;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.stereotype.Repository;

/** Asks the database whether it answers (R-017-12). */
@Repository
public class HealthRepository {

  private static final int TIMEOUT_SECONDS = 2;

  private final DataSource dataSource;

  public HealthRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** True when a connection is available and the database answers within two seconds. */
  public boolean databaseWorks() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(TIMEOUT_SECONDS);
    } catch (SQLException e) {
      return false;
    }
  }
}
