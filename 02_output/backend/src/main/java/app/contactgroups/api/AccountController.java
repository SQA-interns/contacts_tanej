package app.contactgroups.api;

import app.contactgroups.domain.User;
import app.contactgroups.service.AccountService;
import app.contactgroups.service.InvitationService;
import app.contactgroups.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user: read it ({@code getMe}) and delete the account ({@code deleteAccount}). */
@RestController
public class AccountController {

  private final UserService users;

  private final InvitationService invitations;

  private final AccountService accounts;

  public AccountController(
      UserService users, InvitationService invitations, AccountService accounts) {
    this.users = users;
    this.invitations = invitations;
    this.accounts = accounts;
  }

  @GetMapping("/me")
  @Operation(operationId = "getMe")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403")
  })
  public User getMe(@AuthenticationPrincipal Jwt jwt) {
    invitations.acceptFor(jwt.getSubject());
    return users.current(jwt.getSubject());
  }

  @DeleteMapping("/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "deleteAccount")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "409")
  })
  public void deleteAccount(@AuthenticationPrincipal Jwt jwt) {
    accounts.delete(jwt.getSubject(), jwt.getClaimAsInstant("auth_time"));
  }
}
