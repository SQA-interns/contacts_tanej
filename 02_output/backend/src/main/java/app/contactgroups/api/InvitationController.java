package app.contactgroups.api;

import app.contactgroups.domain.Invitation;
import app.contactgroups.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** List, create and cancel the invitations of a group (US-003). Owner only. */
@RestController
public class InvitationController {

  /** The body of create (contract {@code InvitationRequest}). */
  public record InvitationRequest(String email, String role) {}

  private final InvitationService invitations;

  public InvitationController(InvitationService invitations) {
    this.invitations = invitations;
  }

  @GetMapping("/groups/{groupId}/invitations")
  @Operation(operationId = "listInvitations")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public List<Invitation> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
    return invitations.list(groupId, jwt.getSubject());
  }

  @PostMapping("/groups/{groupId}/invitations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "createInvitation")
  @ApiResponses({
    @ApiResponse(responseCode = "201"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409"),
    @ApiResponse(responseCode = "413"),
    @ApiResponse(responseCode = "429")
  })
  public Invitation create(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestBody InvitationRequest request) {
    return invitations.create(groupId, jwt.getSubject(), request.email(), request.role());
  }

  @DeleteMapping("/groups/{groupId}/invitations/{invitationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "cancelInvitation")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public void cancel(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @PathVariable UUID invitationId) {
    invitations.cancel(groupId, jwt.getSubject(), invitationId);
  }
}
