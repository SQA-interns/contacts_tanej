package app.contactgroups.api;

import app.contactgroups.service.HistoryService;
import app.contactgroups.service.HistoryService.HistoryPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The History of a group and of a contact; read only, there is no way to change it (R-013-10). */
@RestController
public class HistoryController {

  private final HistoryService history;

  public HistoryController(HistoryService history) {
    this.history = history;
  }

  @GetMapping("/groups/{groupId}/history")
  @Operation(operationId = "listGroupHistory")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public HistoryPage group(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestParam(required = false) Integer page) {
    return history.ofGroup(groupId, jwt.getSubject(), page);
  }

  @GetMapping("/groups/{groupId}/contacts/{contactId}/history")
  @Operation(operationId = "listContactHistory")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public HistoryPage contact(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId,
      @RequestParam(required = false) Integer page) {
    return history.ofContact(groupId, jwt.getSubject(), contactId, page);
  }
}
