package app.contactgroups.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * NFR-13: the schema comes only from versioned, forward-only Flyway migrations. The migrations run on
 * an empty database and on a database left by an earlier release (version 1, holding data), and the
 * migration list has no gaps, no repeatable and no undo scripts.
 */
@Testcontainers
class MigrationIT {

  private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

  /** A new, empty database inside the container, so each test starts clean. */
  private static String newDatabase(String name) {
    new JdbcTemplate(source(POSTGRES.getDatabaseName())).execute("CREATE DATABASE " + name);
    return name;
  }

  private static DriverManagerDataSource source(String database) {
    String url =
        "jdbc:postgresql://%s:%d/%s"
            .formatted(POSTGRES.getHost(), POSTGRES.getMappedPort(5432), database);
    return new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static Flyway flyway(String database, String target) {
    return Flyway.configure()
        .dataSource(source(database))
        .locations("classpath:db/migration")
        .target(target)
        .load();
  }

  @Test
  void migratesAnEmptyDatabaseToTheLatestVersion() {
    Flyway flyway = flyway(newDatabase("empty_db"), "latest");
    flyway.migrate();

    MigrationInfo[] all = flyway.info().all();
    assertThat(all).isNotEmpty();
    assertThat(all).allSatisfy(m -> assertThat(m.getState().isApplied()).isTrue());
    assertThat(flyway.info().pending()).isEmpty();
  }

  @Test
  void migratesTheDatabaseOfAnEarlierReleaseAndKeepsItsData() {
    String database = newDatabase("earlier_release");
    Flyway first = flyway(database, "1");
    first.migrate();
    assertThat(first.info().current().getVersion().getVersion()).isEqualTo("1");

    JdbcTemplate jdbc = new JdbcTemplate(source(database));
    jdbc.update(
        "INSERT INTO users (id, name, email, created_at) VALUES ('u1', 'Ana Novak', 'ana@example.com', now())");
    jdbc.update(
        "INSERT INTO groups (id, name, state, created_at, created_by) VALUES "
            + "('00000000-0000-0000-0000-000000000001', 'Konferenca', 'ACTIVE', now(), 'u1')");
    jdbc.update(
        "INSERT INTO history_entries (id, group_id, change_set_id, event_type, actor_name, created_at) VALUES "
            + "('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000001', "
            + "'00000000-0000-0000-0000-0000000000b1', 'GROUP_CREATED', 'Ana Novak', now())");

    Flyway latest = flyway(database, "latest");
    var result = latest.migrate();

    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isGreaterThan(0);
    assertThat(latest.info().pending()).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM history_entries", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT name FROM groups", String.class)).isEqualTo("Konferenca");
  }

  @Test
  void aSecondRunChangesNothing() {
    Flyway flyway = flyway(newDatabase("second_run"), "latest");
    flyway.migrate();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
  }

  @Test
  void theMigrationListIsVersionedForwardOnlyAndWithoutGaps() throws IOException {
    List<String> names;
    try (Stream<Path> files = Files.list(MIGRATIONS)) {
      names = files.map(p -> p.getFileName().toString()).sorted().toList();
    }
    assertThat(names).isNotEmpty();
    assertThat(names).allMatch(n -> n.matches("V\\d+__[a-z0-9_]+\\.sql"));
    List<Integer> versions =
        names.stream().map(n -> Integer.parseInt(n.substring(1, n.indexOf("__")))).sorted().toList();
    for (int i = 0; i < versions.size(); i++) {
      assertThat(versions.get(i)).as("version at position %d", i + 1).isEqualTo(i + 1);
    }
  }
}
