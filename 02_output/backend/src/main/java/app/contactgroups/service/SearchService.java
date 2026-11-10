package app.contactgroups.service;

import app.contactgroups.domain.ContactPage;
import app.contactgroups.domain.ContactSort;
import app.contactgroups.domain.SearchWords;
import app.contactgroups.repository.ContactSearchRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The contact list with search text, sort and paging, the same for every member (US-009). */
@Service
public class SearchService {

  private final ContactSearchRepository contacts;
  private final AccessService access;

  public SearchService(ContactSearchRepository contacts, AccessService access) {
    this.contacts = contacts;
    this.access = access;
  }

  /** One page; a page beyond the last gives the last, a missing page the first (EX-009-09). */
  @Transactional(readOnly = true)
  public ContactPage search(
      UUID groupId, String userId, String text, ContactSort sort, Integer requestedPage) {
    access.requireMember(groupId, userId);
    List<String> patterns = SearchWords.patterns(text);
    int total = contacts.count(groupId, patterns);
    int wanted = requestedPage == null ? 1 : Math.max(1, requestedPage);
    int page = Math.min(wanted, ContactPage.lastPage(total));
    var items = contacts.page(groupId, patterns, ContactSort.of(sort), page);
    return new ContactPage(items, total, page, ContactPage.PAGE_SIZE);
  }
}
