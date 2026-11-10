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

/** T-31: the companies of a group offered by the company condition (R-010-07). */
class CompanySuggestionsIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
  }

  private JsonNode companies(String query) {
    return get(ANA, "/groups/" + gid + "/companies" + query).expect(200).json();
  }

  @Test
  @DisplayName("R-010-07: each company once, sorted, empty ones left out")
  void listsEachCompanyOnce() {
    newContact(ANA, gid, j("lastName", "A", "company", "Šola"));
    newContact(ANA, gid, j("lastName", "B", "company", "Acme"));
    newContact(ANA, gid, j("lastName", "C", "company", "acme"));
    newContact(ANA, gid, j("lastName", "D"));
    JsonNode all = companies("");
    assertThat(all).hasSize(2);
    assertThat(all.get(0).asText()).isEqualToIgnoringCase("Acme");
    assertThat(all.get(1).asText()).isEqualTo("Šola");
  }

  @Test
  @DisplayName("R-010-07: the typed text narrows the list, ignoring case and accents")
  void typedTextNarrows() {
    newContact(ANA, gid, j("lastName", "A", "company", "Šola"));
    newContact(ANA, gid, j("lastName", "B", "company", "Acme"));
    JsonNode found = companies("?q=SOL");
    assertThat(found).hasSize(1);
    assertThat(found.get(0).asText()).isEqualTo("Šola");
  }

  @Test
  @DisplayName("R-010-07: a company of Trash or of another group is not offered")
  void onlyActiveContactsOfTheGroup() {
    JsonNode c = newContact(ANA, gid, j("lastName", "A", "company", "Zaprta"));
    jdbc.update(
        "UPDATE contacts SET state = 'TRASHED', trashed_at = now() WHERE id = ?",
        UUID.fromString(c.path("id").asText()));
    String other = group(BORIS, "Poroka");
    newContact(BORIS, other, j("lastName", "T", "company", "Tuja"));
    assertThat(companies("")).isEmpty();
  }

  @Test
  @DisplayName("R-010-07: a person outside the group gets 404")
  void nonMemberGetsNotFound() {
    get(BORIS, "/groups/" + gid + "/companies").expect(404);
  }
}
