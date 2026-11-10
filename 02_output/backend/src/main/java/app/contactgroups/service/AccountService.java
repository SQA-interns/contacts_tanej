package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.GroupRef;
import app.contactgroups.repository.GroupRepository;
import app.contactgroups.repository.HistoryRepository;
import app.contactgroups.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes the account's data in the product (US-016, ADR-02). */
@Service
public class AccountService {

  /** How recent the sign-in must be (R-016-07). */
  static final Duration RECENT_LOGIN = Duration.ofMinutes(5);

  private final GroupRepository groups;
  private final HistoryRepository history;
  private final UserRepository users;
  private final Clock clock;

  public AccountService(
      GroupRepository groups, HistoryRepository history, UserRepository users, Clock clock) {
    this.groups = groups;
    this.history = history;
    this.users = users;
    this.clock = clock;
  }

  /**
   * Removes the memberships and the user record, removes the e-mail from the History entries the
   * user caused (the name stays) and records the id as deleted (R-016-01 to R-016-07).
   */
  @Transactional
  public void delete(String userId, Instant authTime) {
    Instant now = clock.instant();
    if (authTime == null || authTime.isBefore(now.minus(RECENT_LOGIN))) {
      throw new ApiException(
          ErrorCode.RECENT_LOGIN_REQUIRED, "Sign in again before you delete your account.");
    }
    List<GroupRef> blocking = groups.lockAndFindSoleOwned(userId);
    if (!blocking.isEmpty()) {
      throw ApiException.soleOwner(blocking);
    }
    history.removeActorEmail(userId);
    users.markDeleted(userId, now);
    users.delete(userId);
  }
}
