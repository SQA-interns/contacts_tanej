package app.contactgroups.repository;

import app.contactgroups.domain.Condition;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The WHERE part of the contact list: the group, active contacts, search words (R-009-03) and
 * filter conditions (R-010-01 to R-010-07), all with AND (R-010-08). The SQL text is fixed; every
 * value from the request is a bind parameter (SR-07).
 */
record Where(String sql, List<Object> params) {

  /** One word must be part of a folded own field, a tag name or an attribute value (R-009-03). */
  private static final String WORD =
      " AND (c.search_fold LIKE f_fold(?) ESCAPE '\\'"
          + " OR EXISTS (SELECT 1 FROM contact_tags ct JOIN tags t ON t.id = ct.tag_id"
          + " WHERE ct.contact_id = c.id AND f_fold(t.name) LIKE f_fold(?) ESCAPE '\\')"
          + " OR EXISTS (SELECT 1 FROM attributes a"
          + " WHERE a.contact_id = c.id AND f_fold(a.value) LIKE f_fold(?) ESCAPE '\\'))";

  private static final String HAS_TAG =
      " AND EXISTS (SELECT 1 FROM contact_tags ct JOIN tags t ON t.id = ct.tag_id"
          + " WHERE ct.contact_id = c.id AND lower(t.name) = lower(?))";

  private static final String HAS_KEY =
      " AND EXISTS (SELECT 1 FROM attributes a"
          + " WHERE a.contact_id = c.id AND lower(a.key) = lower(?)";

  static Where of(UUID groupId, List<String> patterns, List<Condition> conditions) {
    StringBuilder sql = new StringBuilder("c.group_id = ? AND c.state = 'ACTIVE'");
    List<Object> params = new ArrayList<>();
    params.add(groupId);
    for (String pattern : patterns) {
      sql.append(WORD);
      params.add(pattern);
      params.add(pattern);
      params.add(pattern);
    }
    for (Condition condition : conditions) {
      add(condition, sql, params);
    }
    return new Where(sql.toString(), params);
  }

  private static void add(Condition condition, StringBuilder sql, List<Object> params) {
    switch (condition) {
      case Condition.TagsAll c ->
          c.tags()
              .forEach(
                  tag -> {
                    sql.append(HAS_TAG);
                    params.add(tag);
                  });
      case Condition.TagsAny c -> {
        sql.append(" AND (FALSE");
        c.tags()
            .forEach(
                tag -> {
                  sql.append(" OR").append(HAS_TAG.substring(" AND".length()));
                  params.add(tag);
                });
        sql.append(")");
      }
      case Condition.TagNot c -> {
        sql.append(" AND NOT").append(HAS_TAG.substring(" AND".length()));
        params.add(c.tag());
      }
      case Condition.AttributeEquals c -> {
        sql.append(HAS_KEY).append(" AND f_fold(a.value) = f_fold(?))");
        params.add(c.key());
        params.add(c.value());
      }
      case Condition.AttributeExists c -> {
        sql.append(HAS_KEY).append(")");
        params.add(c.key());
      }
      case Condition.Country c -> {
        sql.append(" AND c.country = ?");
        params.add(c.code());
      }
      case Condition.Company c -> {
        sql.append(" AND f_fold(c.company) = f_fold(?)");
        params.add(c.name());
      }
    }
  }
}
