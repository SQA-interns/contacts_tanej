package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-006 Tag contacts: AC-006-01 to AC-006-08, EX-006-01 to EX-006-08. */
class Us006TagsAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private JsonNode contact(String last) {
    return newContact(ANA, gid, j("firstName", "Ana", "lastName", last));
  }

  private Res tag(Who who, JsonNode contact, Object... tags) {
    return save(who, gid, openContact(who, gid, contact.path("id").asText()), "tags", arr(tags));
  }

  private List<String> tagsOf(Who who, JsonNode contact) {
    return strings(openContact(who, gid, contact.path("id").asText()).path("tags"));
  }

  private List<String> groupTags(Who who, String group) {
    return values(get(who, "/groups/" + group + "/tags").expect(200).json(), "name");
  }

  @Test
  void AC_006_01_aTagIsAttachedToTheContactAndOfferedInTheGroup() {
    JsonNode c = contact("Novak");
    assertThat(groupTags(ANA, gid)).isEmpty();
    tag(ANA, c, "udelezenec2025").expect(200);
    assertThat(tagsOf(ANA, c)).containsExactly("udelezenec2025");
    assertThat(groupTags(ANA, gid)).containsExactly("udelezenec2025");
  }

  @Test
  void AC_006_02_aTagOfTheSameNameIgnoringCaseUsesTheExistingTagAndSpelling() {
    JsonNode first = contact("Novak");
    JsonNode other = contact("Horvat");
    tag(ANA, first, "udelezenec2025").expect(200);
    tag(MARKO, other, "UDELEZENEC2025").expect(200);
    assertThat(tagsOf(MARKO, other)).containsExactly("udelezenec2025");
    assertThat(groupTags(ANA, gid)).containsExactly("udelezenec2025");
  }

  @Test
  void AC_006_03_aTagWithASpaceIsRefusedNamingTheTagAndNothingIsStored() {
    JsonNode c = contact("Novak");
    tag(ANA, c, "neka značka").expectFieldErrorStarting("tags", "INVALID_CHARACTERS");
    assertThat(tagsOf(ANA, c)).isEmpty();
  }

  @Test
  void AC_006_04_a51stTagIsRefusedAndTheContactKeepsItsFifty() {
    JsonNode c = contact("Novak");
    List<String> fifty = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      fifty.add("t" + i);
    }
    tag(ANA, c, fifty.toArray()).expect(200);
    List<String> fiftyOne = new ArrayList<>(fifty);
    fiftyOne.add("t51");
    tag(ANA, c, fiftyOne.toArray()).expectFieldErrorStarting("tags", "TOO_MANY");
    assertThat(tagsOf(ANA, c)).hasSize(50);
  }

  @Test
  void AC_006_05_aTagOfAnotherGroupIsNeverSuggested() {
    tag(ANA, contact("Novak"), "neObvescaj").expect(200);
    String groupB = group(BORIS, "Poroka");
    assertThat(groupTags(BORIS, groupB)).doesNotContain("neObvescaj").isEmpty();
  }

  @Test
  void AC_006_06_removingATagDetachesItFromThatContactOnly() {
    JsonNode a = contact("Novak");
    JsonNode b = contact("Horvat");
    tag(ANA, a, "udelezenec2025", "neObvescaj").expect(200);
    tag(ANA, b, "neObvescaj").expect(200);
    tag(ANA, a, "udelezenec2025").expect(200);
    assertThat(tagsOf(ANA, a)).containsExactly("udelezenec2025");
    assertThat(tagsOf(ANA, b)).containsExactly("neObvescaj");
  }

  @Test
  void AC_006_07_aViewerSeesTheTagsButCannotChangeThem() {
    JsonNode c = contact("Novak");
    tag(ANA, c, "udelezenec2025").expect(200);
    assertThat(tagsOf(PETRA, c)).containsExactly("udelezenec2025");
    tag(PETRA, c, "udelezenec2025", "drugo").expectError(403, "FORBIDDEN");
    assertThat(tagsOf(ANA, c)).containsExactly("udelezenec2025");
  }

  @Test
  void AC_006_08_aTagIsOnlyALabelAndChangesNoBehaviour() {
    JsonNode c = contact("Novak");
    int before = search(ANA, gid, "").at("/total").asInt();
    tag(ANA, c, "neObvescaj").expect(200);
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(before);
    assertThat(get(ANA, "/groups/" + gid + "/members").expect(200).json()).hasSize(3);
    assertThat(get(PETRA, "/groups").expect(200).json()).hasSize(1);
    assertThatThrownBy(() -> Class.forName("jakarta.mail.Session"))
        .as("nothing can be sent because of a tag")
        .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void EX_006_01_aTagNameOfExactly50CharactersIsAcceptedAndOf51Refused() {
    JsonNode c = contact("Novak");
    tag(ANA, c, repeat("a", 50)).expect(200);
    tag(ANA, c, repeat("a", 51)).expectFieldErrorStarting("tags", "TOO_LONG");
    assertThat(tagsOf(ANA, c)).containsExactly(repeat("a", 50));
  }

  @Test
  void EX_006_02_aTagWithHyphenUnderscoreAndDigitsIsAccepted() {
    JsonNode c = contact("Novak");
    tag(ANA, c, "a-b_c9").expect(200);
    assertThat(tagsOf(ANA, c)).containsExactly("a-b_c9");
  }

  @Test
  void EX_006_03_tagsWithDotSpaceSlashOrNothingAreRefused() {
    JsonNode c = contact("Novak");
    tag(ANA, c, "a.b").expectFieldErrorStarting("tags", "INVALID_CHARACTERS");
    tag(ANA, c, "a b").expectFieldErrorStarting("tags", "INVALID_CHARACTERS");
    tag(ANA, c, "a/b").expectFieldErrorStarting("tags", "INVALID_CHARACTERS");
    tag(ANA, c, "").expectFieldErrorStarting("tags", "TOO_SHORT");
    assertThat(tagsOf(ANA, c)).isEmpty();
  }

  @Test
  void EX_006_04_aTagWithAccentedLettersIsAccepted() {
    JsonNode c = contact("Novak");
    tag(ANA, c, "čaj2026").expect(200);
    assertThat(tagsOf(ANA, c)).containsExactly("čaj2026");
  }

  @Test
  void EX_006_05_theSameTagTypedTwiceOnOneContactIsStoredOnce() {
    JsonNode c = contact("Novak");
    tag(ANA, c, "Vip", "vip").expect(200);
    assertThat(tagsOf(ANA, c)).containsExactly("Vip");
  }

  @Test
  void EX_006_06_exactlyFiftyTagsAreAccepted() {
    JsonNode c = contact("Novak");
    List<String> fifty = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      fifty.add("t" + i);
    }
    tag(ANA, c, fifty.toArray()).expect(200);
    assertThat(tagsOf(ANA, c)).hasSize(50);
  }

  @Test
  void EX_006_07_theLastContactLosingATagMakesTheGroupStopOfferingIt() {
    JsonNode c = contact("Novak");
    tag(ANA, c, "old").expect(200);
    assertThat(groupTags(ANA, gid)).containsExactly("old");
    tag(ANA, c).expect(200);
    assertThat(groupTags(ANA, gid)).isEmpty();
  }

  @Test
  void EX_006_08_aTagAddedWhileAnotherUserEditsTheSameContactFollowsTheConflictRule() {
    JsonNode c = contact("Novak");
    JsonNode forMarko = openContact(MARKO, gid, c.path("id").asText());
    JsonNode forAna = openContact(ANA, gid, c.path("id").asText());
    save(MARKO, gid, forMarko, "company", "Acme").expect(200);
    save(ANA, gid, forAna, "tags", arr("x")).expectError(409, "CONFLICT");
    assertThat(tagsOf(ANA, c)).isEmpty();
  }
}
