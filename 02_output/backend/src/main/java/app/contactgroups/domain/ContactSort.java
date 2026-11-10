package app.contactgroups.domain;

/** The sort of the list; a missing sort means last name, then first name (R-009-01). */
public record ContactSort(SortField field, Direction direction) {

  public enum Direction {
    ASC,
    DESC
  }

  public static final ContactSort DEFAULT = new ContactSort(SortField.lastName, Direction.ASC);

  /** Missing parts of a request fall back to the default. */
  public static ContactSort of(ContactSort requested) {
    if (requested == null || requested.field() == null) {
      return DEFAULT;
    }
    Direction direction = requested.direction() == null ? Direction.ASC : requested.direction();
    return new ContactSort(requested.field(), direction);
  }
}
