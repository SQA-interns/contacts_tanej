package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * AC-017-07, EX-017-02 and SR-03 without any data: every operation of the contract whose path has a
 * group answers the one 404 for a group that does not exist and for a malformed group id, and the
 * answers are byte for byte the same. The frozen AC-017-07 adds the non-member of a real group.
 */
class IsolationMatrixIT extends AcceptanceBase {

  private static final List<String> METHODS = List.of("get", "post", "put", "patch", "delete");

  private record Operation(String method, String path, String id, boolean hasBody) {}

  @SuppressWarnings("unchecked")
  private static List<Operation> operations() {
    try (InputStream in = IsolationMatrixIT.class.getResourceAsStream("/acceptance/openapi.yaml")) {
      Map<String, Object> paths =
          (Map<String, Object>) ((Map<String, Object>) new Yaml().load(in)).get("paths");
      List<Operation> out = new ArrayList<>();
      for (var p : paths.entrySet()) {
        for (String m : METHODS) {
          Map<String, Object> op = ((Map<String, Map<String, Object>>) p.getValue()).get(m);
          if (op != null && p.getKey().contains("{groupId}")) {
            out.add(
                new Operation(
                    m.toUpperCase(),
                    p.getKey(),
                    String.valueOf(op.get("operationId")),
                    op.containsKey("requestBody")));
          }
        }
      }
      return out;
    } catch (Exception e) {
      throw new AssertionError("The contract copy cannot be read: " + e, e);
    }
  }

  private Res call(Operation op, String groupId) {
    String path =
        op.path()
            .replace("{groupId}", groupId)
            .replace("{userId}", "uid-x")
            .replace("{number}", "1")
            .replaceAll("\\{[A-Za-z]+\\}", UUID.randomUUID().toString());
    return request(op.method(), path, "Bearer " + token(ANA), op.hasBody() ? "{}" : null);
  }

  @TestFactory
  Stream<DynamicTest> everyGroupOperationAnswersTheSame404ForAMissingAndAMalformedGroup() {
    String reference = get(ANA, "/groups/" + UUID.randomUUID() + "/nowhere").expect(404).text();
    return operations().stream()
        .map(
            op ->
                DynamicTest.dynamicTest(
                    op.id() + " " + op.method() + " " + op.path(),
                    () -> {
                      Res missing =
                          call(op, UUID.randomUUID().toString()).expectError(404, "NOT_FOUND");
                      Res malformed = call(op, "not-an-id").expectError(404, "NOT_FOUND");
                      assertThat(missing.text()).isEqualTo(reference).isEqualTo(malformed.text());
                    }));
  }
}
