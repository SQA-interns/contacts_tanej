package app.contactgroups.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A contact as the API shows it (contract {@code Contact}): the 11 fields, tags, attributes, the
 * state and the two counters. {@code revision} is the change counter of the conflict check (D-19);
 * {@code versionNumber} is the current version number (US-019).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Contact(
    UUID id,
    @JsonIgnore UUID groupId,
    ContactState state,
    Instant trashedAt,
    Instant createdAt,
    long revision,
    int versionNumber,
    @JsonUnwrapped ContactInput fields,
    List<String> tags,
    List<Attribute> attributes) {

  /** One attribute of a contact (contract {@code Attribute}). */
  public record Attribute(String key, String value) {}

  public Contact withTags(List<String> names) {
    return new Contact(
        id,
        groupId,
        state,
        trashedAt,
        createdAt,
        revision,
        versionNumber,
        fields,
        names,
        attributes);
  }

  public Contact withAttributes(List<Attribute> list) {
    return new Contact(
        id, groupId, state, trashedAt, createdAt, revision, versionNumber, fields, tags, list);
  }

  /** The name shown for the contact in History: its name, else its e-mail, else its company. */
  @JsonIgnore
  public String label() {
    String name = (fields.firstName() + " " + fields.lastName()).strip();
    if (!name.isEmpty()) {
      return name;
    }
    return fields.email().isEmpty() ? fields.company() : fields.email();
  }
}
