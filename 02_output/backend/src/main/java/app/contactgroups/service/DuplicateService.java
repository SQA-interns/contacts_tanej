package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ContactRow;
import app.contactgroups.domain.DuplicateDetector;
import app.contactgroups.domain.DuplicateMatch;
import app.contactgroups.domain.DuplicateSet;
import app.contactgroups.domain.DuplicateSet.Member;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldProblem;
import app.contactgroups.domain.Role;
import app.contactgroups.repository.ContactSearchRepository;
import app.contactgroups.repository.DuplicateRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds duplicate sets on request and remembers "not a duplicate" marks (US-021, ADR-11). */
@Service
public class DuplicateService {

  private final DuplicateRepository duplicates;
  private final ContactSearchRepository rows;
  private final AccessService access;
  private final Clock clock;

  public DuplicateService(
      DuplicateRepository duplicates,
      ContactSearchRepository rows,
      AccessService access,
      Clock clock) {
    this.duplicates = duplicates;
    this.rows = rows;
    this.access = access;
    this.clock = clock;
  }

  /** The sets of the whole group that are not dismissed; changes nothing (R-021-08). */
  @Transactional(readOnly = true)
  public List<DuplicateSet> search(UUID groupId, String userId) {
    requireWriter(groupId, userId);
    Set<String> dismissed = duplicates.dismissedKeys(groupId);
    List<DuplicateMatch> shown =
        DuplicateDetector.detect(duplicates.candidates(groupId)).stream()
            .filter(m -> !dismissed.contains(m.key()))
            .toList();
    List<UUID> all = shown.stream().flatMap(m -> m.ids().stream()).toList();
    if (all.isEmpty()) {
      return List.of();
    }
    Map<UUID, ContactRow> rowOf = new HashMap<>();
    rows.byIds(groupId, all).forEach(r -> rowOf.put(r.id(), r));
    Map<UUID, Instant> created = duplicates.createdAt(all);
    return shown.stream()
        .map(
            m ->
                new DuplicateSet(
                    m.kind(),
                    m.reasons(),
                    m.ids().stream()
                        .map(id -> new Member(rowOf.get(id), created.get(id)))
                        .toList()))
        .toList();
  }

  /** Marks the set of exactly these contacts "not a duplicate" (R-021-09). */
  @Transactional
  public void dismiss(UUID groupId, String userId, List<UUID> contactIds) {
    requireWriter(groupId, userId);
    List<UUID> ids = contactIds == null ? List.of() : contactIds.stream().distinct().toList();
    if (ids.size() < 2) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          "The request is not valid.",
          List.of(new FieldProblem("contactIds", "TOO_FEW", "At least two different contacts.")));
    }
    if (duplicates.countActive(groupId, ids) != ids.size()) {
      throw AccessService.notFound();
    }
    duplicates.dismiss(groupId, DuplicateMatch.keyOf(ids), ids, userId, clock.instant());
  }

  private void requireWriter(UUID groupId, String userId) {
    if (access.requireMember(groupId, userId).role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
  }
}
