package app.contactgroups.api;

import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactInput;
import app.contactgroups.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Create, open and save a contact (US-005). */
@RestController
public class ContactController {

  /**
   * The body of create and save (contract {@code ContactRequest}); {@code revision} only on save.
   */
  public record ContactRequest(
      String firstName,
      String lastName,
      String email,
      String phone,
      String company,
      String jobTitle,
      String street,
      String postalCode,
      String city,
      String country,
      String notes,
      Long revision) {

    ContactInput fields() {
      return new ContactInput(
          firstName,
          lastName,
          email,
          phone,
          company,
          jobTitle,
          street,
          postalCode,
          city,
          country,
          notes);
    }
  }

  private final ContactService contacts;

  public ContactController(ContactService contacts) {
    this.contacts = contacts;
  }

  @PostMapping("/groups/{groupId}/contacts")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "createContact")
  @ApiResponses({
    @ApiResponse(responseCode = "201"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "413"),
    @ApiResponse(responseCode = "429")
  })
  public Contact create(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestBody ContactRequest request) {
    return contacts.create(groupId, jwt.getSubject(), request.fields());
  }

  @GetMapping("/groups/{groupId}/contacts/{contactId}")
  @Operation(operationId = "getContact")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404")
  })
  public Contact get(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId, @PathVariable UUID contactId) {
    return contacts.open(groupId, jwt.getSubject(), contactId);
  }

  @PutMapping("/groups/{groupId}/contacts/{contactId}")
  @Operation(operationId = "saveContact")
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
  public Contact save(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @PathVariable UUID contactId,
      @RequestBody ContactRequest request) {
    return contacts.save(
        groupId, jwt.getSubject(), contactId, request.fields(), request.revision());
  }
}
