package app.contactgroups.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings that differ between environments (ES-01, NFR-12, AR-12). Every value comes from an
 * environment variable through {@code application.yml}; a missing required variable stops the
 * start.
 *
 * @param firebaseProjectId expected issuer and audience of the ID token (SR-01)
 * @param firebaseAuthEmulatorHost accept the emulator's unsigned tokens when not empty; empty in
 *     production
 * @param allowedOrigin the only CORS origin (SR-05)
 * @param trashCleanupCron schedule of the trash cleanup (AR-10)
 * @param rateLimitPerMinute requests per user and minute (SR-09)
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @NotBlank String firebaseProjectId,
    String firebaseAuthEmulatorHost,
    @NotBlank String allowedOrigin,
    @NotBlank String trashCleanupCron,
    @Min(1) int rateLimitPerMinute) {

  /** True when the emulator's unsigned test tokens are accepted (test and local only). */
  public boolean emulatorEnabled() {
    return firebaseAuthEmulatorHost != null && !firebaseAuthEmulatorHost.isBlank();
  }
}
