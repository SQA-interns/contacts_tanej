package app.contactgroups.domain;

import java.util.List;

/** One page of the list: the rows, the number of matches and the page really returned. */
public record ContactPage(List<ContactRow> items, int total, int page, int pageSize) {

  public static final int PAGE_SIZE = 25;

  /** The last page that exists for this many matches; 1 when there are none (EX-009-09). */
  public static int lastPage(int total) {
    return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
  }
}
