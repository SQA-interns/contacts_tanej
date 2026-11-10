package app.contactgroups.api;

import app.contactgroups.domain.Member;
import app.contactgroups.service.MemberService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** List members, change a role, remove a member and leave a group (US-004). */
@RestController
public class MemberController {

  /** The body of change role (contract {@code RoleRequest}). */
  public record RoleRequest(String role) {}

  private final MemberService members;

  public MemberController(MemberService members) {
    this.members = members;
  }

  @GetMapping("/groups/{groupId}/members")
  @Operation(operationId = "listMembers")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public List<Member> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
    return members.list(groupId, jwt.getSubject());
  }

  @PutMapping("/groups/{groupId}/members/{userId}")
  @Operation(operationId = "changeRole")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409")
  })
  public Member changeRole(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @PathVariable String userId,
      @RequestBody RoleRequest request) {
    return members.changeRole(groupId, jwt.getSubject(), userId, request.role());
  }

  @DeleteMapping("/groups/{groupId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "removeMember")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409")
  })
  public void remove(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId, @PathVariable String userId) {
    members.remove(groupId, jwt.getSubject(), userId);
  }

  @PostMapping("/groups/{groupId}/leave")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "leaveGroup")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409")
  })
  public void leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId) {
    members.leave(groupId, jwt.getSubject());
  }
}
