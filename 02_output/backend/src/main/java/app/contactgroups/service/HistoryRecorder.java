package app.contactgroups.service;

import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.HistoryEntry;
import app.contactgroups.domain.User;
import app.contactgroups.repository.HistoryRepository;
import app.contactgroups.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only writer of History (ADR-05). It must be called inside the transaction of the change it
 * records, so that a refused or failed change leaves no entry (AR-09, EX-013-01, EX-013-08).
 */
@Service
public class HistoryRecorder {

  private final HistoryRepository history;
  private final UserRepository users;
  private final Clock clock;

  public HistoryRecorder(HistoryRepository history, UserRepository users, Clock clock) {
    this.history = history;
    this.users = users;
    this.clock = clock;
  }

  /** Appends one entry per event of the change set; all share the change set id. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(ChangeSet set) {
    User actor =
        users
            .find(set.actorId())
            .orElseThrow(() -> new IllegalStateException("The actor has no user record"));
    var now = clock.instant();
    for (ChangeSet.Event e : set.events()) {
      history.insert(
          new HistoryEntry(
              UUID.randomUUID(),
              set.groupId(),
              e.contactId(),
              e.contactLabel(),
              set.id(),
              e.type(),
              actor.id(),
              actor.name(),
              actor.email(),
              e.subjectName(),
              e.role(),
              false,
              now,
              e.changes()));
    }
  }
}
