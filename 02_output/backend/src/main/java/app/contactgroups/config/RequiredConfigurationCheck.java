package app.contactgroups.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.PlaceholderResolutionException;

/**
 * Stops the start when a required variable is missing (AR-12, NFR-12) and names it. Runs when the
 * application is prepared, after test and command-line properties are known.
 */
public class RequiredConfigurationCheck implements ApplicationListener<ApplicationPreparedEvent> {

  /** Property keys whose value must exist. */
  private static final List<String> REQUIRED =
      List.of(
          "spring.datasource.url",
          "spring.datasource.username",
          "spring.datasource.password",
          "app.firebase-project-id",
          "app.allowed-origin");

  @Override
  public void onApplicationEvent(ApplicationPreparedEvent event) {
    Environment env = event.getApplicationContext().getEnvironment();
    List<String> missing = new ArrayList<>();
    for (String key : REQUIRED) {
      try {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) {
          missing.add(key);
        }
      } catch (PlaceholderResolutionException e) {
        missing.add(e.getPlaceholder());
      }
    }
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "Missing required configuration: " + String.join(", ", missing));
    }
    EmulatorHostGuard.check(env);
  }
}
