package app.contactgroups.service;

import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactVersion;
import app.contactgroups.domain.Role;
import app.contactgroups.domain.User;
import app.contactgroups.domain.VersionSummary;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.VersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records the versions of a contact (US-019, ADR-05). The number is the one the caller computed
 * under the row lock of the contact, so it is never reused or lowered (R-019-05).
 */
@Service
public class VersionService {

  private final VersionRepository versions;
  private final ContactRepository contacts;
  private final AccessService access;
  private final ObjectMapper json;
  private final Clock clock;

  public VersionService(
      VersionRepository versions,
      ContactRepository contacts,
      AccessService access,
      ObjectMapper json,
      Clock clock) {
    this.versions = versions;
    this.contacts = contacts;
    this.access = access;
    this.json = json;
    this.clock = clock;
  }

  /** The versions of a contact, newest first, for an Owner or Editor (R-019-06). */
  @Transactional(readOnly = true)
  public List<VersionSummary> list(UUID groupId, String userId, UUID contactId) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
    contacts.find(groupId, contactId).orElseThrow(AccessService::notFound);
    return versions.list(contactId);
  }

  /** Stores the contact as version {@code number}; the name of the author stays with it. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Contact contact, int number, User author) {
    ObjectNode snapshot = json.valueToTree(contact.fields());
    snapshot.set("tags", json.valueToTree(contact.tags()));
    snapshot.set("attributes", json.valueToTree(contact.attributes()));
    versions.insert(
        new ContactVersion(
            contact.id(),
            number,
            snapshot.toString(),
            author.id(),
            author.name(),
            clock.instant()));
  }
}
