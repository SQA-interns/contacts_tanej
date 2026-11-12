package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.AttributeChange;
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
import app.contactgroups.domain.MergeCombiner;
import app.contactgroups.domain.Role;
import app.contactgroups.domain.TagChange;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.MergeRepository;
import app.contactgroups.repository.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merges two or more contacts into the oldest (US-022, ADR-11). Everything runs in one transaction
 * under the row locks of the picked contacts, so a refusal changes nothing.
 */
@Service
public class MergeService {

  /**
   * One picked contact with the revision the merge screen showed (contract {@code MergeRequest}).
   */
  public record Pick(UUID id, Long revision) {}

  private final ContactRepository contacts;
  private final MergeRepository merges;
  private final AccessService access;
  private final UserRepository users;
  private final VersionService versions;
  private final HistoryRecorder history;
  private final TagService tags;
  private final AttributeService attributes;
  private final Clock clock;

  public MergeService(
      ContactRepository contacts,
      MergeRepository merges,
      AccessService access,
      UserRepository users,
      VersionService versions,
      HistoryRecorder history,
      TagService tags,
      AttributeService attributes,
      Clock clock) {
    this.contacts = contacts;
    this.merges = merges;
    this.access = access;
    this.users = users;
    this.versions = versions;
    this.history = history;
    this.tags = tags;
    this.attributes = attributes;
    this.clock = clock;
  }

  /** Merges the picked contacts into the oldest and returns it, Owner or Editor (R-022-05). */
  @Transactional
  public Contact merge(
      UUID groupId,
      String userId,
      List<Pick> picks,
      ContactInput rawFields,
      List<Attribute> attributeChoices) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
    requireTwoDifferent(picks);
    List<Contact> picked = lockAll(groupId, picks);
    ContactInput fields = ContactRules.validate(rawFields);
    List<Contact> byAge =
        picked.stream()
            .sorted(Comparator.comparing(Contact::createdAt).thenComparing(c -> c.id().toString()))
            .toList();
    Contact survivor = byAge.get(0);
    List<String> mergedTags = MergeCombiner.tags(byAge.stream().map(Contact::tags).toList());
    List<Attribute> mergedAttributes =
        MergeCombiner.attributes(
            byAge.stream().map(Contact::attributes).toList(),
            attributeChoices == null ? List.of() : attributeChoices);
    List<Contact> away = byAge.subList(1, byAge.size());
    Contact result = update(groupId, userId, survivor, fields, mergedTags, mergedAttributes);
    var now = clock.instant();
    for (Contact c : away) {
      contacts.trash(c.id(), now);
      merges.link(c.id(), survivor.id(), now);
    }
    history.record(
        ChangeSet.of(
            groupId,
            userId,
            Event.contact(
                HistoryEventType.CONTACTS_MERGED,
                survivor.id(),
                survivor.label(),
                entryChanges(away, result))));
    return contacts.find(groupId, survivor.id()).map(this::withRelations).orElseThrow();
  }

  private Contact update(
      UUID groupId,
      String userId,
      Contact survivor,
      ContactInput fields,
      List<String> mergedTags,
      List<Attribute> mergedAttributes) {
    TagChange tagChange = TagChange.between(survivor.tags(), mergedTags);
    List<FieldChange> attributeChanges =
        AttributeChange.between(survivor.attributes(), mergedAttributes);
    if (survivor.fields().equals(fields) && tagChange.isEmpty() && attributeChanges.isEmpty()) {
      return survivor;
    }
    int number = survivor.versionNumber() + 1;
    contacts.update(survivor.id(), fields, number);
    tags.apply(groupId, survivor.id(), tagChange);
    if (!attributeChanges.isEmpty()) {
      attributes.replace(survivor.id(), mergedAttributes);
    }
    Contact saved = contacts.find(groupId, survivor.id()).map(this::withRelations).orElseThrow();
    versions.record(saved, number, users.find(userId).orElseThrow());
    return saved;
  }

  /** The merged-away contacts by id (no personal data stays in a survivor's entry) and values. */
  private static List<FieldChange> entryChanges(List<Contact> away, Contact result) {
    List<FieldChange> changes = new ArrayList<>();
    away.forEach(c -> changes.add(new FieldChange("merged", null, c.id().toString())));
    changes.addAll(result.fields().changesFrom(null));
    result.tags().forEach(t -> changes.add(new FieldChange("tag", null, t)));
    result
        .attributes()
        .forEach(a -> changes.add(new FieldChange("attribute:" + a.key(), null, a.value())));
    return changes;
  }

  private static void requireTwoDifferent(List<Pick> picks) {
    if (picks == null
        || picks.size() < 2
        || picks.stream().anyMatch(p -> p == null || p.id() == null)) {
      throw refused("TOO_FEW_CONTACTS", "Pick at least two contacts to merge.");
    }
    if (new HashSet<>(picks.stream().map(Pick::id).toList()).size() != picks.size()) {
      throw refused("DUPLICATE_CONTACT", "A contact cannot be merged with itself.");
    }
  }

  /** Locks in id order, then checks Trash and revision of every contact (R-022-10, R-022-12). */
  private List<Contact> lockAll(UUID groupId, List<Pick> picks) {
    List<Pick> ordered =
        picks.stream().sorted(Comparator.comparing(p -> p.id().toString())).toList();
    List<Contact> locked = new ArrayList<>();
    for (Pick p : ordered) {
      locked.add(
          contacts
              .lock(groupId, p.id())
              .map(this::withRelations)
              .orElseThrow(AccessService::notFound));
    }
    if (locked.stream().anyMatch(c -> c.state() == ContactState.TRASHED)) {
      throw refused("IN_TRASH", "A contact in Trash cannot be merged.");
    }
    for (int i = 0; i < ordered.size(); i++) {
      if (ordered.get(i).revision() == null) {
        throw refused("REQUIRED", "The revision of every opened contact.");
      }
      if (locked.get(i).revision() != ordered.get(i).revision()) {
        throw ApiException.conflict(locked.get(i));
      }
    }
    return locked;
  }

  private static ApiException refused(String code, String message) {
    return new ApiException(
        ErrorCode.VALIDATION_FAILED,
        "The request is not valid.",
        List.of(new FieldProblem("contacts", code, message)));
  }

  private Contact withRelations(Contact contact) {
    return contact.withTags(tags.namesOf(contact.id())).withAttributes(attributes.of(contact.id()));
  }
}
