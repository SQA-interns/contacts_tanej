package app.contactgroups.repository;

import app.contactgroups.domain.ContactPage;
import app.contactgroups.domain.ContactRow;
import app.contactgroups.domain.ContactSort;
import app.contactgroups.domain.ContactSort.Direction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The list and search query over {@code contacts} (US-009). Search text only ever travels as bind
 * parameters; the sort comes from a fixed list (SR-07).
 */
@Repository
public class ContactSearchRepository {

  private static final String COLLATION = "COLLATE \"sl-SI-x-icu\"";

  /** One word must be part of a folded own field, a tag name or an attribute value (R-009-03). */
  private static final String WORD =
      " AND (c.search_fold LIKE f_fold(?) ESCAPE '\\'"
          + " OR EXISTS (SELECT 1 FROM contact_tags ct JOIN tags t ON t.id = ct.tag_id"
          + " WHERE ct.contact_id = c.id AND f_fold(t.name) LIKE f_fold(?) ESCAPE '\\')"
          + " OR EXISTS (SELECT 1 FROM attributes a"
          + " WHERE a.contact_id = c.id AND f_fold(a.value) LIKE f_fold(?) ESCAPE '\\'))";

  private final JdbcClient jdbc;

  public ContactSearchRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** The number of active contacts of the group in which every pattern matches. */
  public int count(UUID groupId, List<String> patterns) {
    return jdbc.sql("SELECT count(*) FROM contacts c WHERE " + where(patterns))
        .params(params(groupId, patterns))
        .query(Integer.class)
        .single();
  }

  /** One page of rows, {@code page} counted from 1, with the tags of each contact. */
  public List<ContactRow> page(UUID groupId, List<String> patterns, ContactSort sort, int page) {
    List<Object> params = params(groupId, patterns);
    params.add(ContactPage.PAGE_SIZE);
    params.add((page - 1) * ContactPage.PAGE_SIZE);
    List<ContactRow> rows =
        jdbc.sql(
                "SELECT c.* FROM contacts c WHERE "
                    + where(patterns)
                    + orderBy(sort)
                    + " LIMIT ? OFFSET ?")
            .params(params)
            .query((rs, n) -> row(rs))
            .list();
    Map<UUID, List<String>> tags = tagsOf(rows);
    return rows.stream().map(r -> r.withTags(tags.getOrDefault(r.id(), List.of()))).toList();
  }

  private static String where(List<String> patterns) {
    return "c.group_id = ? AND c.state = 'ACTIVE'" + WORD.repeat(patterns.size());
  }

  private static List<Object> params(UUID groupId, List<String> patterns) {
    List<Object> params = new ArrayList<>();
    params.add(groupId);
    for (String pattern : patterns) {
      params.add(pattern);
      params.add(pattern);
      params.add(pattern);
    }
    return params;
  }

  /** The chosen column first, then last name, first name and id so that the order is stable. */
  private static String orderBy(ContactSort sort) {
    String column = "c." + sort.field().column();
    String direction = sort.direction() == Direction.ASC ? "ASC NULLS LAST" : "DESC NULLS FIRST";
    return " ORDER BY "
        + column
        + " "
        + COLLATION
        + " "
        + direction
        + ", c.last_name "
        + COLLATION
        + " NULLS LAST, c.first_name "
        + COLLATION
        + " NULLS LAST, c.id";
  }

  private Map<UUID, List<String>> tagsOf(List<ContactRow> rows) {
    Map<UUID, List<String>> byContact = new HashMap<>();
    if (rows.isEmpty()) {
      return byContact;
    }
    UUID[] ids = rows.stream().map(ContactRow::id).toArray(UUID[]::new);
    jdbc.sql(
            "SELECT ct.contact_id, t.name FROM contact_tags ct JOIN tags t ON t.id = ct.tag_id"
                + " WHERE ct.contact_id = ANY (?) ORDER BY lower(t.name), t.name")
        .param(ids)
        .query(
            rs -> {
              byContact
                  .computeIfAbsent(rs.getObject("contact_id", UUID.class), k -> new ArrayList<>())
                  .add(rs.getString("name"));
            });
    return byContact;
  }

  private static ContactRow row(ResultSet rs) throws SQLException {
    return new ContactRow(
        rs.getObject("id", UUID.class),
        text(rs, "last_name"),
        text(rs, "first_name"),
        text(rs, "email"),
        text(rs, "phone"),
        text(rs, "company"),
        text(rs, "city"),
        text(rs, "country"),
        List.of());
  }

  private static String text(ResultSet rs, String column) throws SQLException {
    String value = rs.getString(column);
    return value == null ? "" : value;
  }
}
