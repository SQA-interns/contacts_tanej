package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-28, T-29: own tests of US-009 for what the frozen tests cannot reach before US-012. */
class ContactSearchIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
  }

  @Test
  @DisplayName("R-009-07 AC-009-08: a contact in Trash is neither listed nor found")
  void trashedContactIsHidden() {
    JsonNode c = newContact(ANA, gid, "Ana", "Novak");
    newContact(ANA, gid, "Eva", "Horvat");
    jdbc.update(
        "UPDATE contacts SET state = 'TRASHED', trashed_at = now() WHERE id = ?",
        UUID.fromString(c.path("id").asText()));
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, gid, "novak").at("/total").asInt()).isZero();
  }

  @Test
  @DisplayName("R-009-07: a contact of another group is never listed")
  void otherGroupIsNeverListed() {
    newContact(ANA, gid, "Ana", "Novak");
    String other = group(BORIS, "Poroka");
    newContact(BORIS, other, "Boris", "Kralj");
    assertThat(rowValues(search(ANA, gid, ""), "lastName")).containsExactly("Novak");
    assertThat(search(ANA, gid, "kralj").at("/total").asInt()).isZero();
  }

  @Test
  @DisplayName("R-009-07: a person outside the group gets the same 404 as for a missing group")
  void nonMemberGetsNotFound() {
    post(BORIS, "/groups/" + gid + "/contacts/search", j()).expect(404);
    post(ANA, "/groups/" + UUID.randomUUID() + "/contacts/search", j()).expect(404);
  }

  @Test
  @DisplayName("R-009-02: a row carries its tags in the order of the name, and empty text as \"\"")
  void rowsCarryTags() {
    newContact(ANA, gid, j("lastName", "Novak", "tags", arr("beta", "Alfa")));
    JsonNode row = search(ANA, gid, "").at("/items/0");
    assertThat(row.path("tags")).hasSize(2);
    assertThat(row.at("/tags/0").asText()).isEqualTo("Alfa");
    assertThat(row.at("/tags/1").asText()).isEqualTo("beta");
    assertThat(row.path("firstName").asText()).isEmpty();
  }

  @Test
  @DisplayName("R-009-02: an unknown sort column is refused and nothing reaches SQL (SR-07)")
  void unknownSortColumnIsRefused() {
    newContact(ANA, gid, "Ana", "Novak");
    post(ANA, "/groups/" + gid + "/contacts/search", j("sort", j("field", "notes; DROP TABLE")))
        .expect(400);
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(1);
  }
}
