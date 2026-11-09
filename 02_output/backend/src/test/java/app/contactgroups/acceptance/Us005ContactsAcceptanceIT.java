package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-005 Create, view and edit a contact: AC-005-01 to AC-005-10, EX-005-01 to EX-005-12. */
class Us005ContactsAcceptanceIT extends AcceptanceBase {

  private String gid;

  @BeforeEach
  void scene() {
    gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    join(gid, ANA, PETRA, "VIEWER");
  }

  private Res create(Who who, ObjectNodeSupplier body) {
    return post(who, "/groups/" + gid + "/contacts", body.get());
  }

  private interface ObjectNodeSupplier {
    com.fasterxml.jackson.databind.node.ObjectNode get();
  }

  private long total() {
    return search(ANA, gid, "").at("/total").asLong();
  }

  @Test
  void AC_005_01_aNewContactIsStoredAndShownWithExactlyTheseValues() {
    JsonNode c =
        post(
                ANA,
                "/groups/" + gid + "/contacts",
                j(
                    "firstName", "Ana",
                    "lastName", "Novak",
                    "email", "ana.novak@example.com",
                    "company", "Acme"))
            .expect(201)
            .json();
    JsonNode opened = openContact(ANA, gid, c.path("id").asText());
    assertThat(opened.path("firstName").asText()).isEqualTo("Ana");
    assertThat(opened.path("lastName").asText()).isEqualTo("Novak");
    assertThat(opened.path("email").asText()).isEqualTo("ana.novak@example.com");
    assertThat(opened.path("company").asText()).isEqualTo("Acme");
    assertThat(opened.path("state").asText()).isEqualTo("ACTIVE");
  }

  @Test
  void AC_005_02_aContactWithOnlyAPhoneIsRefusedAndNothingIsStored() {
    post(ANA, "/groups/" + gid + "/contacts", j("phone", "+386 40 123 456"))
        .expectFieldError("firstName", "REQUIRED_ONE_OF");
    assertThat(total()).isZero();
  }

  @Test
  void AC_005_03_theEmailIsStoredInLowercase() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "email", "ANA.Novak@Example.com"));
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("email").asText())
        .isEqualTo("ana.novak@example.com");
  }

  @Test
  void AC_005_04_anInvalidEmailIsRefusedNamingTheFieldAndNothingIsStored() {
    post(ANA, "/groups/" + gid + "/contacts", j("lastName", "Novak", "email", "ana.novak@"))
        .expectFieldError("email", "INVALID_FORMAT");
    assertThat(total()).isZero();
  }

  @Test
  void AC_005_05_aSecondContactWithTheSameEmailIsAllowedWithoutWarning() {
    newContact(
        ANA, gid, j("firstName", "Ana", "lastName", "Novak", "email", "ana.novak@example.com"));
    Res second =
        post(
                ANA,
                "/groups/" + gid + "/contacts",
                j("lastName", "Novak", "email", "ana.novak@example.com"))
            .expect(201);
    assertThat(second.json().has("warning")).isFalse();
    assertThat(total()).isEqualTo(2);
  }

  @Test
  void AC_005_06_anEditorChangesThePhoneAndSeesTheNewPhone() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "phone", "+386 40 123 456"));
    JsonNode opened = openContact(MARKO, gid, c.path("id").asText());
    Res saved = save(MARKO, gid, opened, "phone", "+386 41 555 123").expect(200);
    assertThat(saved.at("/phone").asText()).isEqualTo("+386 41 555 123");
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("phone").asText())
        .isEqualTo("+386 41 555 123");
  }

  @Test
  void AC_005_07_theSecondOfTwoSavesFromTheSameStartIsRefusedAsConflict() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "phone", "+386 40 123 456"));
    JsonNode forMarko = openContact(MARKO, gid, c.path("id").asText());
    JsonNode forBoris = openContact(ANA, gid, c.path("id").asText());
    save(MARKO, gid, forMarko, "phone", "+386 41 555 123").expect(200);
    Res refused = save(ANA, gid, forBoris, "company", "Acme").expectError(409, "CONFLICT");
    assertThat(refused.at("/current/phone").asText()).isEqualTo("+386 41 555 123");
    JsonNode now = openContact(ANA, gid, c.path("id").asText());
    assertThat(now.path("phone").asText()).isEqualTo("+386 41 555 123");
    assertThat(now.path("company").asText()).isEmpty();
  }

  @Test
  void AC_005_08_aViewerSeesAllValuesAndCannotCreateOrSave() {
    JsonNode c =
        newContact(ANA, gid, j("firstName", "Ana", "lastName", "Novak", "company", "Acme"));
    JsonNode seen = openContact(PETRA, gid, c.path("id").asText());
    assertThat(seen.path("company").asText()).isEqualTo("Acme");
    save(PETRA, gid, seen, "company", "Evil").expectError(403, "FORBIDDEN");
    post(PETRA, "/groups/" + gid + "/contacts", j("lastName", "Nov")).expectError(403, "FORBIDDEN");
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("company").asText())
        .isEqualTo("Acme");
  }

  @Test
  void AC_005_09_aContactInTrashCannotBeSaved() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "company", "Acme"));
    JsonNode forMarko = openContact(MARKO, gid, c.path("id").asText());
    trash(ANA, gid, c.path("id").asText());
    save(MARKO, gid, forMarko, "company", "Beta").expectError(409, "CONTACT_IN_TRASH");
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("company").asText())
        .isEqualTo("Acme");
  }

  @Test
  void AC_005_10_aCountryOutsideTheListIsRefusedNamingTheField() {
    post(ANA, "/groups/" + gid + "/contacts", j("lastName", "Novak", "country", "Slovenija"))
        .expectFieldError("country", "UNKNOWN_COUNTRY");
    assertThat(total()).isZero();
  }

  @Test
  void EX_005_01_onlyACompanyIsEnough() {
    create(ANA, () -> j("company", "Acme")).expect(201);
  }

  @Test
  void EX_005_02_aFirstNameOfSpacesAloneIsRefused() {
    create(ANA, () -> j("firstName", "   ")).expectFieldError("firstName", "REQUIRED_ONE_OF");
    assertThat(total()).isZero();
  }

  @Test
  void EX_005_03_aTextFieldOfExactly255CharactersIsSaved() {
    String s = repeat("x", 255);
    JsonNode c =
        create(
                ANA,
                () ->
                    j(
                        "firstName",
                        s,
                        "lastName",
                        s,
                        "company",
                        s,
                        "jobTitle",
                        s,
                        "street",
                        s,
                        "postalCode",
                        s,
                        "city",
                        s))
            .expect(201)
            .json();
    assertThat(c.path("city").asText()).hasSize(255);
    assertThat(c.path("email").asText()).isEmpty();
  }

  @Test
  void EX_005_04_aTextFieldOf256CharactersIsRefusedNamingTheField() {
    String s = repeat("x", 256);
    for (String field :
        List.of("firstName", "lastName", "company", "jobTitle", "street", "postalCode", "city")) {
      create(ANA, () -> j("lastName", "Novak", field, s)).expectFieldError(field, "TOO_LONG");
    }
    assertThat(total()).isZero();
  }

  @Test
  void EX_005_05_notesOf5000AreSavedAndOf5001AreRefused() {
    create(ANA, () -> j("lastName", "Novak", "notes", repeat("n", 5000))).expect(201);
    create(ANA, () -> j("lastName", "Novak", "notes", repeat("n", 5001)))
        .expectFieldError("notes", "TOO_LONG");
    assertThat(total()).isEqualTo(1);
  }

  @Test
  void EX_005_06_aPhoneWithLettersIsRefused() {
    create(ANA, () -> j("lastName", "Novak", "phone", "abc"))
        .expectFieldError("phone", "INVALID_CHARACTERS");
  }

  @Test
  void EX_005_07_aPhoneIsSavedAsTyped() {
    JsonNode c =
        create(ANA, () -> j("lastName", "Novak", "phone", "040/123-456")).expect(201).json();
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("phone").asText())
        .isEqualTo("040/123-456");
  }

  @Test
  void EX_005_08_aCountryFromTheListIsSaved() {
    JsonNode c = create(ANA, () -> j("lastName", "Novak", "country", "SI")).expect(201).json();
    assertThat(openContact(ANA, gid, c.path("id").asText()).path("country").asText())
        .isEqualTo("SI");
  }

  @Test
  void EX_005_09_aSaveWithNoChangeWritesNothing() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "company", "Acme"));
    int entriesBefore = history(ANA, gid).expect(200).at("/total").asInt();
    Res saved = save(MARKO, gid, openContact(MARKO, gid, c.path("id").asText())).expect(200);
    assertThat(saved.at("/versionNumber").asInt()).isEqualTo(1);
    assertThat(history(ANA, gid).expect(200).at("/total").asInt()).isEqualTo(entriesBefore);
    assertThat(
            get(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText() + "/versions")
                .expect(200)
                .json())
        .hasSize(1);
  }

  @Test
  void EX_005_10_twoFailedFieldsAreBothNamedAndTheWholeSaveIsRefused() {
    Res r =
        create(ANA, () -> j("lastName", "Novak", "email", "ana.novak@", "country", "Slovenija"))
            .expectFieldError("email", "INVALID_FORMAT");
    r.expectFieldError("country", "UNKNOWN_COUNTRY");
    assertThat(total()).isZero();
  }

  @Test
  void EX_005_11_aContactOfAnotherGroupIsNotFound() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    String other = group(BORIS, "Poroka");
    get(BORIS, "/groups/" + gid + "/contacts/" + c.path("id").asText())
        .expectError(404, "NOT_FOUND");
    get(BORIS, "/groups/" + other + "/contacts/" + c.path("id").asText())
        .expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_005_12_aContactTrashedWhileEditingGivesASaveRefusedAsInTrash() {
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak"));
    JsonNode opened = openContact(MARKO, gid, c.path("id").asText());
    trash(ANA, gid, c.path("id").asText());
    save(MARKO, gid, opened, "company", "Acme").expectError(409, "CONTACT_IN_TRASH");
  }
}
