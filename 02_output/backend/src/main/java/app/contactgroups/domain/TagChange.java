package app.contactgroups.domain;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** The tags a save adds to a contact and takes from it; names compare ignoring case (BR-07). */
public record TagChange(List<String> added, List<String> removed) {

  public TagChange {
    added = List.copyOf(added);
    removed = List.copyOf(removed);
  }

  public static TagChange between(List<String> current, List<String> wanted) {
    Set<String> have = keys(current);
    Set<String> want = keys(wanted);
    return new TagChange(
        wanted.stream().filter(t -> !have.contains(key(t))).toList(),
        current.stream().filter(t -> !want.contains(key(t))).toList());
  }

  public boolean isEmpty() {
    return added.isEmpty() && removed.isEmpty();
  }

  private static Set<String> keys(List<String> names) {
    return names.stream().map(TagChange::key).collect(Collectors.toSet());
  }

  private static String key(String name) {
    return name.toLowerCase(Locale.ROOT);
  }
}
