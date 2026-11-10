package app.contactgroups.repository;

import app.contactgroups.domain.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The {@code tags} and {@code contact_tags} tables. A tag exists only while a contact carries it
 * (R-006-07); a name is unique per group ignoring case (BR-07).
 */
@Repository
public class TagRepository {

  private final JdbcClient jdbc;

  public TagRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Serialises the tag changes of one group until the end of the transaction. */
  public void lockGroupTags(UUID groupId) {
    jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(?, 1)) IS NOT NULL")
        .param("tags:" + groupId)
        .query(Boolean.class)
        .single();
  }

  /** The tag names of a contact, ignoring case in the order of the name. */
  public List<String> namesOf(UUID contactId) {
    return jdbc.sql(
            "SELECT t.name FROM contact_tags ct JOIN tags t ON t.id = ct.tag_id"
                + " WHERE ct.contact_id = ? ORDER BY lower(t.name), t.name")
        .param(contactId)
        .query(String.class)
        .list();
  }

  /** The tags of the group whose name contains {@code part}, ignoring case and accents. */
  public List<Tag> findByGroup(UUID groupId, String part) {
    return jdbc.sql(
            "SELECT t.id, t.name, count(ct.contact_id) AS n FROM tags t"
                + " JOIN contact_tags ct ON ct.tag_id = t.id"
                + " WHERE t.group_id = ? AND f_fold(t.name) LIKE '%' || f_fold(?) || '%' ESCAPE '\\'"
                + " GROUP BY t.id, t.name ORDER BY lower(t.name), t.name")
        .params(groupId, likeFolded(part))
        .query(
            (rs, i) ->
                new Tag(rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("n")))
        .list();
  }

  /**
   * Puts the tag of this name on the contact, creating it with the spelling given when the group
   * has none; returns the stored spelling (R-006-01, R-006-02).
   */
  public String attach(UUID groupId, UUID contactId, String name) {
    jdbc.sql(
            "INSERT INTO tags (id, group_id, name) VALUES (?, ?, ?)"
                + " ON CONFLICT (group_id, lower(name)) DO NOTHING")
        .params(UUID.randomUUID(), groupId, name)
        .update();
    var tag =
        jdbc.sql("SELECT id, name FROM tags WHERE group_id = ? AND lower(name) = lower(?)")
            .params(groupId, name)
            .query((rs, i) -> new String[] {rs.getString("id"), rs.getString("name")})
            .single();
    jdbc.sql("INSERT INTO contact_tags (contact_id, tag_id) VALUES (?, ?::uuid)")
        .params(contactId, tag[0])
        .update();
    return tag[1];
  }

  /** Takes the tag off this contact only and drops it from the group when no contact has it. */
  public void detach(UUID groupId, UUID contactId, String name) {
    jdbc.sql(
            "DELETE FROM contact_tags WHERE contact_id = ? AND tag_id IN"
                + " (SELECT id FROM tags WHERE group_id = ? AND lower(name) = lower(?))")
        .params(contactId, groupId, name)
        .update();
    jdbc.sql(
            "DELETE FROM tags t WHERE t.group_id = ? AND lower(t.name) = lower(?)"
                + " AND NOT EXISTS (SELECT 1 FROM contact_tags ct WHERE ct.tag_id = t.id)")
        .params(groupId, name)
        .update();
  }

  /** Drops the tags of the group that no contact carries any more (EX-012-08). */
  public void deleteUnused(UUID groupId) {
    jdbc.sql(
            "DELETE FROM tags t WHERE t.group_id = ?"
                + " AND NOT EXISTS (SELECT 1 FROM contact_tags ct WHERE ct.tag_id = t.id)")
        .param(groupId)
        .update();
  }

  /** The tag of the group, if it exists (a tag no contact carries does not exist, R-006-07). */
  public Optional<Tag> find(UUID groupId, UUID tagId) {
    return jdbc.sql(
            "SELECT t.id, t.name, (SELECT count(*) FROM contact_tags ct WHERE ct.tag_id = t.id) AS n"
                + " FROM tags t WHERE t.id = ? AND t.group_id = ?")
        .params(tagId, groupId)
        .query(
            (rs, i) ->
                new Tag(rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("n")))
        .optional();
  }

  /** True when another tag of the group has this name ignoring case (BR-07). */
  public boolean nameTakenByOther(UUID groupId, UUID tagId, String name) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM tags WHERE group_id = ? AND id <> ?"
                + " AND lower(name) = lower(?))")
        .params(groupId, tagId, name)
        .query(Boolean.class)
        .single();
  }

  /** Gives the tag its new name; false when the tag is gone. */
  public boolean rename(UUID tagId, String name) {
    return jdbc.sql("UPDATE tags SET name = ? WHERE id = ?").params(name, tagId).update() == 1;
  }

  /** Deletes the tag and, by cascade, its place on every contact; false when it is gone. */
  public boolean delete(UUID tagId) {
    return jdbc.sql("DELETE FROM tags WHERE id = ?").param(tagId).update() == 1;
  }

  private static String likeFolded(String part) {
    String text = part == null ? "" : part.strip();
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
