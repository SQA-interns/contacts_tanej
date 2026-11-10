package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.MARKO;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.domain.TrashedContact;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-33, T-34: own tests of US-012 for what the frozen tests do not look at. */
class TrashIT extends AcceptanceBase {

  private String gid;
  private String cid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    JsonNode c =
        newContact(
            ANA,
            gid,
            j(
                "firstName", "Eva",
                "lastName", "Horvat",
                "email", "eva.horvat@example.com",
                "phone", "+386 41 555 123",
                "notes", "secretnote",
                "tags", arr("onlytag"),
                "attributes", arr(attr("kompetenca", "javaskill"))));
    cid = c.path("id").asText();
  }

  private String path() {
    return "/groups/" + gid + "/contacts/" + cid;
  }

  @Test
  @DisplayName("SR-15 R-012-04: after Delete forever no personal data of the contact is left")
  void noPersonalDataSurvivesAnErasure() {
    save(ANA, gid, openContact(ANA, gid, cid), "company", "Acme").expect(200);
    post(ANA, path() + "/trash", null).expect(204);
    delete(ANA, path()).expect(204);
    for (String table :
        List.of(
            "contacts",
            "contact_versions",
            "attributes",
            "contact_tags",
            "tags",
            "history_entries",
            "history_changes")) {
      List<String> rows = jdbc.queryForList("SELECT t::text FROM " + table + " t", String.class);
      assertThat(String.join("|", rows))
          .as(table)
          .doesNotContain(
              "Eva",
              "Horvat",
              "eva.horvat",
              "555 123",
              "secretnote",
              "onlytag",
              "javaskill",
              "Acme");
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM history_entries WHERE contact_id = ? AND NOT erased",
                Integer.class,
                UUID.fromString(cid)))
        .isZero();
  }

  @Test
  @DisplayName("R-012-09: delete, restore and erase each write one History entry")
  void everyStepWritesOneEntry() {
    post(ANA, path() + "/trash", null).expect(204);
    post(MARKO, path() + "/restore", null).expect(204);
    post(ANA, path() + "/trash", null).expect(204);
    delete(ANA, path()).expect(204);
    List<String> types =
        jdbc.queryForList(
            "SELECT event_type FROM history_entries WHERE contact_id = ? ORDER BY created_at, id",
            String.class,
            UUID.fromString(cid));
    assertThat(types)
        .containsExactlyInAnyOrder(
            "CONTACT_CREATED",
            "TAG_CHANGED",
            "ATTRIBUTE_CHANGED",
            "CONTACT_DELETED",
            "CONTACT_RESTORED",
            "CONTACT_DELETED",
            "CONTACT_ERASED");
  }

  @Test
  @DisplayName("R-012-03 R-012-04: restore of an active contact and erase of an active one are 409")
  void stateErrors() {
    post(ANA, path() + "/restore", null).expectError(409, "NOT_IN_TRASH");
    delete(ANA, path()).expectError(409, "NOT_IN_TRASH");
    post(ANA, path() + "/trash", null).expect(204);
    post(ANA, path() + "/trash", null).expectError(409, "ALREADY_IN_TRASH");
  }

  @Test
  @DisplayName("R-012-01: trash and restore raise the revision so that an open edit conflicts")
  void restoreRaisesTheRevision() {
    JsonNode opened = openContact(ANA, gid, cid);
    post(ANA, path() + "/trash", null).expect(204);
    post(ANA, path() + "/restore", null).expect(204);
    save(ANA, gid, opened, "company", "Beta").expectError(409, "CONFLICT");
    assertThat(openContact(ANA, gid, cid).path("versionNumber").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName("R-012-02: days left is 30 minus the full days, never below zero")
  void daysLeft() {
    Instant t = Instant.parse("2026-01-01T10:00:00Z");
    assertThat(TrashedContact.daysLeft(t, t)).isEqualTo(30);
    assertThat(TrashedContact.daysLeft(t, t.plus(Duration.ofDays(29).plusHours(23)))).isEqualTo(1);
    assertThat(TrashedContact.daysLeft(t, t.plus(Duration.ofDays(30)))).isZero();
    assertThat(TrashedContact.daysLeft(t, t.plus(Duration.ofDays(45)))).isZero();
  }
}
