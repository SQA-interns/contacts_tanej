package app.contactgroups.domain;

import app.contactgroups.domain.Contact.Attribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What a save changes on the attributes of a contact: one {@link FieldChange} "attribute:key" per
 * key added, removed or given another value (R-013-02). Keys compare ignoring case (BR-07).
 */
public final class AttributeChange {

  private AttributeChange() {}

  public static List<FieldChange> between(List<Attribute> current, List<Attribute> wanted) {
    Map<String, Attribute> have = byKey(current);
    Map<String, Attribute> want = byKey(wanted);
    List<FieldChange> changes = new ArrayList<>();
    for (Attribute old : current) {
      if (!want.containsKey(key(old))) {
        changes.add(new FieldChange("attribute:" + old.key(), old.value(), null));
      }
    }
    for (Attribute next : wanted) {
      Attribute old = have.get(key(next));
      if (old == null) {
        changes.add(new FieldChange("attribute:" + next.key(), null, next.value()));
      } else if (!old.equals(next)) {
        changes.add(new FieldChange("attribute:" + next.key(), old.value(), next.value()));
      }
    }
    return changes;
  }

  private static Map<String, Attribute> byKey(List<Attribute> list) {
    Map<String, Attribute> map = new HashMap<>();
    list.forEach(a -> map.put(key(a), a));
    return map;
  }

  private static String key(Attribute attribute) {
    return attribute.key().toLowerCase(Locale.ROOT);
  }
}
