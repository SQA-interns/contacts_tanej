package app.contactgroups.service;

import app.contactgroups.domain.TrashedContact;
import app.contactgroups.repository.ContactRepository;
import app.contactgroups.repository.GroupRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * The scheduled cleanup (ADR-08). It erases contacts that have been in Trash for 30 days; each
 * contact in its own transaction, so one failure does not stop the rest. The steps for groups,
 * invitations and account tombstones join it with their stories.
 */
@Service
public class TrashCleanupService {

  private static final Logger LOG = LoggerFactory.getLogger(TrashCleanupService.class);

  private final ContactRepository contacts;
  private final GroupRepository groups;
  private final TrashService trash;
  private final GroupService groupService;
  private final Clock clock;

  public TrashCleanupService(
      ContactRepository contacts,
      GroupRepository groups,
      TrashService trash,
      GroupService groupService,
      Clock clock) {
    this.contacts = contacts;
    this.groups = groups;
    this.trash = trash;
    this.groupService = groupService;
    this.clock = clock;
  }

  /** Runs at the time set by {@code app.trash-cleanup-cron}; tests call {@link #run} directly. */
  @Scheduled(cron = "${app.trash-cleanup-cron}")
  public void scheduled() {
    run();
  }

  /** Erases the contacts with {@code trashed_at <= now - 30 days} (R-012-06, EX-012-02). */
  public void run() {
    Instant cutoff = clock.instant().minus(Duration.ofDays(TrashedContact.RETENTION_DAYS));
    for (UUID[] ids : contacts.expired(cutoff)) {
      try {
        trash.eraseExpired(ids[1], ids[0], cutoff);
      } catch (RuntimeException e) {
        LOG.error("Erasing an expired contact failed; the next run tries again", e);
      }
    }
    purgeGroups(cutoff);
  }

  /** Deletes the groups in Trash for 30 days with all their data (R-015-06, EX-015-02). */
  private void purgeGroups(Instant cutoff) {
    for (UUID groupId : groups.expired(cutoff)) {
      try {
        groupService.purgeExpired(groupId, cutoff);
      } catch (RuntimeException e) {
        LOG.error("Purging an expired group failed; the next run tries again", e);
      }
    }
  }
}
