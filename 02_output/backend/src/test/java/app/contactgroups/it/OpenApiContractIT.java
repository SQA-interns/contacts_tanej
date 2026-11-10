package app.contactgroups.it;

import static app.contactgroups.acceptance.Who.ANA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * ADR-09 drift test, direction code to contract: every operation that the running code publishes at
 * {@code /v3/api-docs} must exist in the contract with the same operationId, the same response
 * statuses and the same parameters. The other direction (every contract operation is built) is
 * AC-017-11 in the frozen tests and passes when the last story is done.
 */
class OpenApiContractIT extends AcceptanceBase {

  private static final List<String> METHODS = List.of("get", "post", "put", "patch", "delete");

  @SuppressWarnings("unchecked")
  private static Map<String, Object> contract() {
    try (InputStream in = OpenApiContractIT.class.getResourceAsStream("/acceptance/openapi.yaml")) {
      return new Yaml().load(in);
    } catch (Exception e) {
      throw new AssertionError("The contract copy cannot be read: " + e, e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Set<String> contractParameters(
      Map<String, Object> doc, Map<String, Object> item, Map<String, Object> op) {
    Map<String, Object> components =
        (Map<String, Object>) ((Map<String, Object>) doc.get("components")).get("parameters");
    Set<String> names = new HashSet<>();
    List<Object> declared =
        new java.util.ArrayList<>((List<Object>) item.getOrDefault("parameters", List.of()));
    declared.addAll((List<Object>) op.getOrDefault("parameters", List.of()));
    for (Object p : declared) {
      Map<String, Object> param = (Map<String, Object>) p;
      if (param.containsKey("$ref")) {
        String key = param.get("$ref").toString().replace("#/components/parameters/", "");
        param = (Map<String, Object>) components.get(key);
      }
      names.add(param.get("in") + ":" + param.get("name"));
    }
    return names;
  }

  @SuppressWarnings("unchecked")
  @Test
  void everyOperationOfTheCodeIsInTheContractWithTheSameShape() {
    Map<String, Object> doc = contract();
    Map<String, Object> paths = (Map<String, Object>) doc.get("paths");
    JsonNode served = get(ANA, "/v3/api-docs").expect(200).at("/paths");
    assertThat(served.size()).as("paths served").isPositive();
    Map<String, String> problems = new HashMap<>();
    served
        .properties()
        .forEach(
            path -> {
              Map<String, Object> item = (Map<String, Object>) paths.get(path.getKey());
              for (String method : METHODS) {
                JsonNode op = path.getValue().path(method);
                if (op.isMissingNode()) {
                  continue;
                }
                String where = method.toUpperCase() + " " + path.getKey();
                if (item == null || !item.containsKey(method)) {
                  problems.put(where, "not in the contract");
                  continue;
                }
                Map<String, Object> expected = (Map<String, Object>) item.get(method);
                if (!expected.get("operationId").equals(op.path("operationId").asText())) {
                  problems.put(where + " operationId", op.path("operationId").asText());
                }
                Set<String> statuses = new HashSet<>();
                op.path("responses").fieldNames().forEachRemaining(statuses::add);
                Set<String> wanted = new HashSet<>();
                ((Map<Object, Object>) expected.get("responses"))
                    .keySet()
                    .forEach(k -> wanted.add(String.valueOf(k)));
                if (!statuses.equals(wanted)) {
                  problems.put(where + " responses", statuses + " instead of " + wanted);
                }
                Set<String> params = new HashSet<>();
                op.path("parameters")
                    .forEach(
                        p -> params.add(p.path("in").asText() + ":" + p.path("name").asText()));
                if (!params.equals(contractParameters(doc, item, expected))) {
                  problems.put(where + " parameters", params.toString());
                }
                if (op.has("requestBody") != expected.containsKey("requestBody")) {
                  problems.put(where + " requestBody", "differs");
                }
              }
            });
    assertThat(problems).as("drift between the code and docs/02_contracts/openapi.yaml").isEmpty();
  }

  @Test
  void theHealthCheckAndMeAreDescribed() {
    JsonNode paths = get(ANA, "/v3/api-docs").expect(200).at("/paths");
    assertThat(paths.at("/~1health/get/operationId").asText()).isEqualTo("getHealth");
    assertThat(paths.at("/~1me/get/operationId").asText()).isEqualTo("getMe");
  }
}
