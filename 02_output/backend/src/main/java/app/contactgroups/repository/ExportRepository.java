package app.contactgroups.repository;

import app.contactgroups.domain.Condition;
import app.contactgroups.domain.Contact;
import app.contactgroups.domain.Contact.Attribute;
import app.contactgroups.domain.ContactSort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The contacts of an export with their tags and attributes, in the order of the list. Only active
 * contacts of the group are read, so a contact in Trash or of another group never leaves (R-011-12,
 * SR-02).
 */
@Repository
public class ExportRepository {

  private final JdbcClient jdbc;

  public ExportRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** The number of active contacts among the ticked ones. */
  public int countIds(UUID groupId, List<UUID> ids) {
    Where where = Where.of(groupId, List.of(), List.of());
    List<Object> params = new ArrayList<>(where.params());
    params.add(ids.toArray(UUID[]::new));
    return jdbc.sql("SELECT count(*) FROM contacts c WHERE " + where.sql() + " AND c.id = ANY (?)")
        .params(params)
        .query(Integer.class)
        .single();
  }

  /** The ticked active contacts of the group; unknown, foreign and trashed ids are left out. */
  public List<Contact> findByIds(UUID groupId, List<UUID> ids, ContactSort sort) {
    Where where = Where.of(groupId, List.of(), List.of());
    List<Object> params = new ArrayList<>(where.params());
    params.add(ids.toArray(UUID[]::new));
    return load(where.sql() + " AND c.id = ANY (?)", params, sort);
  }

  /** Every active contact of the group that matches the search words and the conditions. */
  public List<Contact> findByFilter(
      UUID groupId, List<String> patterns, List<Condition> conditions, ContactSort sort) {
    Where where = Where.of(groupId, patterns, conditions);
    return load(where.sql(), where.params(), sort);
  }

  private List<Contact> load(String where, List<Object> params, ContactSort sort) {
    List<Contact> rows =
        jdbc.sql(
                "SELECT c.* FROM contacts c WHERE " + where + ContactSearchRepository.orderBy(sort))
            .params(params)
            .query((rs, n) -> ContactRepository.contact(rs))
            .list();
    if (rows.isEmpty()) {
      return rows;
    }
    UUID[] ids = rows.stream().map(Contact::id).toArray(UUID[]::new);
    Map<UUID, List<String>> tags = new HashMap<>();
    jdbc.sql(
            "SELECT ct.contact_id, t.name FROM contact_tags ct JOIN tags t ON t.id = ct.tag_id"
                + " WHERE ct.contact_id = ANY (?) ORDER BY lower(t.name), t.name")
        .param(ids)
        .query(
            rs -> {
              tags.computeIfAbsent(rs.getObject("contact_id", UUID.class), k -> new ArrayList<>())
                  .add(rs.getString("name"));
            });
    Map<UUID, List<Attribute>> attributes = new HashMap<>();
    jdbc.sql(
            "SELECT contact_id, key, value FROM attributes WHERE contact_id = ANY (?)"
                + " ORDER BY lower(key), key")
        .param(ids)
        .query(
            rs -> {
              attributes
                  .computeIfAbsent(rs.getObject("contact_id", UUID.class), k -> new ArrayList<>())
                  .add(new Attribute(rs.getString("key"), rs.getString("value")));
            });
    return rows.stream()
        .map(
            c ->
                c.withTags(tags.getOrDefault(c.id(), List.of()))
                    .withAttributes(attributes.getOrDefault(c.id(), List.of())))
        .toList();
  }
}
