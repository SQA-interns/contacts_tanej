package app.contactgroups.api;

import app.contactgroups.domain.GroupSummary;
import app.contactgroups.service.GroupService;
import app.contactgroups.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Create, list, open and rename a group (US-002). The group id comes only from the path. */
@RestController
public class GroupController {

  /** The body of create and rename: only the name (contract {@code GroupNameRequest}). */
  public record GroupNameRequest(String name) {}

  private final GroupService groups;

  private final InvitationService invitations;

  public GroupController(GroupService groups, InvitationService invitations) {
    this.groups = groups;
    this.invitations = invitations;
  }

  @GetMapping("/groups")
  @Operation(operationId = "listGroups")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403")
  })
  public List<GroupSummary> list(@AuthenticationPrincipal Jwt jwt) {
    invitations.acceptFor(jwt.getSubject());
    return groups.listFor(jwt.getSubject());
  }

  @PostMapping("/groups")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "createGroup")
  @ApiResponses({
    @ApiResponse(responseCode = "201"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "409"),
    @ApiResponse(responseCode = "413"),
    @ApiResponse(responseCode = "429")
  })
  public GroupSummary create(
      @AuthenticationPrincipal Jwt jwt, @RequestBody GroupNameRequest request) {
    return groups.create(jwt.getSubject(), request.name());
  }

  @GetMapping("/groups/{groupId}")
  @Operation(operationId = "getGroup")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public GroupSummary get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
    return groups.get(groupId, jwt.getSubject());
  }

  @PatchMapping("/groups/{groupId}")
  @Operation(operationId = "renameGroup")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409"),
    @ApiResponse(responseCode = "413"),
    @ApiResponse(responseCode = "429")
  })
  public GroupSummary rename(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestBody GroupNameRequest request) {
    return groups.rename(groupId, jwt.getSubject(), request.name());
  }
}
