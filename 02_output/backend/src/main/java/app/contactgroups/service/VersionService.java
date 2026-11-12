package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.AttributeChange;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.ChangeSet.Event;
import app.contactgroups.domain.Contact;
import app.contactgroups.domain.Contact.Attribute;
import app.contactgroups.domain.ContactInput;
import app.contactgroups.domain.ContactState;
import app.contactgroups.domain.ContactVersion;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.FieldProblem;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.domain.TagChange;
import app.contactgroups.domain.User;
import app.contactgroups.domain.VersionDetail;
import app.contactgroups.domain.VersionSummary;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.UserRepository;
import app.contactgroups.repository.VersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the versions of a contact (US-019, ADR-05). The number is the one the caller computed
 * under the row lock of the contact, so it is never reused or lowered (R-019-05).
 */
@Service
public class VersionService {

  private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
  private static final TypeReference<List<Attribute>> ATTRIBUTES = new TypeReference<>() {};

  private final VersionRepository versions;
  private final ContactRepository contacts;
  private final AccessService access;
  private final TagService tags;
  private final AttributeService attributes;
  private final HistoryRecorder history;
  private final UserRepository users;
  private final ObjectMapper json;
  private final Clock clock;

  public VersionService(
      VersionRepository versions,
      ContactRepository contacts,
      AccessService access,
      TagService tags,
      AttributeService attributes,
      HistoryRecorder history,
      UserRepository users,
      ObjectMapper json,
      Clock clock) {
    this.versions = versions;
    this.contacts = contacts;
    this.access = access;
    this.tags = tags;
    this.attributes = attributes;
    this.history = history;
    this.users = users;
    this.json = json;
    this.clock = clock;
  }

  /** The versions of a contact, newest first, for an Owner or Editor (R-019-06). */
  @Transactional(readOnly = true)
  public List<VersionSummary> list(UUID groupId, String userId, UUID contactId) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
    contacts.find(groupId, contactId).orElseThrow(AccessService::notFound);
    return versions.list(contactId);
  }

  /** One version with its snapshot, for an Owner or Editor (R-020-02, R-020-07). */
  @Transactional(readOnly = true)
  public VersionDetail get(UUID groupId, String userId, UUID contactId, int number) {
    requireWriter(groupId, userId);
    contacts.find(groupId, contactId).orElseThrow(AccessService::notFound);
    ContactVersion v = versions.find(contactId, number).orElseThrow(AccessService::notFound);
    return new VersionDetail(v.number(), v.userName(), v.createdAt(), readTree(v.snapshot()));
  }

  /**
   * Makes the contact equal to version {@code number} as a new version (R-020-04). Runs under the
   * row lock: Trash, revision and no-change checks, then the update, the version and one {@code
   * VERSION_RESTORED} entry in the one transaction.
   */
  @Transactional
  public Contact restore(UUID groupId, String userId, UUID contactId, int number, Long revision) {
    requireWriter(groupId, userId);
    if (revision == null) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          "The request is not valid.",
          List.of(new FieldProblem("revision", "REQUIRED", "The revision of the opened contact.")));
    }
    Contact current =
        contacts
            .lock(groupId, contactId)
            .map(this::withRelations)
            .orElseThrow(AccessService::notFound);
    ContactVersion version = versions.find(contactId, number).orElseThrow(AccessService::notFound);
    if (current.state() == ContactState.TRASHED) {
      throw new ApiException(ErrorCode.CONTACT_IN_TRASH, "This contact is in Trash.");
    }
    if (current.revision() != revision) {
      throw ApiException.conflict(current);
    }
    JsonNode snapshot = readTree(version.snapshot());
    ContactInput wanted = json.convertValue(snapshot, ContactInput.class);
    List<String> wantedTags = json.convertValue(snapshot.path("tags"), STRINGS);
    List<Attribute> wantedAttributes = json.convertValue(snapshot.path("attributes"), ATTRIBUTES);
    TagChange wantedChange = TagChange.between(current.tags(), wantedTags);
    List<FieldChange> attributeChanges =
        AttributeChange.between(current.attributes(), wantedAttributes);
    if (current.fields().equals(wanted) && wantedChange.isEmpty() && attributeChanges.isEmpty()) {
      throw new ApiException(ErrorCode.NOTHING_TO_CHANGE, "The contact is already like this.");
    }
    int next = current.versionNumber() + 1;
    contacts.update(contactId, wanted, next);
    TagChange tagChange = tags.apply(groupId, contactId, wantedChange);
    if (!attributeChanges.isEmpty()) {
      attributes.replace(contactId, wantedAttributes);
    }
    Contact restored = withRelations(contacts.find(groupId, contactId).orElseThrow());
    User author = users.find(userId).orElseThrow();
    record(restored, next, author);
    List<FieldChange> changes = new ArrayList<>();
    changes.add(new FieldChange("version", null, String.valueOf(number)));
    changes.addAll(restored.fields().changesFrom(current.fields()));
    tagChange.removed().forEach(t -> changes.add(new FieldChange("tag", t, null)));
    tagChange.added().forEach(t -> changes.add(new FieldChange("tag", null, t)));
    changes.addAll(attributeChanges);
    history.record(
        ChangeSet.of(
            groupId,
            userId,
            Event.contact(
                HistoryEventType.VERSION_RESTORED, contactId, restored.label(), changes)));
    return restored;
  }

  private Contact withRelations(Contact contact) {
    return contact.withTags(tags.namesOf(contact.id())).withAttributes(attributes.of(contact.id()));
  }

  private void requireWriter(UUID groupId, String userId) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
  }

  private JsonNode readTree(String snapshot) {
    try {
      return json.readTree(snapshot);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("A stored snapshot is not JSON", e);
    }
  }

  /** Stores the contact as version {@code number}; the name of the author stays with it. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Contact contact, int number, User author) {
    ObjectNode snapshot = json.valueToTree(contact.fields());
    snapshot.set("tags", json.valueToTree(contact.tags()));
    snapshot.set("attributes", json.valueToTree(contact.attributes()));
    versions.insert(
        new ContactVersion(
            contact.id(),
            number,
            snapshot.toString(),
            author.id(),
            author.name(),
            clock.instant()));
  }
}
