package app.contactgroups.api;

import app.contactgroups.domain.ContactSort;
import app.contactgroups.service.ExportService;
import app.contactgroups.service.ExportService.ExportFile;
import app.contactgroups.service.ExportService.Filter;
import app.contactgroups.service.ExportService.Selection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Export of the ticked contacts to an Excel file (US-011). */
@RestController
public class ExportController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  /** The body of the export (contract {@code ExportRequest}). */
  public record ExportRequest(
      String language, ContactSort sort, List<UUID> contactIds, Filter filter) {}

  private final ExportService export;

  public ExportController(ExportService export) {
    this.export = export;
  }

  @PostMapping("/groups/{groupId}/exports")
  @Operation(operationId = "exportContacts")
  @ApiResponses({
    @ApiResponse(responseCode = "200"),
    @ApiResponse(responseCode = "400"),
    @ApiResponse(responseCode = "401"),
    @ApiResponse(responseCode = "403"),
    @ApiResponse(responseCode = "404"),
    @ApiResponse(responseCode = "413")
  })
  public ResponseEntity<byte[]> export(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID groupId,
      @RequestBody ExportRequest request) {
    ExportFile file =
        export.export(
            groupId,
            jwt.getSubject(),
            request.language(),
            request.sort(),
            new Selection(request.contactIds(), request.filter()));
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(file.fileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(file.bytes());
  }
}
