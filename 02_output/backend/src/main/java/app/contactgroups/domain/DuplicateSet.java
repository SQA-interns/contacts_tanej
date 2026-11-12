package app.contactgroups.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import java.util.List;

/** A found set as the API shows it (contract {@code DuplicateSet}). */
public record DuplicateSet(
    DuplicateKind kind, List<DuplicateReason> reasons, List<Member> contacts) {

  /** A contact row with its creation time (the oldest survives a merge). */
  public record Member(@JsonUnwrapped ContactRow row, Instant createdAt) {}
}
