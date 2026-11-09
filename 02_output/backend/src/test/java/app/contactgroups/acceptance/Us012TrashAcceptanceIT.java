package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-012 Trash and "Delete forever": AC-012-01 to AC-012-08, EX-012-01 to EX-012-08. */
class Us012TrashAcceptanceIT extends AcceptanceBase {

  private String gid;
  private String ana;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
    ana =
        newContact(
                ANA,
                gid,
                j(
                    "firstName", "Eva",
                    "lastName", "Horvat",
                    "email", "eva.horvat@example.com",
                    "tags", arr("udelezenec2025"),
                    "attributes", arr(attr("kompetenca", "java"))))
            .path("id")
            .asText();
  }

  private String path() {
    return "/groups/" + gid + "/contacts/" + ana;
  }

  private Res restore(Who who) {
    return post(who, path() + "/restore", null);
  }

  private Res erase(Who who) {
    return delete(who, path());
  }

  private JsonNode trashView(Who who) {
    return get(who, "/groups/" + gid + "/trash-contacts").expect(200).json();
  }

  private void waitUntilErased() {
    eventually(() -> get(ANA, path()).status() == 404, "the contact is erased by the cleanup");
  }

  @Test
  void AC_012_01_deletingMovesTheContactToTrashWithThirtyDaysLeft() {
    trash(MARKO, gid, ana);
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
    assertThat(search(ANA, gid, "horvat").at("/total").asInt()).isZero();
    JsonNode view = trashView(MARKO);
    assertThat(view.at("/total").asInt()).isEqualTo(1);
    assertThat(view.at("/items/0/id").asText()).isEqualTo(ana);
    assertThat(view.at("/items/0/daysLeft").asInt()).isEqualTo(30);
  }

  @Test
  void AC_012_02_restoringBringsBackAllData() {
    trash(MARKO, gid, ana);
    restore(MARKO).expect(204);
    JsonNode c = openContact(ANA, gid, ana);
    assertThat(c.path("state").asText()).isEqualTo("ACTIVE");
    assertThat(c.path("email").asText()).isEqualTo("eva.horvat@example.com");
    assertThat(strings(c.path("tags"))).containsExactly("udelezenec2025");
    assertThat(c.at("/attributes/0/value").asText()).isEqualTo("java");
    assertThat(search(ANA, gid, "horvat").at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void AC_012_03_deleteForeverByTheOwnerErasesAtOnce() {
    trash(MARKO, gid, ana);
    erase(ANA).expect(204);
    get(ANA, path()).expectError(404, "NOT_FOUND");
    assertThat(trashView(ANA).at("/total").asInt()).isZero();
  }

  @Test
  void AC_012_04_anEditorCannotDeleteForever() {
    trash(MARKO, gid, ana);
    erase(MARKO).expectError(403, "FORBIDDEN");
    assertThat(trashView(ANA).at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void AC_012_05_aContactInTrashFor29DaysCanBeRestored() {
    trash(MARKO, gid, ana);
    clock.advance(Duration.ofDays(29));
    restore(MARKO).expect(204);
    assertThat(openContact(ANA, gid, ana).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void AC_012_06_aContactInTrashFor30DaysIsErasedByTheCleanup() {
    trash(MARKO, gid, ana);
    clock.advance(Duration.ofDays(30).plusMinutes(1));
    waitUntilErased();
    restore(MARKO).expectError(404, "NOT_FOUND");
  }

  @Test
  void AC_012_07_aContactInTrashCannotBeEdited() {
    JsonNode opened = openContact(MARKO, gid, ana);
    trash(ANA, gid, ana);
    save(MARKO, gid, opened, "company", "Beta").expectError(409, "CONTACT_IN_TRASH");
  }

  @Test
  void AC_012_08_aViewerHasNoTrashAndCannotDeleteOrRestore() {
    get(PETRA, "/groups/" + gid + "/trash-contacts").expectError(403, "FORBIDDEN");
    post(PETRA, path() + "/trash", null).expectError(403, "FORBIDDEN");
    trash(ANA, gid, ana);
    restore(PETRA).expectError(403, "FORBIDDEN");
    erase(PETRA).expectError(403, "FORBIDDEN");
    assertThat(trashView(ANA).at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void EX_012_01_aContactInTrashFor29Days23HoursIsStillRestorable() {
    trash(MARKO, gid, ana);
    clock.advance(Duration.ofDays(29).plusHours(23));
    restore(ANA).expect(204);
  }

  @Test
  void EX_012_02_aContactInTrashForExactly30DaysIsErasedByTheNextCleanup() {
    trash(MARKO, gid, ana);
    clock.advance(Duration.ofDays(30));
    waitUntilErased();
  }

  @Test
  void EX_012_03_twoEditorsDeletingTheSameContactAtOnceGiveOneSuccessAndOneAlreadyInTrash()
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Callable<Res>> calls =
          List.of(
              () -> post(MARKO, path() + "/trash", null), () -> post(ANA, path() + "/trash", null));
      int ok = 0;
      int already = 0;
      for (Future<Res> f : pool.invokeAll(calls)) {
        Res r = f.get();
        if (r.status() == 204) {
          ok++;
        } else if (r.status() == 409 && "ALREADY_IN_TRASH".equals(r.code())) {
          already++;
        }
      }
      assertThat(ok).isEqualTo(1);
      assertThat(already).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void EX_012_04_restoringAnErasedContactIsNotFound() {
    trash(ANA, gid, ana);
    erase(ANA).expect(204);
    restore(ANA).expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_012_05_theThirtyDaysRestartAtTheNextDeletion() {
    trash(ANA, gid, ana);
    clock.advance(Duration.ofDays(20));
    restore(ANA).expect(204);
    trash(ANA, gid, ana);
    assertThat(trashView(ANA).at("/items/0/daysLeft").asInt()).isEqualTo(30);
    clock.advance(Duration.ofDays(20));
    assertThat(get(ANA, path()).expect(200).at("/state").asText()).isEqualTo("TRASHED");
  }

  @Test
  void EX_012_06_deletingAContactThatIsNotInThisGroupIsNotFound() {
    String other = group(BORIS, "Poroka");
    post(BORIS, "/groups/" + other + "/contacts/" + ana + "/trash", null)
        .expectError(404, "NOT_FOUND");
    post(ANA, "/groups/" + gid + "/contacts/" + UUID.randomUUID() + "/trash", null)
        .expectError(404, "NOT_FOUND");
    assertThat(openContact(ANA, gid, ana).path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void EX_012_07_afterErasureTheHistoryRemainsWithTheContactsDataReplaced() {
    JsonNode opened = openContact(MARKO, gid, ana);
    save(MARKO, gid, opened, "phone", "+386 41 555 123").expect(200);
    trash(ANA, gid, ana);
    erase(ANA).expect(204);
    JsonNode page = history(ANA, gid).expect(200).json();
    assertThat(page.path("items").size()).isGreaterThanOrEqualTo(4);
    for (JsonNode e : page.path("items")) {
      if (e.path("eventType").asText().startsWith("CONTACT")
          || e.path("eventType").asText().startsWith("TAG")
          || e.path("eventType").asText().startsWith("ATTRIBUTE")) {
        assertThat(e.path("erased").asBoolean()).as(e.toString()).isTrue();
      }
    }
    assertThat(page.toString())
        .doesNotContain(
            "Eva", "Horvat", "eva.horvat@example.com", "+386 41 555 123", "udelezenec2025");
    assertThat(page.toString()).contains("Marko Pirc");
  }

  @Test
  void EX_012_08_anErasedContactsOnlyTagIsNoLongerOffered() {
    assertThat(values(get(ANA, "/groups/" + gid + "/tags").expect(200).json(), "name"))
        .containsExactly("udelezenec2025");
    trash(ANA, gid, ana);
    erase(ANA).expect(204);
    assertThat(get(ANA, "/groups/" + gid + "/tags").expect(200).json()).isEmpty();
  }
}
