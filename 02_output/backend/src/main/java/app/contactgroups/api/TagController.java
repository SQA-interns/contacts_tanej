package app.contactgroups.api;

import app.contactgroups.domain.Tag;
import app.contactgroups.service.TagService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The tags of a group (US-006 list; US-007 adds rename and delete). */
@RestController
public class TagController {

  private final TagService tags;

  public TagController(TagService tags) {
    this.tags = tags;
  }

  @GetMapping("/groups/{groupId}/tags")
  @Operation(operationId = "listTags")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public List<Tag> list(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestParam(required = false) String q) {
    return tags.list(groupId, jwt.getSubject(), q);
  }

  @PatchMapping("/groups/{groupId}/tags/{tagId}")
  @Operation(operationId = "renameTag")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "409")
  })
  public Tag rename(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @PathVariable UUID tagId,
      @RequestBody TagRequest request) {
    return tags.rename(groupId, jwt.getSubject(), tagId, request.name());
  }

  @DeleteMapping("/groups/{groupId}/tags/{tagId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "deleteTag")
  @ApiResponses({
    @ApiResponse(responseCode = "204"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public void delete(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId, @PathVariable UUID tagId) {
    tags.delete(groupId, jwt.getSubject(), tagId);
  }

  /** The body of a rename (contract {@code TagRequest}). */
  public record TagRequest(String name) {}
}
