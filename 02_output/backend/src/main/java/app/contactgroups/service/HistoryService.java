package app.contactgroups.service;

import app.contactgroups.domain.ContactPage;
import app.contactgroups.domain.HistoryActor;
import app.contactgroups.domain.HistoryEntry;
import app.contactgroups.domain.HistoryEntryView;
import app.contactgroups.domain.HistoryFilter;
import app.contactgroups.domain.Role;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.HistoryRepository;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads History for Owner and Editor; a Viewer gets 403 (R-013-09, R-013-13). Read only. */
@Service
public class HistoryService {

  /** One page of History, newest first. */
  public record HistoryPage(List<HistoryEntryView> items, int total, int page, int pageSize) {}

  private final HistoryRepository history;
  private final ContactRepository contacts;
  private final AccessService access;

  public HistoryService(
      HistoryRepository history, ContactRepository contacts, AccessService access) {
    this.history = history;
    this.contacts = contacts;
    this.access = access;
  }

  /** The group-wide History. */
  @Transactional(readOnly = true)
  public HistoryPage ofGroup(
      UUID groupId, String userId, HistoryFilter filter, Integer requestedPage) {
    requireWriter(groupId, userId);
    return page(
        history.countByGroup(groupId, filter),
        requestedPage,
        offset -> history.pageByGroup(groupId, filter, ContactPage.PAGE_SIZE, offset));
  }

  /** The people for the person filter, also those who left (EX-014-03). */
  @Transactional(readOnly = true)
  public List<HistoryActor> actors(UUID groupId, String userId) {
    requireWriter(groupId, userId);
    return history.actors(groupId);
  }

  /** The History tab of one contact, also of a contact in Trash. */
  @Transactional(readOnly = true)
  public HistoryPage ofContact(UUID groupId, String userId, UUID contactId, Integer requestedPage) {
    requireWriter(groupId, userId);
    contacts.find(groupId, contactId).orElseThrow(AccessService::notFound);
    return page(
        history.countByContact(contactId),
        requestedPage,
        offset -> history.pageByContact(contactId, ContactPage.PAGE_SIZE, offset));
  }

  private static HistoryPage page(
      int total, Integer requestedPage, IntFunction<List<HistoryEntry>> load) {
    int wanted = requestedPage == null ? 1 : requestedPage;
    int page = Math.min(Math.max(1, wanted), ContactPage.lastPage(total));
    var items =
        load.apply((page - 1) * ContactPage.PAGE_SIZE).stream().map(HistoryEntryView::of).toList();
    return new HistoryPage(items, total, page, ContactPage.PAGE_SIZE);
  }

  private void requireWriter(UUID groupId, String userId) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
  }
}
