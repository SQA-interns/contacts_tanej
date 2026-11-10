package app.contactgroups.api;

import app.contactgroups.domain.ConditionInput;
import app.contactgroups.domain.ContactPage;
import app.contactgroups.domain.ContactSort;
import app.contactgroups.service.SearchService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The contact list: search text, sort and page in a POST body, never in a URL (SR-20). */
@RestController
public class SearchController {

  /** The body of the search (contract {@code ContactSearchRequest}). */
  public record SearchRequest(
      String q, List<ConditionInput> conditions, ContactSort sort, Integer page) {}

  private final SearchService search;

  public SearchController(SearchService search) {
    this.search = search;
  }

  @GetMapping("/groups/{groupId}/companies")
  @Operation(operationId = "listCompanies")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public List<String> companies(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestParam(required = false) String q) {
    return search.companies(groupId, jwt.getSubject(), q);
  }

  @PostMapping("/groups/{groupId}/contacts/search")
  @Operation(operationId = "searchContacts")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "413"),
    @ApiResponse(responseCode = "429")
  })
  public ContactPage search(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestBody SearchRequest request) {
    return search.search(
        groupId,
        jwt.getSubject(),
        request.q(),
        request.conditions(),
        request.sort(),
        request.page());
  }
}
