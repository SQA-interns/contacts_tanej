package app.contactgroups.api;

import app.contactgroups.domain.DuplicateSet;
import app.contactgroups.service.DuplicateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Find duplicates and "not a duplicate" (US-021); the merge is added by US-022. */
@RestController
public class DuplicateController {

  private final DuplicateService duplicates;

  public DuplicateController(DuplicateService duplicates) {
    this.duplicates = duplicates;
  }

  /** The found sets (contract {@code DuplicateSearchResult}). */
  public record SearchResult(List<DuplicateSet> sets) {}

  /** The ids of the contacts of the set (contract {@code DismissalRequest}). */
  public record DismissalRequest(List<UUID> contactIds) {}

  @PostMapping("/groups/{groupId}/duplicates/search")
  @Operation(operationId = "findDuplicates")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "429")
  })
  public SearchResult search(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
    return new SearchResult(duplicates.search(groupId, jwt.getSubject()));
  }

  @PostMapping("/groups/{groupId}/duplicates/dismissals")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "dismissDuplicates")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public void dismiss(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestBody DismissalRequest request) {
    duplicates.dismiss(groupId, jwt.getSubject(), request.contactIds());
  }
}
