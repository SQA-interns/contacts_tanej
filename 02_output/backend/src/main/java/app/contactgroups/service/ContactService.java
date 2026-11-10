package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactInput;
import app.contactgroups.domain.ContactRules;
import app.contactgroups.domain.ContactState;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldProblem;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.domain.User;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.UserRepository;
import java.time.Clock;
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
  private final Clock clock;

  public ContactService(
      ContactRepository contacts,
      AccessService access,
      UserRepository users,
      VersionService versions,
      HistoryRecorder history,
      Clock clock) {
    this.contacts = contacts;
    this.access = access;
    this.users = users;
    this.versions = versions;
    this.history = history;
    this.clock = clock;
  }

  /** Stores a new contact as version 1, Owner or Editor (R-005-01). */
  @Transactional
  public Contact create(UUID groupId, String userId, ContactInput raw) {
    requireWriter(groupId, userId);
    ContactInput input = ContactRules.validate(raw);
    UUID id = UUID.randomUUID();
    contacts.insert(id, groupId, input, userId, clock.instant());
    Contact created = contacts.find(groupId, id).orElseThrow();
    versions.record(created, 1, author(userId));
    history.record(
        ChangeSet.of(
            groupId,
            userId,
            ChangeSet.Event.contact(
                HistoryEventType.CONTACT_CREATED, id, created.label(), input.changesFrom(null))));
    return created;
  }

  /** The contact with its fields, tags and attributes, for every member (R-005-06). */
  @Transactional(readOnly = true)
  public Contact open(UUID groupId, String userId, UUID contactId) {
    access.requireMember(groupId, userId);
    return contacts.find(groupId, contactId).orElseThrow(AccessService::notFound);
  }

  /** Saves the contact as opened (revision), Owner or Editor (R-005-07, R-005-08). */
  @Transactional
  public Contact save(
      UUID groupId, String userId, UUID contactId, ContactInput raw, Long revision) {
    requireWriter(groupId, userId);
    if (revision == null) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          "The request is not valid.",
          List.of(new FieldProblem("revision", "REQUIRED", "The revision of the opened contact.")));
    }
    ContactInput input = ContactRules.validate(raw);
    Contact current = contacts.lock(groupId, contactId).orElseThrow(AccessService::notFound);
    if (current.state() == ContactState.TRASHED) {
      throw new ApiException(ErrorCode.CONTACT_IN_TRASH, "This contact is in Trash.");
    }
    if (current.revision() != revision) {
      throw ApiException.conflict(current);
    }
    if (current.fields().equals(input)) {
      return current;
    }
    int number = current.versionNumber() + 1;
    contacts.update(contactId, input, number);
    Contact saved = contacts.find(groupId, contactId).orElseThrow();
    versions.record(saved, number, author(userId));
    history.record(
        ChangeSet.of(
            groupId,
            userId,
            ChangeSet.Event.contact(
                HistoryEventType.CONTACT_EDITED,
                contactId,
                saved.label(),
                input.changesFrom(current.fields()))));
    return saved;
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
