package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** R-006-01, R-006-02, R-006-06: what a save adds to and takes from the tags of a contact. */
class TagChangeTest {

  @Test
  void findsAddedAndRemovedTags() {
    TagChange change = TagChange.between(List.of("a", "b"), List.of("b", "c"));
    assertThat(change.added()).containsExactly("c");
    assertThat(change.removed()).containsExactly("a");
    assertThat(change.isEmpty()).isFalse();
  }

  @Test
  void comparesIgnoringCaseSoARetypedTagIsNoChange() {
    TagChange change = TagChange.between(List.of("udelezenec2025"), List.of("UDELEZENEC2025"));
    assertThat(change.isEmpty()).isTrue();
  }

  @Test
  void anEmptyWantedListRemovesEverything() {
    TagChange change = TagChange.between(List.of("a", "b"), List.of());
    assertThat(change.added()).isEmpty();
    assertThat(change.removed()).containsExactly("a", "b");
  }
}
