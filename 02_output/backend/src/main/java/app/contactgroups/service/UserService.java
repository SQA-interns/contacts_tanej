package app.contactgroups.service;

import app.contactgroups.domain.DisplayName;
import app.contactgroups.domain.User;
import app.contactgroups.repository.UserRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** The user record that every request refreshes from its ID token (ADR-02, D-20). */
@Service
public class UserService {

  private final UserRepository users;
  private final Clock clock;

  public UserService(UserRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  /** True when the account was deleted (R-016-06). */
  public boolean isDeleted(String userId) {
    return users.isDeleted(userId);
  }

  /** Stores the user of the token: name by D-20, e-mail in lowercase. */
  public User sync(String userId, String tokenName, String tokenEmail) {
    String email = tokenEmail == null ? "" : tokenEmail.strip().toLowerCase(Locale.ROOT);
    User user = new User(userId, DisplayName.of(tokenName, email), email);
    users.upsert(user, clock.instant());
    return user;
  }

  /** The record of the signed-in user; the sync filter created it earlier in the same request. */
  public User current(String userId) {
    return users.find(userId).orElseThrow(() -> new IllegalStateException("No user record"));
  }
}
