package app.contactgroups.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Splits the search text into words and turns each into a LIKE pattern of plain text (R-009-03).
 */
public final class SearchWords {

  private SearchWords() {}

  /**
   * The words of the text, each as {@code %word%} with {@code \}, {@code %} and {@code _} escaped,
   * so that they match as themselves (EX-009-03). Blank or missing text gives no word (EX-009-04).
   */
  public static List<String> patterns(String text) {
    String stripped = text == null ? "" : text.strip();
    if (stripped.isEmpty()) {
      return List.of();
    }
    return Arrays.stream(stripped.split("\\s+")).map(SearchWords::pattern).toList();
  }

  private static String pattern(String word) {
    return "%" + word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }
}
