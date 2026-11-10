package app.contactgroups.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.repository.HistoryRepository;
import app.contactgroups.service.HistoryRecorder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * T-07 History append foundation (D-22): R-013-07, R-013-10, R-013-11; AC-013-09; EX-013-01,
 * EX-013-08. The guard triggers are proven in {@code SkeletonIT}.
 */
class HistoryRecorderTransactionIT extends AcceptanceBase {

  private static final String ACTOR = "uid-ana";

  @Autowired HistoryRecorder recorder;
  @Autowired HistoryRepository history;
  @Autowired TransactionTemplate tx;

  private UUID group;

  @BeforeEach
  void aGroupAndItsActor() {
    group = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users (id, name, email, created_at) VALUES (?, 'Ana Novak',"
            + " 'ana.novak@example.com', now())",
        ACTOR);
    jdbc.update(
        "INSERT INTO groups (id, name, state, created_at, created_by) VALUES (?, 'G', 'ACTIVE',"
            + " now(), ?)",
        group,
        ACTOR);
  }

  private ChangeSet renamed(String from, String to) {
    return ChangeSet.of(
        group,
        ACTOR,
        ChangeSet.Event.group(
            HistoryEventType.GROUP_RENAMED, List.of(new FieldChange("name", from, to))));
  }

  @Test
  void aFailedChangeWritesNoEntry() {
    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    s -> {
                      recorder.record(renamed("a", "b"));
                      throw new IllegalStateException("the change failed");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(history.findByGroup(group)).isEmpty();
  }

  @Test
  void anEntryCannotBeWrittenOutsideTheTransactionOfTheChange() {
    assertThatThrownBy(() -> recorder.record(renamed("a", "b")))
        .isInstanceOf(RuntimeException.class);
    assertThat(history.findByGroup(group)).isEmpty();
  }

  @Test
  void newestEntryComesFirst() {
    clockAt("2026-07-01T10:00:00Z");
    tx.executeWithoutResult(s -> recorder.record(renamed("a", "b")));
    clock.advance(java.time.Duration.ofMinutes(5));
    tx.executeWithoutResult(s -> recorder.record(renamed("b", "c")));
    assertThat(history.findByGroup(group))
        .extracting(e -> e.changes().get(0).newValue())
        .containsExactly("c", "b");
  }

  @Test
  void entriesLiveWithTheGroup() {
    tx.executeWithoutResult(s -> recorder.record(renamed("a", "b")));
    jdbc.update("DELETE FROM groups WHERE id = ?", group);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM history_entries", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM history_changes", Integer.class)).isZero();
  }
}
