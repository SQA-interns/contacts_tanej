package app.contactgroups.repository;

import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactInput;
import app.contactgroups.domain.ContactState;
import app.contactgroups.domain.TrashedContact;
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

  /**
   * The contacts of the group that carry the tag, in Trash too, locked in id order until the end of
   * the transaction so that no save changes them meanwhile (R-007-01, D-19).
   */
  public List<Contact> lockWithTag(UUID tagId) {
    return jdbc.sql(
            "SELECT c.* FROM contacts c JOIN contact_tags ct ON ct.contact_id = c.id"
                + " WHERE ct.tag_id = ? ORDER BY c.id FOR UPDATE OF c")
        .param(tagId)
        .query((rs, n) -> contact(rs))
        .list();
  }

  /** A change without a version still counts as a change of the contact (D-19, R-007-06). */
  public void touch(List<UUID> contactIds) {
    jdbc.sql("UPDATE contacts SET revision = revision + 1 WHERE id = ANY (?)")
        .param(contactIds.toArray(UUID[]::new))
        .update();
  }

  /** Moves the contact to Trash; the revision grows so that an open edit sees a change. */
  public void trash(UUID contactId, Instant now) {
    jdbc.sql(
            "UPDATE contacts SET state = 'TRASHED', trashed_at = ?, revision = revision + 1"
                + " WHERE id = ?")
        .params(Timestamp.from(now), contactId)
        .update();
  }

  /** Brings the contact back to the list; no version is recorded (EX-019-02). */
  public void restore(UUID contactId) {
    jdbc.sql(
            "UPDATE contacts SET state = 'ACTIVE', trashed_at = NULL, revision = revision + 1"
                + " WHERE id = ?")
        .param(contactId)
        .update();
  }

  /** Deletes the row; versions, tag links, attributes and merge links go by cascade. */
  public void delete(UUID contactId) {
    jdbc.sql("DELETE FROM contacts WHERE id = ?").param(contactId).update();
  }

  /** The number of contacts of the group in Trash. */
  public int countTrashed(UUID groupId) {
    return jdbc.sql("SELECT count(*) FROM contacts WHERE group_id = ? AND state = 'TRASHED'")
        .param(groupId)
        .query(Integer.class)
        .single();
  }

  /** One page of the Trash view, the longest in Trash first. */
  public List<TrashedContact> pageTrashed(UUID groupId, int limit, int offset, Instant now) {
    return jdbc.sql(
            "SELECT id, first_name, last_name, email, company, trashed_at FROM contacts"
                + " WHERE group_id = ? AND state = 'TRASHED'"
                + " ORDER BY trashed_at, id LIMIT ? OFFSET ?")
        .params(groupId, limit, offset)
        .query(
            (rs, n) -> {
              Instant trashedAt = rs.getTimestamp("trashed_at").toInstant();
              return new TrashedContact(
                  rs.getObject("id", UUID.class),
                  orEmpty(rs.getString("first_name")),
                  orEmpty(rs.getString("last_name")),
                  orEmpty(rs.getString("email")),
                  orEmpty(rs.getString("company")),
                  trashedAt,
                  TrashedContact.daysLeft(trashedAt, now));
            })
        .list();
  }

  /** Ids (contact, group) of the contacts in Trash since {@code cutoff} or earlier (BR-04). */
  public List<UUID[]> expired(Instant cutoff) {
    return jdbc.sql(
            "SELECT id, group_id FROM contacts WHERE state = 'TRASHED' AND trashed_at <= ?"
                + " ORDER BY trashed_at, id")
        .param(Timestamp.from(cutoff))
        .query(
            (rs, n) ->
                new UUID[] {rs.getObject("id", UUID.class), rs.getObject("group_id", UUID.class)})
        .list();
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
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
