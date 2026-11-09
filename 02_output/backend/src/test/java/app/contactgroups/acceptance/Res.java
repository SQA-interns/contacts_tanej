package app.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;

/** One HTTP answer of the system under test. */
public record Res(int status, byte[] bytes, HttpHeaders headers) {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public String text() {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public JsonNode json() {
    try {
      return MAPPER.readTree(bytes);
    } catch (Exception e) {
      throw new AssertionError("The answer is not JSON (status " + status + "): " + text(), e);
    }
  }

  /** Value at a JSON pointer, for example {@code /items/0/id}; missing gives a missing node. */
  public JsonNode at(String pointer) {
    return json().at(pointer);
  }

  public String code() {
    return json().path("code").asText();
  }

  /** Asserts the status and returns this answer. */
  public Res expect(int expected) {
    assertThat(status).as("status of the answer, body: %s", shortText()).isEqualTo(expected);
    return this;
  }

  /** Asserts status and error code. */
  public Res expectError(int expectedStatus, String expectedCode) {
    expect(expectedStatus);
    assertThat(code()).as("error code, body: %s", shortText()).isEqualTo(expectedCode);
    return this;
  }

  /** Asserts a 400 that names the field with the rule code. */
  public Res expectFieldError(String field, String fieldCode) {
    expectError(400, "VALIDATION_FAILED");
    boolean found = false;
    for (JsonNode e : json().path("errors")) {
      if (field.equals(e.path("field").asText()) && fieldCode.equals(e.path("code").asText())) {
        found = true;
      }
    }
    assertThat(found).as("field error %s/%s in %s", field, fieldCode, shortText()).isTrue();
    return this;
  }

  /** Asserts a 400 that carries a field error with the rule code, whatever the field. */
  public Res expectFieldCode(String fieldCode) {
    expectError(400, "VALIDATION_FAILED");
    boolean found = false;
    for (JsonNode e : json().path("errors")) {
      if (fieldCode.equals(e.path("code").asText())) {
        found = true;
      }
    }
    assertThat(found).as("a field error %s in %s", fieldCode, shortText()).isTrue();
    return this;
  }

  /** Asserts a 400 with a field error whose field starts with the prefix (for example tags). */
  public Res expectFieldErrorStarting(String fieldPrefix, String fieldCode) {
    expectError(400, "VALIDATION_FAILED");
    boolean found = false;
    for (JsonNode e : json().path("errors")) {
      if (e.path("field").asText().startsWith(fieldPrefix)
          && fieldCode.equals(e.path("code").asText())) {
        found = true;
      }
    }
    assertThat(found).as("field error %s*/%s in %s", fieldPrefix, fieldCode, shortText()).isTrue();
    return this;
  }

  private String shortText() {
    String t = status == 200 && bytes.length > 600 ? "(" + bytes.length + " bytes)" : text();
    return t.length() > 600 ? t.substring(0, 600) + "..." : t;
  }
}
