package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-021 Find duplicates (API part): AC-021-01 to AC-021-13, EX-021-01 to EX-021-08. */
class Us021DuplicatesAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private String contact(Object... pairs) {
    return newContact(MARKO, gid, j(pairs)).path("id").asText();
  }

  private JsonNode sets(Who who) {
    return post(who, "/groups/" + gid + "/duplicates/search", null).expect(200).at("/sets");
  }

  private static List<String> ids(JsonNode set) {
    List<String> out = new ArrayList<>();
    for (JsonNode c : set.path("contacts")) {
      out.add(c.path("id").asText());
    }
    return out;
  }

  private Res dismiss(Who who, String... contactIds) {
    return post(
        who,
        "/groups/" + gid + "/duplicates/dismissals",
        j("contactIds", arr((Object[]) contactIds)));
  }

  @Test
  void AC_021_01_theSameEmailIgnoringCaseIsACertainDuplicate() {
    String a = contact("lastName", "Novak", "email", "ana.novak@example.com");
    String b = contact("lastName", "Nova", "email", "ANA.novak@example.com");
    JsonNode s = sets(MARKO);
    assertThat(s).hasSize(1);
    assertThat(s.get(0).path("kind").asText()).isEqualTo("CERTAIN");
    assertThat(strings(s.get(0).path("reasons"))).containsExactly("SAME_EMAIL");
    assertThat(ids(s.get(0))).containsExactlyInAnyOrder(a, b);
  }

  @Test
  void AC_021_02_theSameNameAndCompanyIsAProbableDuplicate() {
    contact("firstName", "Ana", "lastName", "Novak", "company", "Acme");
    contact("firstName", "ana", "lastName", "novak", "company", "Acme");
    JsonNode s = sets(MARKO);
    assertThat(s).hasSize(1);
    assertThat(s.get(0).path("kind").asText()).isEqualTo("PROBABLE");
    assertThat(strings(s.get(0).path("reasons"))).containsExactly("SAME_NAME_AND_COMPANY");
  }

  @Test
  void AC_021_03_theSameNameAndPhoneIgnoringFormattingIsAProbableDuplicate() {
    contact("firstName", "Ana", "lastName", "Novak", "phone", "+386 41 555 123", "company", "Acme");
    contact(
        "firstName", "Ana", "lastName", "Novak", "phone", "+386 (41) 555-123", "company", "Beta");
    JsonNode s = sets(MARKO);
    assertThat(s).hasSize(1);
    assertThat(s.get(0).path("kind").asText()).isEqualTo("PROBABLE");
    assertThat(strings(s.get(0).path("reasons"))).containsExactly("SAME_NAME_AND_PHONE");
  }

  @Test
  void AC_021_04_theSameNameAloneIsNotReported() {
    contact("firstName", "Ana", "lastName", "Novak", "company", "Acme", "phone", "+386 41 555 123");
    contact("firstName", "Ana", "lastName", "Novak", "company", "Beta", "phone", "+386 40 000 000");
    assertThat(sets(MARKO)).isEmpty();
  }

  @Test
  void AC_021_05_contactsWithOnlyAPhoneAreNeverMatched() {
    // D-59: BR-05 refuses a contact with only a phone, so R-021-05 holds by BR-05;
    // EX-021-07 covers the case that can happen (company only).
    for (int i = 0; i < 2; i++) {
      post(MARKO, "/groups/" + gid + "/contacts", j("phone", "+386 41 555 123"))
          .expectFieldError("firstName", "REQUIRED_ONE_OF");
    }
    assertThat(sets(MARKO)).isEmpty();
  }

  @Test
  void AC_021_06_theSameEmailInAnotherGroupIsNotReported() {
    String other = group(BORIS, "Poroka");
    newContact(BORIS, other, j("lastName", "Novak", "email", "ana.novak@example.com"));
    contact("lastName", "Novak", "email", "ana.novak@example.com");
    assertThat(sets(MARKO)).isEmpty();
  }

  @Test
  void AC_021_07_theWholeGroupIsSearchedWhateverTheListFilter() {
    contact("lastName", "A", "email", "x@example.com");
    contact("lastName", "B", "email", "x@example.com");
    JsonNode filtered =
        search(MARKO, gid, j("conditions", arr(j("type", "TAGS_ALL", "tags", arr("x")))));
    assertThat(filtered.at("/total").asInt()).isZero();
    assertThat(sets(MARKO)).hasSize(1);
  }

  @Test
  void AC_021_08_aDismissedSetIsNotShownAgainAndNothingIsChanged() {
    String a = contact("lastName", "A", "email", "x@example.com");
    String b = contact("lastName", "B", "email", "x@example.com");
    int versionBefore = openContact(MARKO, gid, a).path("versionNumber").asInt();
    dismiss(MARKO, a, b).expect(204);
    assertThat(sets(MARKO)).isEmpty();
    assertThat(openContact(MARKO, gid, a).path("versionNumber").asInt()).isEqualTo(versionBefore);
    assertThat(openContact(MARKO, gid, b).path("state").asText()).isEqualTo("ACTIVE");
    assertThat(search(MARKO, gid, "").at("/total").asInt()).isEqualTo(2);
  }

  @Test
  void AC_021_09_aNewContactJoiningADismissedSetMakesItVisibleAgain() {
    String a = contact("lastName", "A", "email", "x@example.com");
    String b = contact("lastName", "B", "email", "x@example.com");
    dismiss(MARKO, a, b).expect(204);
    String c = contact("lastName", "C", "email", "x@example.com");
    JsonNode s = sets(MARKO);
    assertThat(s).hasSize(1);
    assertThat(ids(s.get(0))).containsExactlyInAnyOrder(a, b, c);
  }

  @Test
  void AC_021_10_editingADismissedContactDoesNotBringTheSetBack() {
    String a = contact("lastName", "A", "email", "x@example.com");
    String b = contact("lastName", "B", "email", "x@example.com");
    dismiss(MARKO, a, b).expect(204);
    save(MARKO, gid, openContact(MARKO, gid, a), "phone", "+386 41 555 123").expect(200);
    assertThat(sets(MARKO)).isEmpty();
    assertThat(b).isNotBlank();
  }

  @Test
  void AC_021_11_aViewerCannotFindDuplicatesOrDismiss() {
    String a = contact("lastName", "A", "email", "x@example.com");
    String b = contact("lastName", "B", "email", "x@example.com");
    post(PETRA, "/groups/" + gid + "/duplicates/search", null).expectError(403, "FORBIDDEN");
    dismiss(PETRA, a, b).expectError(403, "FORBIDDEN");
    assertThat(sets(MARKO)).hasSize(1);
  }

  @Test
  void AC_021_12_withoutDuplicatesTheResultIsEmpty() {
    contact("lastName", "A", "email", "a@example.com");
    contact("lastName", "B", "email", "b@example.com");
    assertThat(sets(MARKO)).isEmpty();
  }

  @Test
  void AC_021_13_aContactInTrashIsInNoSet() {
    String a = contact("lastName", "A", "email", "x@example.com");
    String b = contact("lastName", "B", "email", "x@example.com");
    trash(MARKO, gid, b);
    assertThat(sets(MARKO)).isEmpty();
    assertThat(a).isNotBlank();
  }

  @Test
  void EX_021_01_namesDifferingOnlyInAccentsWithTheSameCompanyAreProbable() {
    contact("firstName", "Ana", "lastName", "Škerlj", "company", "Acme");
    contact("firstName", "Ana", "lastName", "Skerlj", "company", "Acme");
    assertThat(sets(MARKO)).hasSize(1);
  }

  @Test
  void EX_021_02_aSimilarSpellingIsNotReported() {
    contact("firstName", "Ana", "lastName", "Novak", "company", "Acme");
    contact("firstName", "Ana", "lastName", "Nowak", "company", "Acme");
    assertThat(sets(MARKO)).isEmpty();
  }

  @Test
  void EX_021_03_emptyEmailsAreNoEmailMatch() {
    contact("firstName", "Ana", "lastName", "Novak");
    contact("firstName", "Eva", "lastName", "Horvat");
    assertThat(sets(MARKO)).isEmpty();
  }

  @Test
  void EX_021_04_aChainOfMatchesGivesOneSetMarkedCertain() {
    String a =
        contact(
            "firstName", "Ana", "lastName", "Novak", "email", "x@example.com", "company", "Acme");
    String b =
        contact(
            "firstName", "Ana", "lastName", "Novak", "email", "x@example.com", "company", "Beta");
    String c = contact("firstName", "Ana", "lastName", "Novak", "company", "Beta");
    JsonNode s = sets(MARKO);
    assertThat(s).hasSize(1);
    assertThat(s.get(0).path("kind").asText()).isEqualTo("CERTAIN");
    assertThat(ids(s.get(0))).containsExactlyInAnyOrder(a, b, c);
  }

  @Test
  void EX_021_05_aSetThatLosesAMemberIsADifferentSetAndIsShown() {
    String a = contact("lastName", "A", "email", "x@example.com");
    String b = contact("lastName", "B", "email", "x@example.com");
    String c = contact("lastName", "C", "email", "x@example.com");
    dismiss(MARKO, a, b, c).expect(204);
    assertThat(sets(MARKO)).isEmpty();
    trash(MARKO, gid, b);
    JsonNode s = sets(MARKO);
    assertThat(s).hasSize(1);
    assertThat(ids(s.get(0))).containsExactlyInAnyOrder(a, c);
  }

  @Test
  void EX_021_06_twoEmptyFirstNamesWithTheSameLastNameAndCompanyAreNotReported() {
    contact("lastName", "Novak", "company", "Acme");
    contact("lastName", "Novak", "company", "Acme");
    assertThat(sets(MARKO)).isEmpty();
  }

  @Test
  void EX_021_07_contactsWithOnlyTheSameCompanyAreNeverMatched() {
    contact("company", "Acme");
    contact("company", "Acme");
    assertThat(sets(MARKO)).isEmpty();
  }

  @Test
  void EX_021_08_phonesWithDifferentDigitsAreNotTheSame() {
    contact("firstName", "Ana", "lastName", "Novak", "phone", "+386 41 555 123");
    contact("firstName", "Ana", "lastName", "Novak", "phone", "041 555 123");
    assertThat(sets(MARKO)).isEmpty();
  }
}
