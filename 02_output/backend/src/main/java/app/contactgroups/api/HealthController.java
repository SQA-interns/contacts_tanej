package app.contactgroups.api;

import app.contactgroups.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The health check without authentication (R-017-12, operation {@code getHealth}). */
@RestController
public class HealthController {

  /** Body of the health check: {@code UP} or {@code DOWN}. */
  public record Health(String status) {}

  private final HealthService service;

  public HealthController(HealthService service) {
    this.service = service;
  }

  @GetMapping("/health")
  @Operation(operationId = "getHealth")
  @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "503")})
  public ResponseEntity<Health> getHealth() {
    return service.databaseWorks()
        ? ResponseEntity.ok(new Health("UP"))
        : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new Health("DOWN"));
  }
}
