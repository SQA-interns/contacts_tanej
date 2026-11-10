package app.contactgroups.api;

import app.contactgroups.domain.User;
import app.contactgroups.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user (operation {@code getMe}). */
@RestController
public class AccountController {

  private final UserService users;

  public AccountController(UserService users) {
    this.users = users;
  }

  @GetMapping("/me")
  @Operation(operationId = "getMe")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403")
  })
  public User getMe(@AuthenticationPrincipal Jwt jwt) {
    return users.current(jwt.getSubject());
  }
}
