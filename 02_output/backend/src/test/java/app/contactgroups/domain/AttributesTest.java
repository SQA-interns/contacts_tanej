package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import app.contactgroups.domain.Contact.Attribute;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** R-008-02, R-008-03, R-008-04, R-008-06; EX-008-01 to EX-008-06: the rules of the attributes. */
class AttributesTest {

  private static List<FieldProblem> refused(List<Attribute> raw) {
    var thrown =
        assertThatThrownBy(() -> Attributes.validate(raw)).isInstanceOf(ApiException.class);
    return ((ApiException) thrown.actual()).fields();
  }

  @Test
  void trimsTheKeyAndKeepsTheValueAsTyped() {
    List<Attribute> out = Attributes.validate(List.of(new Attribute("  kompetenca ", " 2025 ")));
    assertThat(out).containsExactly(new Attribute("kompetenca", " 2025 "));
  }

  @Test
  void anEmptyOrMissingValueIsAccepted() {
    assertThat(Attributes.validate(List.of(new Attribute("a", ""), new Attribute("b", null))))
        .containsExactly(new Attribute("a", ""), new Attribute("b", ""));
  }

  @Test
  void acceptsKeyOf50AndValueOf255() {
    assertThat(Attributes.validate(List.of(new Attribute("k".repeat(50), "v".repeat(255)))))
        .hasSize(1);
  }

  @Test
  void refusesAnEmptyKey() {
    assertThat(refused(List.of(new Attribute("   ", "v"))))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(tuple("attributes[0].key", "REQUIRED"));
  }

  @Test
  void refusesLongKeyAndLongValue() {
    List<FieldProblem> problems =
        refused(List.of(new Attribute("k".repeat(51), "v"), new Attribute("k", "v".repeat(256))));
    assertThat(problems)
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(
            tuple("attributes[0].key", "TOO_LONG"), tuple("attributes[1].value", "TOO_LONG"));
  }

  @Test
  void refusesTheSameKeyIgnoringCase() {
    assertThat(refused(List.of(new Attribute("kompetenca", "a"), new Attribute("Kompetenca", "b"))))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(tuple("attributes[1].key", "DUPLICATE_KEY"));
  }

  @Test
  void keysThatDifferInAccentsAreDifferent() {
    assertThat(
            Attributes.validate(List.of(new Attribute("Ključ", "a"), new Attribute("kljuc", "b"))))
        .hasSize(2);
  }

  @Test
  void acceptsFiftyAndRefusesFiftyOne() {
    List<Attribute> list = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      list.add(new Attribute("k" + i, "v"));
    }
    assertThat(Attributes.validate(list)).hasSize(50);
    list.add(new Attribute("k51", "v"));
    assertThat(refused(list))
        .extracting(FieldProblem::field, FieldProblem::code)
        .containsExactly(tuple("attributes", "TOO_MANY"));
  }
}
