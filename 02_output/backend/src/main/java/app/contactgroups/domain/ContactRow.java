package app.contactgroups.domain;

import java.util.List;
import java.util.UUID;

/** One line of the contact list (contract {@code ContactRow}); empty text is "". */
public record ContactRow(
    UUID id,
    String lastName,
    String firstName,
    String email,
    String phone,
    String company,
    String city,
    String country,
    List<String> tags) {

  public ContactRow withTags(List<String> names) {
    return new ContactRow(id, lastName, firstName, email, phone, company, city, country, names);
  }
}
