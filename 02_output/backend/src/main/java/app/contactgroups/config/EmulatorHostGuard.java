package app.contactgroups.config;

import java.util.List;
import org.springframework.core.env.Environment;

/**
 * Refuses to start with an Authentication Emulator host outside the profiles {@code local} and
 * {@code test}: the emulator's unsigned tokens would let anyone forge an identity (ADR-02).
 */
final class EmulatorHostGuard {

  private static final List<String> ALLOWED_PROFILES = List.of("local", "test");

  private EmulatorHostGuard() {}

  static void check(Environment env) {
    String host = env.getProperty("app.firebase-auth-emulator-host");
    if (host == null || host.isBlank()) {
      return;
    }
    for (String profile : env.getActiveProfiles()) {
      if (ALLOWED_PROFILES.contains(profile)) {
        return;
      }
    }
    throw new IllegalStateException(
        "FIREBASE_AUTH_EMULATOR_HOST must be empty unless the profile is local or test");
  }
}
