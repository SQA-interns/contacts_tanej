package app.contactgroups.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 11 predefined fields of a contact (glossary, BR-05). Empty text and absent text are the same,
 * so a field is never null; {@link ContactRules#validate} returns the stored form.
 */
public record ContactInput(
    String firstName,
    String lastName,
    String email,
    String phone,
    String company,
    String jobTitle,
    String street,
    String postalCode,
    String city,
    String country,
    String notes) {

  public ContactInput {
    firstName = orEmpty(firstName);
    lastName = orEmpty(lastName);
    email = orEmpty(email);
    phone = orEmpty(phone);
    company = orEmpty(company);
    jobTitle = orEmpty(jobTitle);
    street = orEmpty(street);
    postalCode = orEmpty(postalCode);
    city = orEmpty(city);
    country = orEmpty(country);
    notes = orEmpty(notes);
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  /**
   * The fields that differ from {@code before} (all non-empty fields when {@code before} is null),
   * in the order of the form, for the History entry (R-013-07); empty text is shown as null.
   */
  public List<FieldChange> changesFrom(ContactInput before) {
    Map<String, String> now = asMap();
    Map<String, String> old = before == null ? Map.of() : before.asMap();
    List<FieldChange> changes = new ArrayList<>();
    now.forEach(
        (field, value) -> {
          String was = old.getOrDefault(field, "");
          if (!was.equals(value)) {
            changes.add(new FieldChange(field, blankToNull(was), blankToNull(value)));
          }
        });
    return changes;
  }

  private static String blankToNull(String value) {
    return value.isEmpty() ? null : value;
  }

  private Map<String, String> asMap() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("firstName", firstName);
    m.put("lastName", lastName);
    m.put("email", email);
    m.put("phone", phone);
    m.put("company", company);
    m.put("jobTitle", jobTitle);
    m.put("street", street);
    m.put("postalCode", postalCode);
    m.put("city", city);
    m.put("country", country);
    m.put("notes", notes);
    return m;
  }

  public ContactInput withPhone(String value) {
    return new ContactInput(
        firstName,
        lastName,
        email,
        value,
        company,
        jobTitle,
        street,
        postalCode,
        city,
        country,
        notes);
  }

  public ContactInput withCountry(String value) {
    return new ContactInput(
        firstName,
        lastName,
        email,
        phone,
        company,
        jobTitle,
        street,
        postalCode,
        city,
        value,
        notes);
  }

  public ContactInput withNotes(String value) {
    return new ContactInput(
        firstName,
        lastName,
        email,
        phone,
        company,
        jobTitle,
        street,
        postalCode,
        city,
        country,
        value);
  }
}
