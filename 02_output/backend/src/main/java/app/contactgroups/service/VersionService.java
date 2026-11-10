package app.contactgroups.service;

import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactVersion;
import app.contactgroups.domain.User;
import app.contactgroups.repository.VersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
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
  private final ObjectMapper json;
  private final Clock clock;

  public VersionService(VersionRepository versions, ObjectMapper json, Clock clock) {
    this.versions = versions;
    this.json = json;
    this.clock = clock;
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
