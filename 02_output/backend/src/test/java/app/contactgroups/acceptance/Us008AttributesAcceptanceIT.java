package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-008 Attributes: AC-008-01 to AC-008-07, EX-008-01 to EX-008-06. */
class Us008AttributesAcceptanceIT extends AcceptanceBase {

  private String gid;
  private JsonNode ana;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, PETRA, "VIEWER");
    ana = newContact(ANA, gid, j("firstName", "Ana", "lastName", "Novak"));
  }

  private Res setAttrs(JsonNode contact, JsonNode... attrs) {
    JsonNode current = openContact(ANA, gid, contact.path("id").asText());
    return save(ANA, gid, current, "attributes", arr((Object[]) attrs));
  }

  private JsonNode attrsOf(JsonNode contact) {
    return openContact(ANA, gid, contact.path("id").asText()).path("attributes");
  }

  @Test
  void AC_008_01_anAttributeIsStoredOnTheContact() {
    setAttrs(ana, attr("kompetenca", "spletnoProgramiranje")).expect(200);
    JsonNode a = attrsOf(ana);
    assertThat(a).hasSize(1);
    assertThat(a.get(0).path("key").asText()).isEqualTo("kompetenca");
    assertThat(a.get(0).path("value").asText()).isEqualTo("spletnoProgramiranje");
  }

  @Test
  void AC_008_02_aKeyEqualIgnoringCaseIsRefusedAndTheContactIsUnchanged() {
    setAttrs(ana, attr("kompetenca", "spletnoProgramiranje")).expect(200);
    setAttrs(ana, attr("kompetenca", "spletnoProgramiranje"), attr("Kompetenca", "java"))
        .expectFieldErrorStarting("attributes", "DUPLICATE_KEY");
    assertThat(attrsOf(ana)).hasSize(1);
  }

  @Test
  void AC_008_03_keysUsedInTheGroupAreSuggestedButNeverForced() {
    JsonNode other = newContact(ANA, gid, j("firstName", "Eva", "lastName", "Horvat"));
    setAttrs(other, attr("kompetenca", "java")).expect(200);
    JsonNode suggested = get(ANA, "/groups/" + gid + "/attribute-keys?q=kom").expect(200).json();
    assertThat(strings(suggested)).containsExactly("kompetenca");
    setAttrs(ana, attr("komp2", "x")).expect(200);
    assertThat(attrsOf(ana).get(0).path("key").asText()).isEqualTo("komp2");
  }

  @Test
  void AC_008_04_a51stAttributeIsRefusedAndTheContactKeepsItsFifty() {
    List<JsonNode> fifty = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      fifty.add(attr("k" + i, "v"));
    }
    setAttrs(ana, fifty.toArray(new JsonNode[0])).expect(200);
    fifty.add(attr("k51", "v"));
    setAttrs(ana, fifty.toArray(new JsonNode[0]))
        .expectFieldErrorStarting("attributes", "TOO_MANY");
    assertThat(attrsOf(ana)).hasSize(50);
  }

  @Test
  void AC_008_05_aNumberLikeValueIsStoredAndShownAsText() {
    setAttrs(ana, attr("leto", "2025")).expect(200);
    JsonNode v = attrsOf(ana).get(0).path("value");
    assertThat(v.isTextual()).isTrue();
    assertThat(v.asText()).isEqualTo("2025");
  }

  @Test
  void AC_008_06_removingAnAttributeRemovesItFromThisContactOnly() {
    JsonNode other = newContact(ANA, gid, j("firstName", "Eva", "lastName", "Horvat"));
    setAttrs(ana, attr("kompetenca", "java")).expect(200);
    setAttrs(other, attr("kompetenca", "java")).expect(200);
    setAttrs(ana).expect(200);
    assertThat(attrsOf(ana)).isEmpty();
    assertThat(attrsOf(other)).hasSize(1);
  }

  @Test
  void AC_008_07_aViewerSeesAttributesAndCannotChangeThem() {
    setAttrs(ana, attr("kompetenca", "java")).expect(200);
    JsonNode seen = openContact(PETRA, gid, ana.path("id").asText());
    assertThat(seen.path("attributes")).hasSize(1);
    save(PETRA, gid, seen, "attributes", arr()).expectError(403, "FORBIDDEN");
    assertThat(attrsOf(ana)).hasSize(1);
  }

  @Test
  void EX_008_01_aKeyOf50IsAcceptedOf51AndEmptyRefused() {
    setAttrs(ana, attr(repeat("k", 50), "v")).expect(200);
    setAttrs(ana, attr(repeat("k", 51), "v")).expectFieldErrorStarting("attributes", "TOO_LONG");
    setAttrs(ana, attr("", "v")).expectFieldErrorStarting("attributes", "REQUIRED");
    assertThat(attrsOf(ana)).hasSize(1);
  }

  @Test
  void EX_008_02_aValueOf255IsAcceptedAndOf256Refused() {
    setAttrs(ana, attr("k", repeat("v", 255))).expect(200);
    setAttrs(ana, attr("k", repeat("v", 256))).expectFieldErrorStarting("attributes", "TOO_LONG");
    assertThat(attrsOf(ana).get(0).path("value").asText()).hasSize(255);
  }

  @Test
  void EX_008_03_exactlyFiftyAttributesAreAccepted() {
    List<JsonNode> fifty = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      fifty.add(attr("k" + i, "v"));
    }
    setAttrs(ana, fifty.toArray(new JsonNode[0])).expect(200);
    assertThat(attrsOf(ana)).hasSize(50);
  }

  @Test
  void EX_008_04_anEmptyValueIsAccepted() {
    setAttrs(ana, attr("leto", "")).expect(200);
    assertThat(attrsOf(ana)).hasSize(1);
    assertThat(attrsOf(ana).get(0).path("value").asText()).isEmpty();
  }

  @Test
  void EX_008_05_keysThatDifferOnlyInAccentsAreTwoDifferentKeys() {
    setAttrs(ana, attr("Ključ", "a"), attr("kljuc", "b")).expect(200);
    assertThat(attrsOf(ana)).hasSize(2);
  }

  @Test
  void EX_008_06_spacesAroundAKeyAreTrimmedBeforeTheChecks() {
    setAttrs(ana, attr("  kompetenca  ", "java")).expect(200);
    assertThat(attrsOf(ana).get(0).path("key").asText()).isEqualTo("kompetenca");
    setAttrs(ana, attr("  ", "java")).expectFieldErrorStarting("attributes", "REQUIRED");
    setAttrs(ana, attr(" " + repeat("k", 50) + " ", "v")).expect(200);
  }
}
