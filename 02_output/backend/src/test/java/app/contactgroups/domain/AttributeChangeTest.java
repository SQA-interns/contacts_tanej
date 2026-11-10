package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.domain.Contact.Attribute;
import java.util.List;
import org.junit.jupiter.api.Test;

/** R-008-01, R-008-07, R-013-02: what a save changes on the attributes of a contact. */
class AttributeChangeTest {

  @Test
  void anAddedAttributeHasNoOldValue() {
    List<FieldChange> changes =
        AttributeChange.between(List.of(), List.of(new Attribute("leto", "2025")));
    assertThat(changes).containsExactly(new FieldChange("attribute:leto", null, "2025"));
  }

  @Test
  void aRemovedAttributeHasNoNewValue() {
    List<FieldChange> changes =
        AttributeChange.between(List.of(new Attribute("leto", "2025")), List.of());
    assertThat(changes).containsExactly(new FieldChange("attribute:leto", "2025", null));
  }

  @Test
  void aChangedValueKeepsBoth() {
    List<FieldChange> changes =
        AttributeChange.between(
            List.of(new Attribute("leto", "2025")), List.of(new Attribute("LETO", "2026")));
    assertThat(changes).containsExactly(new FieldChange("attribute:LETO", "2025", "2026"));
  }

  @Test
  void theSameAttributesAreNoChange() {
    List<Attribute> same = List.of(new Attribute("leto", "2025"));
    assertThat(AttributeChange.between(same, List.of(new Attribute("leto", "2025")))).isEmpty();
  }
}
