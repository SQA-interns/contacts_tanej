package app.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** ADR-02: forged emulator tokens must never be accepted outside the local and test profiles. */
class EmulatorHostGuardTest {

  private static MockEnvironment env(String host, String... profiles) {
    MockEnvironment env =
        new MockEnvironment().withProperty("app.firebase-auth-emulator-host", host);
    env.setActiveProfiles(profiles);
    return env;
  }

  @Test
  void refusesTheEmulatorHostWithoutALocalOrTestProfile() {
    assertThatThrownBy(() -> EmulatorHostGuard.check(env("emulator:9099")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FIREBASE_AUTH_EMULATOR_HOST");
    assertThatThrownBy(() -> EmulatorHostGuard.check(env("emulator:9099", "prod")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void allowsTheEmulatorHostInLocalAndTest() {
    assertThatCode(() -> EmulatorHostGuard.check(env("emulator:9099", "local")))
        .doesNotThrowAnyException();
    assertThatCode(() -> EmulatorHostGuard.check(env("emulator:9099", "test")))
        .doesNotThrowAnyException();
  }

  @Test
  void allowsAnEmptyHostInEveryProfile() {
    assertThatCode(() -> EmulatorHostGuard.check(env("", "prod"))).doesNotThrowAnyException();
    assertThatCode(() -> EmulatorHostGuard.check(env(""))).doesNotThrowAnyException();
  }
}
