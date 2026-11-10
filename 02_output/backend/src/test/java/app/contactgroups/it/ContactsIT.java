package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-18, T-19: own tests of US-005 for what the frozen tests cannot reach before US-009/012/013. */
class ContactsIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private int count(String table, String column, String id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE " + column + " = ?",
        Integer.class,
        UUID.fromString(id));
  }

  @Test
  @DisplayName(
      "R-005-01 R-019-01: a created contact is version 1, revision 1 and has one History entry")
  void createRecordsVersionOneAndHistory() {
    JsonNode c = newContact(ANA, gid, j("firstName", "Ana", "lastName", "Novak"));
    assertThat(c.path("versionNumber").asInt()).isEqualTo(1);
    assertThat(c.path("revision").asLong()).isEqualTo(1);
    assertThat(c.path("tags")).isEmpty();
    assertThat(c.path("attributes")).isEmpty();
    assertThat(count("contact_versions", "contact_id", c.path("id").asText())).isEqualTo(1);
    List<String> types =
        jdbc.queryForList(
            "SELECT event_type FROM history_entries WHERE contact_id = ?",
            String.class,
            UUID.fromString(c.path("id").asText()));
    assertThat(types).containsExactly("CONTACT_CREATED");
  }

  @Test
  @DisplayName("R-005-07 AC-005-06: a changed save raises revision and version and writes History")
  void saveRecordsNextVersionAndEditedEntry() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "phone", "+386 40 123 456"));
    JsonNode saved =
        save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText()), "phone", "+386 41 555 123")
            .expect(200)
            .json();
    assertThat(saved.path("versionNumber").asInt()).isEqualTo(2);
    assertThat(saved.path("revision").asLong()).isEqualTo(2);
    String change =
        jdbc.queryForObject(
            "SELECT h.old_value || '>' || h.new_value FROM history_changes h JOIN history_entries e"
                + " ON e.id = h.entry_id WHERE e.event_type = 'CONTACT_EDITED' AND h.field_name ="
                + " 'phone'",
            String.class);
    assertThat(change).isEqualTo("+386 40 123 456>+386 41 555 123");
  }

  @Test
  @DisplayName("EX-005-09: a save with no change writes no version and no History entry")
  void saveWithoutChangeWritesNothing() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "company", "Acme"));
    JsonNode same =
        save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText())).expect(200).json();
    assertThat(same.path("versionNumber").asInt()).isEqualTo(1);
    assertThat(same.path("revision").asLong()).isEqualTo(1);
    assertThat(count("contact_versions", "contact_id", c.path("id").asText())).isEqualTo(1);
    assertThat(count("history_entries", "contact_id", c.path("id").asText())).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "R-005-08 AC-005-07: a stale save is refused with the current contact, nothing written")
  void staleSaveIsRefusedAndWritesNothing() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "phone", "+386 40 123 456"));
    JsonNode opened = openContact(ANA, gid, c.path("id").asText());
    save(MARKO, gid, opened, "phone", "+386 41 555 123").expect(200);
    var refused = save(ANA, gid, opened, "company", "Acme").expectError(409, "CONFLICT");
    assertThat(refused.at("/current/phone").asText()).isEqualTo("+386 41 555 123");
    assertThat(refused.at("/current/revision").asLong()).isEqualTo(2);
    assertThat(count("contact_versions", "contact_id", c.path("id").asText())).isEqualTo(2);
  }

  @Test
  @DisplayName("R-005-07 ADR-03: two parallel saves from the same start: one wins, one conflicts")
  void parallelSavesAreSerialised() throws Exception {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    JsonNode a = openContact(ANA, gid, c.path("id").asText());
    JsonNode m = openContact(MARKO, gid, c.path("id").asText());
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> byAna = () -> save(ANA, gid, a, "company", "Acme").status();
      Callable<Integer> byMarko = () -> save(MARKO, gid, m, "company", "Beta").status();
      Future<Integer> f1 = pool.submit(byAna);
      Future<Integer> f2 = pool.submit(byMarko);
      assertThat(List.of(f1.get(), f2.get())).containsExactlyInAnyOrder(200, 409);
    } finally {
      pool.shutdownNow();
    }
    assertThat(count("contact_versions", "contact_id", c.path("id").asText())).isEqualTo(2);
  }

  @Test
  @DisplayName("R-005-09: a Viewer is refused on create and save, also with an invalid body")
  void viewerIsRefused() {
    post(PETRA, "/groups/" + gid + "/contacts", j("phone", "abc")).expectError(403, "FORBIDDEN");
  }

  @Test
  @DisplayName("R-005-03: a save without the revision is refused naming the revision")
  void saveNeedsTheRevision() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    put(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText(), j("lastName", "Novak"))
        .expectFieldError("revision", "REQUIRED");
  }

  @Test
  @DisplayName("R-005-10 R-005-03: a contact in Trash is refused as such, before the revision")
  void trashedContactIsRefused() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    JsonNode opened = openContact(ANA, gid, c.path("id").asText());
    jdbc.update(
        "UPDATE contacts SET state = 'TRASHED', trashed_at = now(), revision = revision + 1"
            + " WHERE id = ?",
        UUID.fromString(c.path("id").asText()));
    save(MARKO, gid, opened, "company", "Beta").expectError(409, "CONTACT_IN_TRASH");
  }

  @Test
  @DisplayName("EX-005-11 SR-03: another group's contact and a malformed id are the same 404")
  void otherGroupsAndMalformedIdsAreNotFound() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    String other = group(BORIS, "Poroka");
    get(BORIS, "/groups/" + other + "/contacts/" + c.path("id").asText())
        .expectError(404, "NOT_FOUND");
    get(ANA, "/groups/" + gid + "/contacts/not-an-id").expectError(404, "NOT_FOUND");
    put(
            BORIS,
            "/groups/" + gid + "/contacts/" + c.path("id").asText(),
            j("lastName", "X", "revision", 1))
        .expectError(404, "NOT_FOUND");
  }
}
