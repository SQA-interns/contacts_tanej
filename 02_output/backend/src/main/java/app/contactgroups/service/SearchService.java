package app.contactgroups.service;

import app.contactgroups.domain.Condition;
import app.contactgroups.domain.ConditionInput;
import app.contactgroups.domain.Conditions;
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

  /** The companies used in the group that match the typed text, for every member (R-010-07). */
  @Transactional(readOnly = true)
  public List<String> companies(UUID groupId, String userId, String part) {
    access.requireMember(groupId, userId);
    return contacts.companiesOfGroup(groupId, part);
  }

  /** One page; a page beyond the last gives the last, a missing page the first (EX-009-09). */
  @Transactional(readOnly = true)
  public ContactPage search(
      UUID groupId,
      String userId,
      String text,
      List<ConditionInput> rawConditions,
      ContactSort sort,
      Integer requestedPage) {
    access.requireMember(groupId, userId);
    List<String> patterns = SearchWords.patterns(text);
    List<Condition> conditions = Conditions.parse(rawConditions);
    int total = contacts.count(groupId, patterns, conditions);
    int wanted = requestedPage == null ? 1 : Math.max(1, requestedPage);
    int page = Math.min(wanted, ContactPage.lastPage(total));
    var items = contacts.page(groupId, patterns, conditions, ContactSort.of(sort), page);
    return new ContactPage(items, total, page, ContactPage.PAGE_SIZE);
  }
}
