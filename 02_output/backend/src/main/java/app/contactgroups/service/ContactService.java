package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.AttributeChange;
import app.contactgroups.domain.Attributes;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.ChangeSet.Event;
import app.contactgroups.domain.Contact;
import app.contactgroups.domain.Contact.Attribute;
import app.contactgroups.domain.ContactInput;
import app.contactgroups.domain.ContactRules;
import app.contactgroups.domain.ContactState;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.FieldProblem;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.domain.TagChange;
import app.contactgroups.domain.TagNames;
import app.contactgroups.domain.User;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create, open and save a contact (US-005). A save runs under the row lock of the contact: state,
 * revision and no-change checks, then the update, the version and the History entry in the one
 * transaction (ADR-03, ADR-05).
 */
@Service
public class ContactService {

  private final ContactRepository contacts;
  private final AccessService access;
  private final UserRepository users;
  private final VersionService versions;
  private final HistoryRecorder history;
  private final TagService tags;
  private final AttributeService attributes;
  private final Clock clock;

  public ContactService(
      ContactRepository contacts,
      AccessService access,
      UserRepository users,
      VersionService versions,
      HistoryRecorder history,
      TagService tags,
      AttributeService attributes,
      Clock clock) {
    this.contacts = contacts;
    this.tags = tags;
    this.attributes = attributes;
    this.access = access;
    this.users = users;
    this.versions = versions;
    this.history = history;
    this.clock = clock;
  }

  /** What a request carries besides the 11 fields; {@code null} means "keep as it is". */
  public record Extras(List<String> tags, List<Attribute> attributes) {}

  /** Stores a new contact as version 1, Owner or Editor (R-005-01). */
  @Transactional
  public Contact create(UUID groupId, String userId, ContactInput raw, Extras extras) {
    requireWriter(groupId, userId);
    ContactInput input = ContactRules.validate(raw);
    List<String> wantedTags = TagNames.validate(orEmpty(extras.tags()));
    List<Attribute> wantedAttributes = Attributes.validate(orEmpty(extras.attributes()));
    UUID id = UUID.randomUUID();
    contacts.insert(id, groupId, input, userId, clock.instant());
    TagChange tagChange = tags.apply(groupId, id, TagChange.between(List.of(), wantedTags));
    attributes.replace(id, wantedAttributes);
    Contact created = load(groupId, id);
    versions.record(created, 1, author(userId));
    List<Event> events = new ArrayList<>();
    events.add(
        Event.contact(
            HistoryEventType.CONTACT_CREATED, id, created.label(), input.changesFrom(null)));
    events.addAll(tagEvents(created, tagChange));
    events.addAll(attributeEvents(created, AttributeChange.between(List.of(), wantedAttributes)));
    history.record(new ChangeSet(UUID.randomUUID(), groupId, userId, events));
    return created;
  }

  /** The contact with its fields, tags and attributes, for every member (R-005-06). */
  @Transactional(readOnly = true)
  public Contact open(UUID groupId, String userId, UUID contactId) {
    access.requireMember(groupId, userId);
    return contacts
        .find(groupId, contactId)
        .map(this::withRelations)
        .orElseThrow(AccessService::notFound);
  }

  /**
   * Saves the contact as opened (revision), Owner or Editor (R-005-07, R-005-08). Tags and
   * attributes that are absent from the request stay as they are; a list replaces them (R-006-01,
   * R-006-06, R-008-01, R-008-07).
   */
  @Transactional
  public Contact save(
      UUID groupId, String userId, UUID contactId, ContactInput raw, Extras extras, Long revision) {
    requireWriter(groupId, userId);
    if (revision == null) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          "The request is not valid.",
          List.of(new FieldProblem("revision", "REQUIRED", "The revision of the opened contact.")));
    }
    ContactInput input = ContactRules.validate(raw);
    List<String> wantedTags = extras.tags() == null ? null : TagNames.validate(extras.tags());
    List<Attribute> wantedAttributes =
        extras.attributes() == null ? null : Attributes.validate(extras.attributes());
    Contact current =
        contacts
            .lock(groupId, contactId)
            .map(this::withRelations)
            .orElseThrow(AccessService::notFound);
    if (current.state() == ContactState.TRASHED) {
      throw new ApiException(ErrorCode.CONTACT_IN_TRASH, "This contact is in Trash.");
    }
    if (current.revision() != revision) {
      throw ApiException.conflict(current);
    }
    TagChange wantedChange =
        TagChange.between(current.tags(), wantedTags == null ? current.tags() : wantedTags);
    List<FieldChange> attributeChanges =
        AttributeChange.between(
            current.attributes(),
            wantedAttributes == null ? current.attributes() : wantedAttributes);
    if (current.fields().equals(input) && wantedChange.isEmpty() && attributeChanges.isEmpty()) {
      return current;
    }
    int number = current.versionNumber() + 1;
    contacts.update(contactId, input, number);
    TagChange tagChange = tags.apply(groupId, contactId, wantedChange);
    if (!attributeChanges.isEmpty()) {
      attributes.replace(contactId, wantedAttributes);
    }
    Contact saved = load(groupId, contactId);
    versions.record(saved, number, author(userId));
    history.record(
        new ChangeSet(
            UUID.randomUUID(),
            groupId,
            userId,
            savedEvents(current, saved, tagChange, attributeChanges)));
    return saved;
  }

  private List<Event> savedEvents(
      Contact before, Contact saved, TagChange tagChange, List<FieldChange> attributeChanges) {
    List<Event> events = new ArrayList<>();
    List<FieldChange> changes = saved.fields().changesFrom(before.fields());
    if (!changes.isEmpty()) {
      events.add(
          Event.contact(HistoryEventType.CONTACT_EDITED, saved.id(), saved.label(), changes));
    }
    events.addAll(tagEvents(saved, tagChange));
    events.addAll(attributeEvents(saved, attributeChanges));
    return events;
  }

  /** One TAG_CHANGED entry per tag added or removed (R-013-02). */
  private static List<Event> tagEvents(Contact contact, TagChange change) {
    List<Event> events = new ArrayList<>();
    for (String name : change.removed()) {
      events.add(
          Event.contact(
              HistoryEventType.TAG_CHANGED,
              contact.id(),
              contact.label(),
              List.of(new FieldChange("tag", name, null))));
    }
    for (String name : change.added()) {
      events.add(
          Event.contact(
              HistoryEventType.TAG_CHANGED,
              contact.id(),
              contact.label(),
              List.of(new FieldChange("tag", null, name))));
    }
    return events;
  }

  /** One ATTRIBUTE_CHANGED entry per key added, removed or changed (R-013-02). */
  private static List<Event> attributeEvents(Contact contact, List<FieldChange> changes) {
    return changes.stream()
        .map(
            change ->
                Event.contact(
                    HistoryEventType.ATTRIBUTE_CHANGED,
                    contact.id(),
                    contact.label(),
                    List.of(change)))
        .toList();
  }

  private static <T> List<T> orEmpty(List<T> list) {
    return list == null ? List.of() : list;
  }

  private Contact load(UUID groupId, UUID contactId) {
    return withRelations(contacts.find(groupId, contactId).orElseThrow());
  }

  private Contact withRelations(Contact contact) {
    return contact.withTags(tags.namesOf(contact.id())).withAttributes(attributes.of(contact.id()));
  }

  private void requireWriter(UUID groupId, String userId) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
  }

  private User author(String userId) {
    return users.find(userId).orElseThrow();
  }
}
