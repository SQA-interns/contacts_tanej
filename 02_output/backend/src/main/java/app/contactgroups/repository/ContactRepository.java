package app.contactgroups.repository;

import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactInput;
import app.contactgroups.domain.ContactState;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The {@code contacts} table (ADR-03). Empty text is stored as NULL and read back as "". */
@Repository
public class ContactRepository {

  private static final String SELECT = "SELECT * FROM contacts WHERE id = ? AND group_id = ?";

  private final JdbcClient jdbc;

  public ContactRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Inserts a new active contact with version 1 and revision 1. */
  public void insert(UUID id, UUID groupId, ContactInput f, String userId, Instant now) {
    jdbc.sql(
            "INSERT INTO contacts (id, group_id, first_name, last_name, email, phone, company,"
                + " job_title, street, postal_code, city, country, notes, state, created_at,"
                + " created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)")
        .params(
            id,
            groupId,
            nul(f.firstName()),
            nul(f.lastName()),
            nul(f.email()),
            nul(f.phone()),
            nul(f.company()),
            nul(f.jobTitle()),
            nul(f.street()),
            nul(f.postalCode()),
            nul(f.city()),
            nul(f.country()),
            nul(f.notes()),
            Timestamp.from(now),
            userId)
        .update();
  }

  public Optional<Contact> find(UUID groupId, UUID contactId) {
    return jdbc.sql(SELECT).params(contactId, groupId).query((rs, n) -> contact(rs)).optional();
  }

  /** Like {@link #find} and locks the row until the end of the transaction (ADR-03). */
  public Optional<Contact> lock(UUID groupId, UUID contactId) {
    return jdbc.sql(SELECT + " FOR UPDATE")
        .params(contactId, groupId)
        .query((rs, n) -> contact(rs))
        .optional();
  }

  /** Writes the fields and moves the counters: revision +1 and the new version number. */
  public void update(UUID contactId, ContactInput f, int versionNumber) {
    jdbc.sql(
            "UPDATE contacts SET first_name = ?, last_name = ?, email = ?, phone = ?, company = ?,"
                + " job_title = ?, street = ?, postal_code = ?, city = ?, country = ?, notes = ?,"
                + " current_version = ?, revision = revision + 1 WHERE id = ?")
        .params(
            nul(f.firstName()),
            nul(f.lastName()),
            nul(f.email()),
            nul(f.phone()),
            nul(f.company()),
            nul(f.jobTitle()),
            nul(f.street()),
            nul(f.postalCode()),
            nul(f.city()),
            nul(f.country()),
            nul(f.notes()),
            versionNumber,
            contactId)
        .update();
  }

  private static String nul(String value) {
    return value.isEmpty() ? null : value;
  }

  private static Contact contact(ResultSet rs) throws SQLException {
    Timestamp trashed = rs.getTimestamp("trashed_at");
    return new Contact(
        rs.getObject("id", UUID.class),
        rs.getObject("group_id", UUID.class),
        ContactState.valueOf(rs.getString("state")),
        trashed == null ? null : trashed.toInstant(),
        rs.getTimestamp("created_at").toInstant(),
        rs.getLong("revision"),
        rs.getInt("current_version"),
        new ContactInput(
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("company"),
            rs.getString("job_title"),
            rs.getString("street"),
            rs.getString("postal_code"),
            rs.getString("city"),
            rs.getString("country"),
            rs.getString("notes")),
        List.of(),
        List.of());
  }
}
