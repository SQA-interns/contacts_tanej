package app.contactgroups;

import app.contactgroups.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point of the Contact Groups API. */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class ContactGroupsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ContactGroupsApplication.class, args);
  }
}
