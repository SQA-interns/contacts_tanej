package app.contactgroups.service;

import app.contactgroups.repository.HealthRepository;
import org.springframework.stereotype.Service;

/** The health of the system: it is up while its database works (R-017-12). */
@Service
public class HealthService {

  private final HealthRepository repository;

  public HealthService(HealthRepository repository) {
    this.repository = repository;
  }

  public boolean databaseWorks() {
    return repository.databaseWorks();
  }
}
