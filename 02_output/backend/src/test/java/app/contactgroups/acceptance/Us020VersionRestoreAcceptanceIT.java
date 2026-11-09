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

/**
 * US-020 View, compare and restore versions (API part): AC-020-01 to AC-020-07, AC-020-09,
 * EX-020-01 to EX-020-06.
 */
class Us020VersionRestoreAcceptanceIT extends AcceptanceBase {

  private String gid;
  private String id;

  /** Version 1: phone 040, tag a, attribute k=1. Version 2: phone 041. Version 3: tag b added. */
  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    JsonNode c =
        newContact(
            ANA,
            gid,
            j(
                "firstName",
                "Ana",
                "lastName",
                "Novak",
                "phone",
                "+386 40 123 456",
                "tags",
                arr("a"),
                "attributes",
                arr(attr("k", "1"))));
    id = c.path("id").asText();
    save(MARKO, gid, current(), "phone", "+386 41 555 123").expect(200);
    save(MARKO, gid, current(), "tags", arr("a", "b")).expect(200);
  }

  private String base() {
    return "/groups/" + gid + "/contacts/" + id;
  }

  private JsonNode current() {
    return openContact(ANA, gid, id);
  }

  private Res restoreVersion(Who who, int number, long revision) {
    return post(who, base() + "/versions/" + number + "/restore", j("revision", revision));
  }

  private JsonNode versions() {
    return get(ANA, base() + "/versions").expect(200).json();
  }

  @Test
  void AC_020_01_theVersionsAreListedNewestFirstWithNameAndTime() {
    JsonNode v = get(MARKO, base() + "/versions").expect(200).json();
    assertThat(values(v, "number")).containsExactly("3", "2", "1");
    for (JsonNode e : v) {
      assertThat(e.path("userName").asText()).isNotBlank();
      assertThat(e.path("createdAt").asText()).isNotBlank();
    }
  }

  @Test
  void AC_020_02_aVersionIsShownReadOnly() {
    Res r = get(MARKO, base() + "/versions/1").expect(200);
    assertThat(r.at("/snapshot/phone").asText()).isEqualTo("+386 40 123 456");
    assertThat(r.at("/number").asInt()).isEqualTo(1);
    for (String method : List.of("PUT", "PATCH", "DELETE")) {
      assertThat(
              request(
                      method,
                      base() + "/versions/1",
                      "Bearer " + token(MARKO),
                      method.equals("DELETE") ? null : j())
                  .status())
          .as(method)
          .isIn(404, 405);
    }
    assertThat(get(MARKO, base() + "/versions/1").at("/snapshot/phone").asText())
        .isEqualTo("+386 40 123 456");
  }

  @Test
  void AC_020_03_theSnapshotOfAVersionDiffersFromTheCurrentContactOnlyInTheChangedPhone() {
    JsonNode v1 = get(MARKO, base() + "/versions/1").expect(200).at("/snapshot");
    JsonNode now = current();
    assertThat(v1.path("phone").asText()).isNotEqualTo(now.path("phone").asText());
    assertThat(v1.path("firstName").asText()).isEqualTo(now.path("firstName").asText());
    assertThat(v1.path("lastName").asText()).isEqualTo(now.path("lastName").asText());
    assertThat(v1.path("company").asText()).isEqualTo(now.path("company").asText());
    assertThat(strings(v1.path("tags"))).containsExactly("a");
    assertThat(strings(now.path("tags"))).containsExactlyInAnyOrder("a", "b");
  }

  @Test
  void AC_020_04_restoringVersion1BringsBackFieldsTagsAndAttributesAsANewVersion() {
    JsonNode before = current();
    Res r = restoreVersion(MARKO, 1, before.path("revision").asLong()).expect(200);
    assertThat(r.at("/phone").asText()).isEqualTo("+386 40 123 456");
    assertThat(strings(r.at("/tags"))).containsExactly("a");
    assertThat(r.at("/attributes/0/key").asText()).isEqualTo("k");
    assertThat(r.at("/versionNumber").asInt()).isEqualTo(4);
    assertThat(versions()).hasSize(4);
    List<JsonNode> restored = entries(ANA, gid, "VERSION_RESTORED");
    assertThat(restored).hasSize(1);
    assertThat(restored.get(0).toString()).contains("phone", "1");
  }

  @Test
  void AC_020_05_aContactInTrashRefusesTheRestoreOfAVersion() {
    long revision = current().path("revision").asLong();
    trash(ANA, gid, id);
    restoreVersion(MARKO, 1, revision).expectError(409, "CONTACT_IN_TRASH");
    assertThat(versions()).hasSize(3);
  }

  @Test
  void AC_020_06_aVersionEqualToTheCurrentContactChangesNothing() {
    long revision = current().path("revision").asLong();
    restoreVersion(MARKO, 3, revision).expectError(409, "NOTHING_TO_CHANGE");
    assertThat(versions()).hasSize(3);
    assertThat(current().path("revision").asLong()).isEqualTo(revision);
  }

  @Test
  void AC_020_07_aViewerHasNoVersions() {
    long revision = current().path("revision").asLong();
    get(PETRA, base() + "/versions").expectError(403, "FORBIDDEN");
    get(PETRA, base() + "/versions/1").expectError(403, "FORBIDDEN");
    restoreVersion(PETRA, 1, revision).expectError(403, "FORBIDDEN");
  }

  @Test
  void AC_020_09_aChangeBySomeoneElseSinceTheTabWasOpenedMakesTheRestoreAConflict() {
    long opened = current().path("revision").asLong();
    save(ANA, gid, current(), "company", "Acme").expect(200);
    restoreVersion(MARKO, 1, opened).expectError(409, "CONFLICT");
    assertThat(current().path("phone").asText()).isEqualTo("+386 41 555 123");
    assertThat(versions()).hasSize(4);
  }

  @Test
  void EX_020_01_aVersionCanBeRestoredAfterTheContactWasTrashedAndRestored() {
    trash(ANA, gid, id);
    post(ANA, base() + "/restore", null).expect(204);
    restoreVersion(MARKO, 1, current().path("revision").asLong()).expect(200);
    assertThat(current().path("phone").asText()).isEqualTo("+386 40 123 456");
  }

  @Test
  void EX_020_02_aRestoreBringsAllFiftyTagsAndLeavesNoneOfTheCurrentOnes() {
    List<String> first = new ArrayList<>();
    List<String> second = new ArrayList<>();
    for (int i = 1; i <= 50; i++) {
      first.add("a" + i);
      second.add("b" + i);
    }
    JsonNode c = newContact(ANA, gid, j("lastName", "Tagovec", "tags", arr(first.toArray())));
    String cid = c.path("id").asText();
    save(ANA, gid, openContact(ANA, gid, cid), "tags", arr(second.toArray())).expect(200);
    JsonNode now = openContact(ANA, gid, cid);
    post(
            ANA,
            "/groups/" + gid + "/contacts/" + cid + "/versions/1/restore",
            j("revision", now.path("revision").asLong()))
        .expect(200);
    assertThat(strings(openContact(ANA, gid, cid).path("tags")))
        .containsExactlyInAnyOrderElementsOf(first);
  }

  @Test
  void EX_020_03_aVersionNumberThatDoesNotExistIsNotFound() {
    get(MARKO, base() + "/versions/99").expectError(404, "NOT_FOUND");
    restoreVersion(MARKO, 99, current().path("revision").asLong()).expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_020_04_aVersionOfAnotherContactIsNotFound() {
    JsonNode other = newContact(ANA, gid, j("lastName", "Drugi"));
    JsonNode o = other;
    for (int i = 1; i <= 5; i++) {
      o = save(ANA, gid, o, "company", "F" + i).expect(200).json();
    }
    get(MARKO, base() + "/versions/6").expectError(404, "NOT_FOUND");
    get(MARKO, "/groups/" + gid + "/contacts/" + UUID.randomUUID() + "/versions/1")
        .expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_020_05_aRestoreAfterAMergeBringsBackThePreMergeValuesAndKeepsTheOthersInTrash() {
    JsonNode b =
        newContact(
            ANA,
            gid,
            j(
                "firstName",
                "Ana",
                "lastName",
                "Novak",
                "phone",
                "+386 40 000 000",
                "company",
                "Acme"));
    JsonNode a = current();
    String bid = b.path("id").asText();
    Res merged =
        post(
                ANA,
                "/groups/" + gid + "/merges",
                j(
                    "contacts",
                    arr(
                        j("id", id, "revision", a.path("revision").asLong()),
                        j("id", bid, "revision", b.path("revision").asLong())),
                    "fields",
                    j(
                        "firstName",
                        "Ana",
                        "lastName",
                        "Novak",
                        "phone",
                        "+386 41 555 123",
                        "company",
                        "Acme")))
            .expect(200);
    long revision = merged.at("/revision").asLong();
    int mergedVersion = merged.at("/versionNumber").asInt();
    restoreVersion(ANA, mergedVersion - 1, revision).expect(200);
    JsonNode after = current();
    assertThat(after.path("phone").asText()).isEqualTo("+386 41 555 123");
    assertThat(after.path("company").asText()).isEmpty();
    assertThat(get(ANA, "/groups/" + gid + "/contacts/" + bid).expect(200).at("/state").asText())
        .isEqualTo("TRASHED");
  }

  @Test
  void EX_020_06_restoringTwiceInARowSaysNothingToChangeTheSecondTime() {
    Res first = restoreVersion(MARKO, 1, current().path("revision").asLong()).expect(200);
    restoreVersion(MARKO, 1, first.at("/revision").asLong()).expectError(409, "NOTHING_TO_CHANGE");
    assertThat(versions()).hasSize(4);
  }
}
