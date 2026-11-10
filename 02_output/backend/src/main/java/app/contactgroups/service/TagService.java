package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.ChangeSet.Event;
import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.domain.Tag;
import app.contactgroups.domain.TagChange;
import app.contactgroups.domain.TagNames;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.TagRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Tags of the contacts of a group (US-006): the save path and the suggestions. */
@Service
public class TagService {

  private final TagRepository tags;
  private final AccessService access;
  private final ContactRepository contacts;
  private final HistoryRecorder history;

  public TagService(
      TagRepository tags,
      AccessService access,
      ContactRepository contacts,
      HistoryRecorder history) {
    this.tags = tags;
    this.access = access;
    this.contacts = contacts;
    this.history = history;
  }

  public List<String> namesOf(UUID contactId) {
    return tags.namesOf(contactId);
  }

  /**
   * Writes the change on the contact, inside the save of the contact. Returns the change with the
   * added tags in their stored spelling (R-006-02).
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public TagChange apply(UUID groupId, UUID contactId, TagChange change) {
    if (change.isEmpty()) {
      return change;
    }
    tags.lockGroupTags(groupId);
    change.removed().forEach(name -> tags.detach(groupId, contactId, name));
    List<String> added =
        change.added().stream().map(name -> tags.attach(groupId, contactId, name)).toList();
    return new TagChange(added, change.removed());
  }

  /**
   * Renames the tag on every contact of the group, also in Trash, Owner or Editor (R-007-01 to
   * R-007-04, R-007-08). No version is recorded; the contacts' revision grows (R-007-06, D-19).
   */
  @Transactional
  public Tag rename(UUID groupId, String userId, UUID tagId, String rawName) {
    requireWriter(groupId, userId);
    String name = TagNames.validateName(rawName);
    Tag tag = tags.find(groupId, tagId).orElseThrow(AccessService::notFound);
    if (tag.name().equals(name)) {
      return tag;
    }
    if (tags.nameTakenByOther(groupId, tagId, name)) {
      throw taken();
    }
    List<Contact> affected = contacts.lockWithTag(tagId);
    if (!renamed(tagId, name)) {
      throw AccessService.notFound();
    }
    contacts.touch(affected.stream().map(Contact::id).toList());
    recordEach(
        groupId,
        userId,
        affected,
        HistoryEventType.TAG_RENAMED,
        new FieldChange("tag", tag.name(), name));
    return new Tag(tagId, name, affected.size());
  }

  /** Takes the tag off every contact of the group; no contact is deleted (R-007-05, R-007-08). */
  @Transactional
  public void delete(UUID groupId, String userId, UUID tagId) {
    requireWriter(groupId, userId);
    Tag tag = tags.find(groupId, tagId).orElseThrow(AccessService::notFound);
    List<Contact> affected = contacts.lockWithTag(tagId);
    if (!tags.delete(tagId)) {
      throw AccessService.notFound();
    }
    contacts.touch(affected.stream().map(Contact::id).toList());
    recordEach(
        groupId,
        userId,
        affected,
        HistoryEventType.TAG_DELETED,
        new FieldChange("tag", tag.name(), null));
  }

  private void recordEach(
      UUID groupId,
      String userId,
      List<Contact> affected,
      HistoryEventType type,
      FieldChange change) {
    List<Event> events =
        affected.stream()
            .map(c -> Event.contact(type, c.id(), c.label(), List.of(change)))
            .toList();
    history.record(ChangeSet.of(groupId, userId, events.toArray(Event[]::new)));
  }

  /** A save that added the same name at the same moment loses to the unique index: 409. */
  private boolean renamed(UUID tagId, String name) {
    try {
      return tags.rename(tagId, name);
    } catch (DuplicateKeyException e) {
      ApiException refusal = taken();
      refusal.initCause(e);
      throw refusal;
    }
  }

  private void requireWriter(UUID groupId, String userId) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
  }

  private static ApiException taken() {
    return new ApiException(ErrorCode.TAG_NAME_TAKEN, "The group has a tag of this name.");
  }

  /** The tags of the group matching the typed text, for every member (R-006-10). */
  @Transactional(readOnly = true)
  public List<Tag> list(UUID groupId, String userId, String part) {
    access.requireMember(groupId, userId);
    return tags.findByGroup(groupId, part);
  }
}
