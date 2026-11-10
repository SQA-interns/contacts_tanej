package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.ChangeSet.Event;
import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactPage;
import app.contactgroups.domain.ContactState;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.domain.TrashedContact;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.TagRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delete to Trash, restore and delete forever of a contact (US-012). Every change runs under the
 * row lock of the contact, so two requests at once see each other's result (ADR-03, EX-012-03).
 */
@Service
public class TrashService {

  /** One page of the Trash view. */
  public record TrashPage(List<TrashedContact> items, int total, int page, int pageSize) {}

  private final ContactRepository contacts;
  private final TagRepository tags;
  private final AccessService access;
  private final HistoryRecorder history;
  private final Clock clock;

  public TrashService(
      ContactRepository contacts,
      TagRepository tags,
      AccessService access,
      HistoryRecorder history,
      Clock clock) {
    this.contacts = contacts;
    this.tags = tags;
    this.access = access;
    this.history = history;
    this.clock = clock;
  }

  /** The Trash view with the days left, Owner or Editor (R-012-02). */
  @Transactional(readOnly = true)
  public TrashPage list(UUID groupId, String userId, Integer requestedPage) {
    requireWriter(groupId, userId);
    int total = contacts.countTrashed(groupId);
    int page = Math.min(Math.max(1, requestedPage == null ? 1 : requestedPage), lastPage(total));
    var items =
        contacts.pageTrashed(
            groupId, ContactPage.PAGE_SIZE, (page - 1) * ContactPage.PAGE_SIZE, clock.instant());
    return new TrashPage(items, total, page, ContactPage.PAGE_SIZE);
  }

  /** Moves an active contact to Trash, Owner or Editor (R-012-01, EX-012-03). */
  @Transactional
  public void trash(UUID groupId, String userId, UUID contactId) {
    requireWriter(groupId, userId);
    Contact contact = lock(groupId, contactId);
    if (contact.state() == ContactState.TRASHED) {
      throw new ApiException(ErrorCode.ALREADY_IN_TRASH, "This contact is already in Trash.");
    }
    contacts.trash(contactId, clock.instant());
    record(groupId, userId, HistoryEventType.CONTACT_DELETED, contact);
  }

  /** Brings a contact in Trash back to the list, Owner or Editor (R-012-03). */
  @Transactional
  public void restore(UUID groupId, String userId, UUID contactId) {
    requireWriter(groupId, userId);
    Contact contact = lock(groupId, contactId);
    if (contact.state() == ContactState.ACTIVE) {
      throw new ApiException(ErrorCode.NOT_IN_TRASH, "This contact is not in Trash.");
    }
    contacts.restore(contactId);
    record(groupId, userId, HistoryEventType.CONTACT_RESTORED, contact);
  }

  /** Delete forever: erases a contact in Trash at once, Owner only (R-012-04, R-012-05). */
  @Transactional
  public void erase(UUID groupId, String userId, UUID contactId) {
    access.requireOwner(groupId, userId);
    Contact contact = lock(groupId, contactId);
    if (contact.state() == ContactState.ACTIVE) {
      throw new ApiException(ErrorCode.NOT_IN_TRASH, "This contact is not in Trash.");
    }
    erase(contact, userId);
  }

  /**
   * Erases the contact if it is still in Trash since {@code cutoff} or earlier; used by the cleanup
   * (R-012-06). A contact restored or erased meanwhile is left alone.
   */
  @Transactional
  public boolean eraseExpired(UUID groupId, UUID contactId, Instant cutoff) {
    Optional<Contact> found = contacts.lock(groupId, contactId);
    if (found.isEmpty()
        || found.get().state() != ContactState.TRASHED
        || found.get().trashedAt().isAfter(cutoff)) {
      return false;
    }
    erase(found.get(), null);
    return true;
  }

  /** ADR-05: entry, anonymisation, row, unused tags; the actor is {@code null} for the system. */
  private void erase(Contact contact, String actorId) {
    UUID groupId = contact.groupId();
    record(groupId, actorId, HistoryEventType.CONTACT_ERASED, contact.id(), null);
    history.erase(contact.id());
    tags.lockGroupTags(groupId);
    contacts.delete(contact.id());
    tags.deleteUnused(groupId);
  }

  private void record(UUID groupId, String actorId, HistoryEventType type, Contact contact) {
    record(groupId, actorId, type, contact.id(), contact.label());
  }

  private void record(
      UUID groupId, String actorId, HistoryEventType type, UUID contactId, String label) {
    history.record(
        ChangeSet.of(groupId, actorId, Event.contact(type, contactId, label, List.of())));
  }

  private Contact lock(UUID groupId, UUID contactId) {
    return contacts.lock(groupId, contactId).orElseThrow(AccessService::notFound);
  }

  private void requireWriter(UUID groupId, String userId) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
  }

  private static int lastPage(int total) {
    return ContactPage.lastPage(total);
  }
}
