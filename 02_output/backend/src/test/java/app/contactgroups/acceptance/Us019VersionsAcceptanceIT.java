package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-019 Versions are recorded: AC-019-01 to AC-019-08, EX-019-01 to EX-019-04. */
class Us019VersionsAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private JsonNode ana() {
    return newContact(
        ANA, gid, j("firstName", "Ana", "lastName", "Novak", "phone", "+386 40 123 456"));
  }

  private JsonNode versions(Who who, JsonNode contact) {
    return get(who, "/groups/" + gid + "/contacts/" + contact.path("id").asText() + "/versions")
        .expect(200)
        .json();
  }

  private JsonNode current(JsonNode contact) {
    return openContact(ANA, gid, contact.path("id").asText());
  }

  @Test
  void AC_019_01_aNewContactHasVersionOneByItsCreator() {
    JsonNode c = ana();
    JsonNode v = versions(ANA, c);
    assertThat(v).hasSize(1);
    assertThat(v.get(0).path("number").asInt()).isEqualTo(1);
    assertThat(v.get(0).path("userName").asText()).isEqualTo("Ana Novak");
    assertThat(c.path("versionNumber").asInt()).isEqualTo(1);
  }

  @Test
  void AC_019_02_aChangedFieldRecordsTheNextVersionByTheEditor() {
    JsonNode c = ana();
    save(MARKO, gid, current(c), "phone", "+386 41 555 123").expect(200);
    JsonNode v = versions(ANA, c);
    assertThat(v).hasSize(2);
    assertThat(v.get(0).path("number").asInt()).isEqualTo(2);
    assertThat(v.get(0).path("userName").asText()).isEqualTo("Marko Pirc");
  }

  @Test
  void AC_019_03_aSaveWithNoChangeRecordsNoVersion() {
    JsonNode c = ana();
    save(MARKO, gid, current(c), "phone", "+386 41 555 123").expect(200);
    save(MARKO, gid, current(c)).expect(200);
    assertThat(versions(ANA, c)).hasSize(2);
    assertThat(current(c).path("versionNumber").asInt()).isEqualTo(2);
  }

  @Test
  void AC_019_04_anAddedTagRecordsAVersion() {
    JsonNode c = ana();
    save(MARKO, gid, current(c), "phone", "+386 41 555 123").expect(200);
    save(MARKO, gid, current(c), "tags", arr("vip")).expect(200);
    assertThat(versions(ANA, c)).hasSize(3);
    assertThat(current(c).path("versionNumber").asInt()).isEqualTo(3);
  }

  @Test
  void AC_019_05_renamingATagForTheGroupRecordsNoVersion() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "tags", arr("old")));
    String tagId = get(ANA, "/groups/" + gid + "/tags").expect(200).at("/0/id").asText();
    patch(ANA, "/groups/" + gid + "/tags/" + tagId, j("name", "new")).expect(200);
    assertThat(versions(ANA, c)).hasSize(1);
  }

  @Test
  void AC_019_06_aViewerCannotRequestTheVersionList() {
    JsonNode c = ana();
    get(PETRA, "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/versions")
        .expectError(403, "FORBIDDEN");
  }

  @Test
  void AC_019_07_erasingTheContactErasesItsVersions() {
    JsonNode c = ana();
    save(MARKO, gid, current(c), "phone", "+386 41 555 123").expect(200);
    save(MARKO, gid, current(c), "tags", arr("vip")).expect(200);
    String id = c.path("id").asText();
    trash(ANA, gid, id);
    delete(ANA, "/groups/" + gid + "/contacts/" + id).expect(204);
    get(ANA, "/groups/" + gid + "/contacts/" + id + "/versions").expectError(404, "NOT_FOUND");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM contact_versions", Integer.class))
        .isZero();
  }

  @Test
  void AC_019_08_aRefusedConflictingSaveRecordsNoVersion() {
    JsonNode c = ana();
    JsonNode stale = current(c);
    save(MARKO, gid, stale, "phone", "+386 41 555 123").expect(200);
    save(ANA, gid, stale, "company", "Acme").expectError(409, "CONFLICT");
    assertThat(versions(ANA, c)).hasSize(2);
  }

  @Test
  void EX_019_01_500ChangesGive501VersionsInOrder() {
    JsonNode c = ana();
    JsonNode now = current(c);
    for (int i = 1; i <= 500; i++) {
      now = save(ANA, gid, now, "company", "Firma " + i).expect(200).json();
    }
    JsonNode v = versions(ANA, c);
    assertThat(v).hasSize(501);
    assertThat(v.get(0).path("number").asInt()).isEqualTo(501);
    assertThat(v.get(500).path("number").asInt()).isEqualTo(1);
    assertThat(values(v, "number")).hasSize(501);
  }

  @Test
  void EX_019_02_trashingAndRestoringRecordsNoVersion() {
    JsonNode c = ana();
    String id = c.path("id").asText();
    trash(ANA, gid, id);
    post(ANA, "/groups/" + gid + "/contacts/" + id + "/restore", null).expect(204);
    assertThat(versions(ANA, c)).hasSize(1);
    assertThat(current(c).path("versionNumber").asInt()).isEqualTo(1);
  }

  @Test
  void EX_019_03_aVersionKeepsTheNameOfAnAuthorWhoLeftTheProduct() {
    JsonNode c = ana();
    save(MARKO, gid, current(c), "phone", "+386 41 555 123").expect(200);
    delete(MARKO, "/me").expect(204);
    JsonNode v = versions(ANA, c);
    assertThat(v.get(0).path("userName").asText()).isEqualTo("Marko Pirc");
    assertThat(values(v, "number")).isEqualTo(List.of("2", "1"));
  }

  @Test
  void EX_019_04_aSaveRefusedByValidationRecordsNoVersion() {
    JsonNode c = ana();
    save(ANA, gid, current(c), "email", "ana.novak@").expect(400);
    assertThat(versions(ANA, c)).hasSize(1);
  }
}
