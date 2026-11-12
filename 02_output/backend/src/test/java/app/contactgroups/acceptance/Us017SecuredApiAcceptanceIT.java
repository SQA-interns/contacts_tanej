package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * US-017 Secured API and OpenAPI: AC-017-01 to AC-017-07, AC-017-09 to AC-017-14, EX-017-01 to
 * EX-017-03 and EX-017-05 to EX-017-09. The rate limit (AC-017-08, EX-017-04) is in
 * Us017RateLimitAcceptanceIT. The contract is the copy of docs/02_contracts/openapi.yaml in the
 * test resources.
 */
class Us017SecuredApiAcceptanceIT extends AcceptanceBase {

  private static final List<String> METHODS = List.of("get", "post", "put", "patch", "delete");

  /** One operation of the contract. */
  private record Operation(
      String method, String path, String id, boolean hasBody, List<String> statuses) {}

  @SuppressWarnings("unchecked")
  private static List<Operation> contract() {
    try (InputStream in =
        Us017SecuredApiAcceptanceIT.class.getResourceAsStream("/acceptance/openapi.yaml")) {
      Map<String, Object> doc = new Yaml().load(in);
      Map<String, Object> paths = (Map<String, Object>) doc.get("paths");
      List<Operation> out = new ArrayList<>();
      for (var p : paths.entrySet()) {
        Map<String, Object> item = (Map<String, Object>) p.getValue();
        for (String m : METHODS) {
          if (item.containsKey(m)) {
            Map<String, Object> op = (Map<String, Object>) item.get(m);
            List<String> statuses = new ArrayList<>();
            for (Object code : ((Map<Object, Object>) op.get("responses")).keySet()) {
              statuses.add(String.valueOf(code));
            }
            out.add(
                new Operation(
                    m.toUpperCase(),
                    p.getKey(),
                    String.valueOf(op.get("operationId")),
                    op.containsKey("requestBody"),
                    statuses));
          }
        }
      }
      return out;
    } catch (Exception e) {
      throw new AssertionError("The contract copy cannot be read: " + e, e);
    }
  }

  private static String fill(String path, String groupId) {
    return path.replace("{groupId}", groupId)
        .replace("{userId}", "uid-x")
        .replace("{number}", "1")
        .replaceAll("\\{[A-Za-z]+\\}", UUID.randomUUID().toString());
  }

  private Res call(Operation op, String groupId, String authorization) {
    return request(
        op.method(), fill(op.path(), groupId), authorization, op.hasBody() ? "{}" : null);
  }

  private String bearer(Who who) {
    return "Bearer " + token(who);
  }

  @Test
  void AC_017_01_everyEndpointExceptTheHealthCheckNeedsAToken() {
    String gid = group(ANA, "Konferenca 2026");
    List<Operation> ops = contract();
    assertThat(ops.size()).isGreaterThanOrEqualTo(40);
    for (Operation op : ops) {
      Res r = call(op, gid, null);
      if (op.path().equals("/health")) {
        assertThat(r.status()).as(op.id()).isEqualTo(200);
      } else {
        r.expectError(401, "UNAUTHENTICATED");
        assertThat(r.text()).as(op.id()).doesNotContain("Konferenca 2026");
      }
    }
  }

  @Test
  void AC_017_02_anExpiredTokenAndATokenOfAnotherProjectAreRefused() {
    group(ANA, "Konferenca 2026");
    Instant now = clock.instant();
    String expired =
        token(ANA, PROJECT, now.minusSeconds(7200), now.minusSeconds(3600), now.minusSeconds(7200));
    String foreign = token(ANA, "another-project", now.minusSeconds(5), now.plusSeconds(3600), now);
    request("GET", "/groups", "Bearer " + expired, null).expectError(401, "UNAUTHENTICATED");
    request("GET", "/groups", "Bearer " + foreign, null).expectError(401, "UNAUTHENTICATED");
    request("GET", "/groups", "Bearer not-a-token", null).expectError(401, "UNAUTHENTICATED");
  }

  @Test
  void AC_017_03_anUnverifiedEmailGetsForbiddenWithTheCode() {
    String gid = group(ANA, "Konferenca 2026");
    get(ANA.unverified(), "/groups").expectError(403, "EMAIL_NOT_VERIFIED");
    get(ANA.unverified(), "/groups/" + gid).expectError(403, "EMAIL_NOT_VERIFIED");
    get(ANA.unverified(), "/groups/" + gid + "/contacts/" + UUID.randomUUID())
        .expectError(403, "EMAIL_NOT_VERIFIED");
    get(null, "/health").expect(200);
  }

  @Test
  void AC_017_04_aNonMembersAnswerIsIdenticalToTheAnswerForAMissingGroup() {
    String b = group(BORIS, "Poroka");
    newContact(BORIS, b, j("lastName", "Tajni"));
    Res foreign = post(ANA, "/groups/" + b + "/contacts/search", j()).expectError(404, "NOT_FOUND");
    Res missing =
        post(ANA, "/groups/" + UUID.randomUUID() + "/contacts/search", j())
            .expectError(404, "NOT_FOUND");
    assertThat(foreign.text()).isEqualTo(missing.text());
    assertThat(foreign.text()).doesNotContain("Tajni", "Poroka");
  }

  @Test
  void AC_017_05_theGroupIsTakenFromThePathAndNeverFromTheBody() {
    String a = group(ANA, "Konferenca 2026");
    String b = group(ANA, "Poroka");
    post(ANA, "/groups/" + a + "/contacts", j("lastName", "Novak", "groupId", b)).expect(201);
    assertThat(search(ANA, a, "").at("/total").asInt()).isEqualTo(1);
    assertThat(search(ANA, b, "").at("/total").asInt()).isZero();
  }

  @Test
  void AC_017_06_aViewerCannotExport() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, PETRA, "VIEWER");
    post(PETRA, "/groups/" + gid + "/exports", j("language", "sl", "filter", j()))
        .expectError(403, "FORBIDDEN");
  }

  @TestFactory
  Stream<DynamicTest> AC_017_07_aNonMemberGetsNotFoundOnEveryOperationWithAGroupInItsPath() {
    String gid = group(ANA, "Konferenca 2026");
    return contract().stream()
        .filter(op -> op.path().contains("{groupId}"))
        .map(
            op ->
                DynamicTest.dynamicTest(
                    "AC-017-07 " + op.id() + " " + op.method() + " " + op.path(),
                    () -> call(op, gid, bearer(BORIS)).expectError(404, "NOT_FOUND")));
  }

  @Test
  void AC_017_09_aBodyOfOneMegabyteAndOneByteIsRefusedAndNothingIsStored() {
    String gid = group(ANA, "Konferenca 2026");
    byte[] body = padded("{\"lastName\":\"Novak\"}", 1024 * 1024 + 1);
    request("POST", "/groups/" + gid + "/contacts", bearer(ANA), body)
        .expectError(413, "PAYLOAD_TOO_LARGE");
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
  }

  @Test
  void AC_017_10_everyFailureHasACodeAndAMessageAndNoStackTrace() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, PETRA, "VIEWER");
    List<Res> failures =
        List.of(
            request("GET", "/groups", null, null),
            get(ANA.unverified(), "/groups"),
            get(ANA, "/groups/" + UUID.randomUUID()),
            post(PETRA, "/groups/" + gid + "/contacts", j("lastName", "x")),
            post(ANA, "/groups/" + gid + "/contacts", j("email", "ana.novak@")),
            request("PUT", "/groups", bearer(ANA), "{}"),
            post(ANA, "/groups/" + gid + "/trash", j("name", "napacno")));
    for (Res r : failures) {
      JsonNode body = r.json();
      assertThat(body.path("code").asText()).as(r.text()).isNotBlank();
      assertThat(body.path("message").asText()).as(r.text()).isNotBlank();
      assertThat(body.path("status").asInt()).as(r.text()).isEqualTo(r.status());
      assertThat(r.text())
          .doesNotContain("Exception", "\tat ", "at app.", "org.springframework", "SELECT ");
    }
  }

  @Test
  void AC_017_11_theOpenApiDescriptionListsEveryEndpointOfTheContract() {
    Res served = get(ANA, "/v3/api-docs").expect(200);
    JsonNode paths = served.at("/paths");
    for (Operation op : contract()) {
      JsonNode item = paths.path(op.path());
      assertThat(item.isMissingNode()).as("path %s in /v3/api-docs", op.path()).isFalse();
      JsonNode operation = item.path(op.method().toLowerCase());
      assertThat(operation.isMissingNode()).as("%s %s", op.method(), op.path()).isFalse();
      assertThat(operation.path("operationId").asText())
          .as("operationId of %s", op.path())
          .isEqualTo(op.id());
      for (String status : op.statuses()) {
        assertThat(operation.path("responses").has(status))
            .as("response %s of %s %s", status, op.method(), op.path())
            .isTrue();
      }
    }
  }

  @Test
  void AC_017_12_theHealthCheckAnswers503WithoutTheDatabaseAnd200WithIt() {
    get(null, "/health").expect(200);
    POSTGRES.getDockerClient().pauseContainerCmd(POSTGRES.getContainerId()).exec();
    try {
      Res down = get(null, "/health");
      assertThat(down.status()).isEqualTo(503);
      assertThat(down.at("/status").asText()).isEqualTo("DOWN");
    } finally {
      POSTGRES.getDockerClient().unpauseContainerCmd(POSTGRES.getContainerId()).exec();
    }
    eventually(() -> get(null, "/health").status() == 200, "the health check recovers");
  }

  @Test
  void AC_017_13_anInvalidContactGivesFourHundredWithCodeAndField() {
    String gid = group(ANA, "Konferenca 2026");
    post(ANA, "/groups/" + gid + "/contacts", j("lastName", "Novak", "email", "ana.novak@"))
        .expectFieldError("email", "INVALID_FORMAT");
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
  }

  @Test
  void AC_017_14_theSecondOfTwoSavesFromTheSameStartGetsConflictWithTheCurrentValues() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    JsonNode c = newContact(ANA, gid, j("lastName", "Novak", "phone", "+386 40 123 456"));
    JsonNode first = openContact(ANA, gid, c.path("id").asText());
    JsonNode second = openContact(MARKO, gid, c.path("id").asText());
    save(ANA, gid, first, "phone", "+386 41 555 123").expect(200);
    Res r = save(MARKO, gid, second, "phone", "+386 42 000 000").expectError(409, "CONFLICT");
    assertThat(r.at("/current/phone").asText()).isEqualTo("+386 41 555 123");
    assertThat(r.at("/current/revision").asLong()).isGreaterThan(second.path("revision").asLong());
  }

  @Test
  void EX_017_01_aBasicAuthorizationHeaderIsRefused() {
    String basic =
        "Basic "
            + Base64.getEncoder().encodeToString("ana:Geslo1234".getBytes(StandardCharsets.UTF_8));
    get(ANA, "/groups").expect(200);
    request("GET", "/groups", basic, null).expectError(401, "UNAUTHENTICATED");
  }

  @Test
  void EX_017_02_aGroupIdOfTheWrongFormatIsNotFound() {
    group(ANA, "Konferenca 2026");
    get(ANA, "/groups/not-an-id/members").expectError(404, "NOT_FOUND");
    get(ANA, "/groups/123").expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_017_03_aRemovedMemberWithAStillValidTokenGetsNotFound() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    String oldToken = bearer(MARKO);
    delete(ANA, "/groups/" + gid + "/members/" + MARKO.uid()).expect(204);
    request("GET", "/groups/" + gid, oldToken, null).expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_017_05_aBodyOfExactlyOneMegabyteIsProcessed() {
    String gid = group(ANA, "Konferenca 2026");
    byte[] body = padded("{\"lastName\":\"Novak\"}", 1024 * 1024);
    request("POST", "/groups/" + gid + "/contacts", bearer(ANA), body).expect(201);
    assertThat(search(ANA, gid, "").at("/total").asInt()).isEqualTo(1);
  }

  @Test
  void EX_017_06_aGroupInTrashRequestedByANonOwnerMemberIsNotFound() {
    String gid = group(ANA, "Konferenca 2026");
    join(gid, ANA, MARKO, "EDITOR");
    post(ANA, "/groups/" + gid + "/trash", j("name", "Konferenca 2026")).expect(204);
    get(MARKO, "/groups/" + gid).expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_017_07_aMethodNotAllowedOnAPathGivesAnErrorResponseWithStatus405() {
    String gid = group(ANA, "Konferenca 2026");
    Res r = request("PUT", "/groups", bearer(ANA), "{}").expect(405);
    assertThat(r.code()).isEqualTo("METHOD_NOT_ALLOWED");
    assertThat(r.at("/status").asInt()).isEqualTo(405);
    request("DELETE", "/groups/" + gid + "/history", bearer(ANA), null).expect(405);
  }

  @Test
  void EX_017_08_aTrashedContactIdOfAnotherGroupIsNotFound() {
    String a = group(ANA, "Konferenca 2026");
    String b = group(BORIS, "Poroka");
    String c = newContact(BORIS, b, j("lastName", "Tuj")).path("id").asText();
    trash(BORIS, b, c);
    get(ANA, "/groups/" + a + "/contacts/" + c).expectError(404, "NOT_FOUND");
    get(ANA, "/groups/" + b + "/contacts/" + c).expectError(404, "NOT_FOUND");
  }

  @Test
  void EX_017_09_theHealthCheckNeedsNoToken() {
    Res r = get(null, "/health").expect(200);
    assertThat(r.at("/status").asText()).isEqualTo("UP");
    get(null, "/groups").expectError(401, "UNAUTHENTICATED");
    get(ANA, "/groups").expect(200);
  }

  private static byte[] padded(String json, int size) {
    byte[] raw = json.getBytes(StandardCharsets.UTF_8);
    byte[] out = new byte[size];
    System.arraycopy(raw, 0, out, 0, raw.length);
    java.util.Arrays.fill(out, raw.length, size, (byte) ' ');
    return out;
  }
}
