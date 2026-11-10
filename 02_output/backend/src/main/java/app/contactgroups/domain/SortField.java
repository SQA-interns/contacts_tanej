package app.contactgroups.domain;

/**
 * The columns the list can be sorted by, mapped to column names; no other text reaches SQL (SR-07).
 */
public enum SortField {
  lastName("last_name"),
  firstName("first_name"),
  email("email"),
  phone("phone"),
  company("company"),
  city("city"),
  country("country");

  private final String column;

  SortField(String column) {
    this.column = column;
  }

  public String column() {
    return column;
  }
}
