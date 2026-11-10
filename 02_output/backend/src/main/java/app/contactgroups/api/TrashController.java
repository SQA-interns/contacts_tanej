package app.contactgroups.api;

import app.contactgroups.service.TrashService;
import app.contactgroups.service.TrashService.TrashPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Delete to Trash, restore, the Trash view and delete forever of contacts (US-012). */
@RestController
public class TrashController {

  private final TrashService trash;

  public TrashController(TrashService trash) {
    this.trash = trash;
  }

  @GetMapping("/groups/{groupId}/trash-contacts")
  @Operation(operationId = "listTrashedContacts")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public TrashPage list(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestParam(required = false) Integer page) {
    return trash.list(groupId, jwt.getSubject(), page);
  }

  @PostMapping("/groups/{groupId}/contacts/{contactId}/trash")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "trashContact")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409")
  })
  public void trash(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId, @PathVariable UUID contactId) {
    trash.trash(groupId, jwt.getSubject(), contactId);
  }

  @PostMapping("/groups/{groupId}/contacts/{contactId}/restore")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "restoreContact")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409")
  })
  public void restore(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId, @PathVariable UUID contactId) {
    trash.restore(groupId, jwt.getSubject(), contactId);
  }

  @DeleteMapping("/groups/{groupId}/contacts/{contactId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "eraseContact")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409")
  })
  public void erase(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId, @PathVariable UUID contactId) {
    trash.erase(groupId, jwt.getSubject(), contactId);
  }
}
