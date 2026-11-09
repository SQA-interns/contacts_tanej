package app.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.contactgroups.ContactGroupsApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** AR-12, NFR-12: the API refuses to start when a required variable is missing. */
class RequiredConfigurationTest {

  @Test
  void refusesToStartWithoutDatabaseUrl() {
    assertThatThrownBy(
            () ->
                new SpringApplicationBuilder(ContactGroupsApplication.class)
                    .web(WebApplicationType.SERVLET)
                    .properties(
                        "server.port=0",
                        "DB_USER=u",
                        "DB_PASSWORD=p",
                        "FIREBASE_PROJECT_ID=demo-contacts",
                        "APP_ALLOWED_ORIGIN=http://localhost:5173")
                    .run())
        .hasStackTraceContaining("DB_URL");
  }

  @Test
  void emulatorIsEnabledOnlyWhenTheHostIsSet() {
    assertThat(new AppProperties("p", "localhost:9099", "o", "c", 100).emulatorEnabled()).isTrue();
    assertThat(new AppProperties("p", "", "o", "c", 100).emulatorEnabled()).isFalse();
    assertThat(new AppProperties("p", null, "o", "c", 100).emulatorEnabled()).isFalse();
  }
}
