package app.contactgroups.domain;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Finds sets of contacts that are probably the same person (US-021, ADR-11): hash maps on keys, no
 * pair loops, so a group of 10,000 contacts is cheap. Contacts are linked by an equal e-mail or by
 * an equal first and last name plus an equal company or phone; sets are the connected parts.
 */
public final class DuplicateDetector {

  private static final String SEP = "\u0000";

  private DuplicateDetector() {}

  public static List<DuplicateMatch> detect(List<DuplicateCandidate> candidates) {
    int[] parent = new int[candidates.size()];
    for (int i = 0; i < parent.length; i++) {
      parent[i] = i;
    }
    for (DuplicateReason reason : DuplicateReason.values()) {
      union(candidates, keyFor(reason), parent);
    }
    Map<Integer, Set<DuplicateReason>> reasons = reasonsOfSets(candidates, parent);
    Map<Integer, List<UUID>> members = new LinkedHashMap<>();
    for (int i = 0; i < parent.length; i++) {
      members.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(candidates.get(i).id());
    }
    List<DuplicateMatch> sets = new ArrayList<>();
    members.forEach(
        (root, ids) -> {
          if (ids.size() >= 2) {
            Set<DuplicateReason> why = reasons.get(root);
            DuplicateKind kind =
                why.contains(DuplicateReason.SAME_EMAIL)
                    ? DuplicateKind.CERTAIN
                    : DuplicateKind.PROBABLE;
            sets.add(new DuplicateMatch(kind, List.copyOf(why), ids));
          }
        });
    return sets;
  }

  /** Joins every contact to the first one that has the same key. */
  private static void union(
      List<DuplicateCandidate> candidates, Function<DuplicateCandidate, String> key, int[] parent) {
    Map<String, Integer> first = new HashMap<>();
    for (int i = 0; i < candidates.size(); i++) {
      String k = key.apply(candidates.get(i));
      Integer seen = k == null ? null : first.putIfAbsent(k, i);
      if (seen != null) {
        parent[find(parent, i)] = find(parent, seen);
      }
    }
  }

  /** The reasons of the links inside each final set, in the order of the enum. */
  private static Map<Integer, Set<DuplicateReason>> reasonsOfSets(
      List<DuplicateCandidate> candidates, int[] parent) {
    Map<Integer, Set<DuplicateReason>> out = new HashMap<>();
    for (DuplicateReason reason : DuplicateReason.values()) {
      Function<DuplicateCandidate, String> key = keyFor(reason);
      Map<String, Integer> count = new HashMap<>();
      candidates.stream()
          .map(key)
          .filter(k -> k != null)
          .forEach(k -> count.merge(k, 1, Integer::sum));
      for (int i = 0; i < parent.length; i++) {
        String k = key.apply(candidates.get(i));
        if (k != null && count.get(k) > 1) {
          out.computeIfAbsent(find(parent, i), r -> EnumSet.noneOf(DuplicateReason.class))
              .add(reason);
        }
      }
    }
    return out;
  }

  private static Function<DuplicateCandidate, String> keyFor(DuplicateReason reason) {
    return switch (reason) {
      case SAME_EMAIL -> c -> blank(c.email()) ? null : c.email().strip();
      case SAME_NAME_AND_COMPANY ->
          c -> {
            String name = nameKey(c);
            return name == null || blank(c.company()) ? null : name + SEP + c.company().strip();
          };
      case SAME_NAME_AND_PHONE ->
          c -> {
            String name = nameKey(c);
            String phone = c.phone() == null ? "" : c.phone().replaceAll("[^0-9+]", "");
            return name == null || phone.isEmpty() ? null : name + SEP + phone;
          };
    };
  }

  private static String nameKey(DuplicateCandidate c) {
    return blank(c.firstName()) || blank(c.lastName())
        ? null
        : c.firstName().strip() + SEP + c.lastName().strip();
  }

  private static int find(int[] parent, int i) {
    int root = i;
    while (parent[root] != root) {
      root = parent[root];
    }
    int x = i;
    while (parent[x] != root) {
      int next = parent[x];
      parent[x] = root;
      x = next;
    }
    return root;
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }
}
