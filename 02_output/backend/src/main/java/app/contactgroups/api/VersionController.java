package app.contactgroups.api;

import app.contactgroups.domain.Contact;
import app.contactgroups.domain.VersionDetail;
import app.contactgroups.domain.VersionSummary;
import app.contactgroups.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The versions of a contact (US-019 list; US-020 adds read and restore). */
@RestController
public class VersionController {

  private final VersionService versions;

  public VersionController(VersionService versions) {
    this.versions = versions;
  }

  @GetMapping("/groups/{groupId}/contacts/{contactId}/versions")
  @Operation(operationId = "listVersions")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public List<VersionSummary> list(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId, @PathVariable UUID contactId) {
    return versions.list(groupId, jwt.getSubject(), contactId);
  }

  @GetMapping("/groups/{groupId}/contacts/{contactId}/versions/{number}")
  @Operation(operationId = "getVersion")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public VersionDetail get(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId,
      @PathVariable int number) {
    return versions.get(groupId, jwt.getSubject(), contactId, number);
  }

  /** The revision of the contact as the Versions tab opened it (R-020-08). */
  public record RestoreRequest(Long revision) {}

  @PostMapping("/groups/{groupId}/contacts/{contactId}/versions/{number}/restore")
  @Operation(operationId = "restoreVersion")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409")
  })
  public Contact restore(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId,
      @PathVariable int number,
      @RequestBody RestoreRequest request) {
    return versions.restore(groupId, jwt.getSubject(), contactId, number, request.revision());
  }
}
