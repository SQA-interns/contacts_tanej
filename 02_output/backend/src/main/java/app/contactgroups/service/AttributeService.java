package app.contactgroups.service;

import app.contactgroups.domain.Contact.Attribute;
import app.contactgroups.repository.AttributeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Attributes of the contacts of a group (US-008): the save path and the key suggestions. */
@Service
public class AttributeService {

  private final AttributeRepository attributes;
  private final AccessService access;

  public AttributeService(AttributeRepository attributes, AccessService access) {
    this.attributes = attributes;
    this.access = access;
  }

  public List<Attribute> of(UUID contactId) {
    return attributes.findByContact(contactId);
  }

  /** Writes the new attributes of the contact, inside the save of the contact. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void replace(UUID contactId, List<Attribute> wanted) {
    attributes.replace(contactId, wanted);
  }

  /** The keys of the group matching the typed text, for every member (R-008-05). */
  @Transactional(readOnly = true)
  public List<String> keys(UUID groupId, String userId, String part) {
    access.requireMember(groupId, userId);
    return attributes.keysOfGroup(groupId, part);
  }
}
