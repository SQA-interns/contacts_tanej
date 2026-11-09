package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-007 Rename and delete tags: AC-007-01 to AC-007-08, EX-007-01 to EX-007-05. */
class Us007TagAdminAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private JsonNode tagged(String last, Object... tags) {
    JsonNode c = newContact(ANA, gid, j("firstName", "Ana", "lastName", last, "tags", arr(tags)));
    return c;
  }

  private String tagId(String name) {
    for (JsonNode t : get(ANA, "/groups/" + gid + "/tags").expect(200).json()) {
      if (name.equals(t.path("name").asText())) {
        return t.path("id").asText();
      }
    }
    throw new AssertionError("tag " + name + " is not offered in the group");
  }

  private Res rename(Who who, String tagId, String name) {
    return patch(who, "/groups/" + gid + "/tags/" + tagId, j("name", name));
  }

  private List<String> tagsOf(JsonNode contact) {
    return strings(openContact(ANA, gid, contact.path("id").asText()).path("tags"));
  }

  private List<String> groupTags() {
    return values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name");
  }

  @Test
  void AC_007_01_renamingATagChangesItOnEveryContact() {
    List<JsonNode> cs =
        List.of(
            tagged("A", "udelezenec2205"),
            tagged("B", "udelezenec2205"),
            tagged("C", "udelezenec2205"));
    rename(ANA, tagId("udelezenec2205"), "udelezenec2025").expect(200);
    for (JsonNode c : cs) {
      assertThat(tagsOf(c)).containsExactly("udelezenec2025");
    }
    assertThat(groupTags()).containsExactly("udelezenec2025");
  }

  @Test
  void AC_007_02_renamingOntoAnotherTagIgnoringCaseIsRefusedAndBothStay() {
    tagged("A", "udelezenec2025");
    tagged("B", "gost");
    rename(ANA, tagId("gost"), "UDELEZENEC2025").expectError(409, "TAG_NAME_TAKEN");
    assertThat(groupTags()).containsExactlyInAnyOrder("udelezenec2025", "gost");
  }

  @Test
  void AC_007_03_aChangeOfCapitalisationOnlyChangesTheSpelling() {
    JsonNode c = tagged("A", "vip");
    rename(ANA, tagId("vip"), "VIP").expect(200);
    assertThat(tagsOf(c)).containsExactly("VIP");
  }

  @Test
  void AC_007_04_deletingATagRemovesItFromAllContactsAndDeletesNone() {
    List<JsonNode> cs = new ArrayList<>();
    for (String l : List.of("A", "B", "C", "D")) {
      cs.add(tagged(l, "old"));
    }
    delete(MARKO, "/groups/" + gid + "/tags/" + tagId("old")).expect(204);
    assertThat(groupTags()).isEmpty();
    for (JsonNode c : cs) {
      assertThat(tagsOf(c)).isEmpty();
    }
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(4);
  }

  @Test
  void AC_007_05_deletingATagCreatesNoVersion() {
    JsonNode c = tagged("A");
    JsonNode v1 = openContact(ANA, gid, c.path("id").asText());
    JsonNode v2 = save(ANA, gid, v1, "tags", arr("old")).expect(200).json();
    save(ANA, gid, v2, "phone", "+386 40 123 456").expect(200);
    delete(ANA, "/groups/" + gid + "/tags/" + tagId("old")).expect(204);
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("versionNumber").asInt())
        .isEqualTo(3);
    assertThat(
            get(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/versions")
                .expect(200)
                .json())
        .hasSize(3);
  }

  @Test
  void AC_007_06_renamingATagWritesOneHistoryEntryPerAffectedContact() {
    for (String l : List.of("A", "B", "C", "D")) {
      tagged(l, "old");
    }
    rename(ANA, tagId("old"), "new").expect(200);
    List<JsonNode> renamed = entries(ANA, gid, "TAG_RENAMED");
    assertThat(renamed).hasSize(4);
    for (JsonNode e : renamed) {
      assertThat(e.at("/changes/0/oldValue").asText()).isEqualTo("old");
      assertThat(e.at("/changes/0/newValue").asText()).isEqualTo("new");
    }
  }

  @Test
  void AC_007_07_aViewerCannotRenameOrDeleteATag() {
    tagged("A", "old");
    String id = tagId("old");
    rename(PETRA, id, "new").expectError(403, "FORBIDDEN");
    delete(PETRA, "/groups/" + gid + "/tags/" + id).expectError(403, "FORBIDDEN");
    assertThat(groupTags()).containsExactly("old");
  }

  @Test
  void AC_007_08_renamingToANameWithASpaceIsRefusedAndNothingChanges() {
    tagged("A", "old");
    rename(ANA, tagId("old"), "neue marke").expectFieldError("name", "INVALID_CHARACTERS");
    assertThat(groupTags()).containsExactly("old");
  }

  @Test
  void EX_007_01_renamingToTheSameNameChangesNothingAndWritesNoEntry() {
    JsonNode c = tagged("A", "vip");
    long revision = openContact(ANA, gid, c.path("id").asText()).path("revision").asLong();
    rename(ANA, tagId("vip"), "vip").expect(200);
    assertThat(entries(ANA, gid, "TAG_RENAMED")).isEmpty();
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("revision").asLong())
        .isEqualTo(revision);
  }

  @Test
  void EX_007_02_aTagOnATrashedContactIsStillListedAndRenameAndDeleteApplyToIt() {
    JsonNode c = tagged("A", "old");
    trash(ANA, gid, c.path("id").asText());
    JsonNode listed = get(ANA, "/groups/" + gid + "/tags").expect(200).json();
    assertThat(listed).hasSize(1);
    assertThat(listed.get(0).path("contactCount").asInt()).isEqualTo(1);
    rename(ANA, listed.get(0).path("id").asText(), "new").expect(200);
    assertThat(tagsOf(c)).containsExactly("new");
    delete(ANA, "/groups/" + gid + "/tags/" + tagId("new")).expect(204);
    assertThat(tagsOf(c)).isEmpty();
  }

  @Test
  void EX_007_03_aNewNameOf50CharactersIsAcceptedAndOf51Refused() {
    tagged("A", "old");
    rename(ANA, tagId("old"), repeat("b", 51)).expectFieldError("name", "TOO_LONG");
    rename(ANA, tagId("old"), repeat("b", 50)).expect(200);
    assertThat(groupTags()).containsExactly(repeat("b", 50));
  }

  @Test
  void EX_007_04_aRenameCannotGiveAContactTwoTagsOfTheSameName() {
    JsonNode c = tagged("A", "a", "b");
    rename(ANA, tagId("b"), "A").expectError(409, "TAG_NAME_TAKEN");
    assertThat(tagsOf(c)).containsExactlyInAnyOrder("a", "b");
  }

  @Test
  void EX_007_05_aTagThatNoContactUsesIsNotOfferedSoItCannotBeDeleted() {
    JsonNode c = tagged("A", "old");
    String id = tagId("old");
    save(ANA, gid, openContact(ANA, gid, c.path("id").asText()), "tags", arr()).expect(200);
    assertThat(groupTags()).isEmpty();
    delete(ANA, "/groups/" + gid + "/tags/" + id).expectError(404, "NOT_FOUND");
    delete(ANA, "/groups/" + gid + "/tags/" + UUID.randomUUID()).expectError(404, "NOT_FOUND");
  }
}
