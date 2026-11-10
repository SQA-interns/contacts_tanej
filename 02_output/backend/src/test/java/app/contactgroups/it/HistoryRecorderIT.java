package app.contactgroups.it;

import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.HistoryEntry;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.repository.HistoryRepository;
import app.contactgroups.service.HistoryRecorder;
import java.time.Instant;
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
class HistoryRecorderIT extends AcceptanceBase {

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
  void anEntryStoresActorTimeAndTheOldAndNewValueInFull() {
    String longValue = "x".repeat(5000);
    clockAt("2026-07-01T10:00:00Z");
    tx.executeWithoutResult(s -> recorder.record(renamed(longValue, "New")));

    List<HistoryEntry> entries = history.findByGroup(group);
    assertThat(entries).hasSize(1);
    HistoryEntry e = entries.get(0);
    assertThat(e.type()).isEqualTo(HistoryEventType.GROUP_RENAMED);
    assertThat(e.actorId()).isEqualTo(ACTOR);
    assertThat(e.actorName()).isEqualTo("Ana Novak");
    assertThat(e.actorEmail()).isEqualTo("ana.novak@example.com");
    assertThat(e.createdAt())
        .isBetween(Instant.parse("2026-07-01T09:59:00Z"), Instant.parse("2026-07-01T10:01:00Z"));
    assertThat(e.contactId()).isNull();
    assertThat(e.erased()).isFalse();
    assertThat(e.changes()).containsExactly(new FieldChange("name", longValue, "New"));
  }

  @Test
  void entriesOfOneChangeSetShareItsIdAndTwoSavesDoNot() {
    UUID contact = UUID.randomUUID();
    ChangeSet one =
        ChangeSet.of(
            group,
            ACTOR,
            ChangeSet.Event.contact(
                HistoryEventType.CONTACT_EDITED,
                contact,
                "Ana Novak",
                List.of(new FieldChange("phone", "1", "2"))),
            ChangeSet.Event.contact(
                HistoryEventType.TAG_CHANGED,
                contact,
                "Ana Novak",
                List.of(new FieldChange("tag", null, "vip"))));
    tx.executeWithoutResult(
        s -> {
          recorder.record(one);
          recorder.record(renamed("a", "b"));
        });

    List<HistoryEntry> entries = history.findByGroup(group);
    assertThat(entries).hasSize(3);
    List<HistoryEntry> shared = entries.stream().filter(e -> e.contactId() != null).toList();
    assertThat(shared).extracting(HistoryEntry::changeSetId).containsOnly(one.id());
    assertThat(shared).extracting(HistoryEntry::contactLabel).containsOnly("Ana Novak");
    assertThat(
            entries.stream()
                .filter(e -> e.contactId() == null)
                .findFirst()
                .orElseThrow()
                .changeSetId())
        .isNotEqualTo(one.id());
  }

  @Test
  void aMemberEntryNamesThePersonAndTheRoleOnly() {
    tx.executeWithoutResult(
        s ->
            recorder.record(
                ChangeSet.of(
                    group,
                    ACTOR,
                    ChangeSet.Event.member(
                        HistoryEventType.ROLE_CHANGED,
                        "Boris Kovač",
                        Role.EDITOR,
                        List.of(new FieldChange("role", "VIEWER", "EDITOR"))))));
    HistoryEntry e = history.findByGroup(group).get(0);
    assertThat(e.subjectName()).isEqualTo("Boris Kovač");
    assertThat(e.role()).isEqualTo(Role.EDITOR);
  }
}
