package app.contactgroups.service;

import app.contactgroups.domain.Tag;
import app.contactgroups.domain.TagChange;
import app.contactgroups.repository.TagRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Tags of the contacts of a group (US-006): the save path and the suggestions. */
@Service
public class TagService {

  private final TagRepository tags;
  private final AccessService access;

  public TagService(TagRepository tags, AccessService access) {
    this.tags = tags;
    this.access = access;
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

  /** The tags of the group matching the typed text, for every member (R-006-10). */
  @Transactional(readOnly = true)
  public List<Tag> list(UUID groupId, String userId, String part) {
    access.requireMember(groupId, userId);
    return tags.findByGroup(groupId, part);
  }
}
