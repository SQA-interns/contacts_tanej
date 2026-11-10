package app.contactgroups.repository;

import app.contactgroups.domain.Contact.Attribute;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The {@code attributes} table: free key-value pairs of one contact (US-008). */
@Repository
public class AttributeRepository {

  private final JdbcClient jdbc;

  public AttributeRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** The attributes of a contact in the order of the key, ignoring case. */
  public List<Attribute> findByContact(UUID contactId) {
    return jdbc.sql(
            "SELECT key, value FROM attributes WHERE contact_id = ? ORDER BY lower(key), key")
        .param(contactId)
        .query((rs, i) -> new Attribute(rs.getString("key"), rs.getString("value")))
        .list();
  }

  /** Replaces all attributes of the contact; the caller holds the row lock of the contact. */
  public void replace(UUID contactId, List<Attribute> attributes) {
    jdbc.sql("DELETE FROM attributes WHERE contact_id = ?").param(contactId).update();
    for (Attribute a : attributes) {
      jdbc.sql("INSERT INTO attributes (id, contact_id, key, value) VALUES (?, ?, ?, ?)")
          .params(UUID.randomUUID(), contactId, a.key(), a.value())
          .update();
    }
  }

  /**
   * The keys used by the contacts of the group that contain {@code part}, ignoring case and
   * accents.
   */
  public List<String> keysOfGroup(UUID groupId, String part) {
    return jdbc.sql(
            "SELECT min(a.key) AS key FROM attributes a JOIN contacts c ON c.id = a.contact_id"
                + " WHERE c.group_id = ? AND f_fold(a.key) LIKE '%' || f_fold(?) || '%' ESCAPE '\\'"
                + " GROUP BY lower(a.key) ORDER BY lower(min(a.key)), min(a.key)")
        .params(groupId, like(part))
        .query(String.class)
        .list();
  }

  private static String like(String part) {
    String text = part == null ? "" : part.strip();
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
