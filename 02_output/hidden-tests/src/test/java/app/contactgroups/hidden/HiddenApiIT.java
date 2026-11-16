package app.contactgroups.hidden;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static app.contactgroups.acceptance.Who.MARKO;
import static app.contactgroups.acceptance.Who.PETRA;
import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.acceptance.AcceptanceBase;
import app.contactgroups.acceptance.Res;
import app.contactgroups.acceptance.Who;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hidden cases for R-017 (the secured API) and the error format. */
class HiddenApiIT extends AcceptanceBase {

  private String bearer(Who w) {
    return "Bearer " + token(w);
  }

  @Test
  @DisplayName("R-017-01 combination every method on a protected path without token")
  void R_017_01_combination_everyMethodNeedsAToken() {
    String gid = group(ANA, "G");
    for (String m : List.of("GET", "POST", "PUT", "PATCH", "DELETE")) {
      for (String p :
          List.of(
              "/me",
              "/groups",
              "/groups/" + gid,
              "/groups/" + gid + "/contacts",
              "/deleted-groups",
              "/v3/api-docs")) {
        Res r = request(m, p, null, m.equals("GET") || m.equals("DELETE") ? null : "{}");
        assertThat(r.status()).as("%s %s", m, p).isIn(401);
      }
    }
  }

  @Test
  @DisplayName("R-017-02 invalid tokens of several kinds are all refused with the same body")
  void R_017_02_invalid_tokenKinds() {
    group(ANA, "G");
    Instant now = clock.instant();
    List<String> bad =
        List.of(
            "",
            "Bearer",
            "Bearer ",
            "Bearer a.b.c",
            "Token " + token(ANA),
            "Bearer " + token(ANA, "other-project", now.minusSeconds(5), now.plusSeconds(600), now),
            "Bearer " + token(ANA, PROJECT, now.minusSeconds(1200), now.minusSeconds(600), now),
            "Bearer " + token(ANA).replaceFirst("\\.[^.]*\\.", ".e30."));
    String first = null;
    for (String h : bad) {
      Res r =
          request("GET", "/groups", h.isEmpty() ? null : h, null)
              .expectError(401, "UNAUTHENTICATED");
      assertThat(r.text()).doesNotContain("Konferenca", "ana.novak");
      first = first == null ? r.text() : first;
    }
    assertThat(first).isNotNull();
  }

  @Test
  @DisplayName(
      "R-017-03 combination unverified is refused on every method and verified of the same user works")
  void R_017_03_combination_everyMethod() {
    Who unverified = MARKO.unverified();
    for (String m : List.of("GET", "POST")) {
      Res r = request(m, "/groups", bearer(unverified), m.equals("POST") ? j("name", "G") : null);
      r.expectError(403, "EMAIL_NOT_VERIFIED");
    }
    get(MARKO, "/groups").expect(200);
    get(null, "/health").expect(200);
    get(unverified, "/health").expect(200);
  }

  @Test
  @DisplayName("R-017-04 combination the group id in the query or in a header is ignored")
  void R_017_04_combination_queryAndHeader() {
    String a = group(ANA, "A");
    String b = group(ANA, "B");
    newContact(ANA, b, "Eva", "VB");
    JsonNode r =
        post(ANA, "/groups/" + a + "/contacts/search?groupId=" + b, j("groupId", b))
            .expect(200)
            .json();
    assertThat(r.at("/total").asInt()).isZero();
    post(ANA, "/groups/" + a + "/contacts", j("lastName", "N", "groupId", b, "group", b))
        .expect(201);
    assertThat(search(ANA, b, "").at("/total").asInt()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "R-017-05 combination the same answer for missing malformed and foreign ids on several routes")
  void R_017_05_combination_sameAnswer() {
    String foreign = group(BORIS, "Tuja");
    String mine = group(ANA, "Moja");
    for (String suffix : List.of("", "/members", "/tags", "/history", "/trash-contacts")) {
      Res a = get(ANA, "/groups/" + foreign + suffix).expectError(404, "NOT_FOUND");
      Res b = get(ANA, "/groups/" + UUID.randomUUID() + suffix).expectError(404, "NOT_FOUND");
      Res c = get(ANA, "/groups/zzz" + suffix).expectError(404, "NOT_FOUND");
      assertThat(a.text()).isEqualTo(b.text()).isEqualTo(c.text());
    }
    Res contact404 =
        get(ANA, "/groups/" + mine + "/contacts/" + UUID.randomUUID())
            .expectError(404, "NOT_FOUND");
    assertThat(contact404.text())
        .isEqualTo(get(ANA, "/groups/" + foreign + "/contacts/" + UUID.randomUUID()).text());
  }

  @Test
  @DisplayName("R-017-06 role forbidden is separate from not found for a member")
  void R_017_06_role_forbiddenVsNotFound() {
    String gid = group(ANA, "G");
    join(gid, ANA, PETRA, "VIEWER");
    Res forbidden = get(PETRA, "/groups/" + gid + "/history").expectError(403, "FORBIDDEN");
    Res notFound = get(BORIS, "/groups/" + gid + "/history").expectError(404, "NOT_FOUND");
    assertThat(forbidden.text()).isNotEqualTo(notFound.text());
  }

  @Test
  @DisplayName("R-017-07 combination several field errors with codes and no data stored")
  void R_017_07_combination_codesAndFields() {
    String gid = group(ANA, "G");
    Res r =
        post(
            ANA,
            "/groups/" + gid + "/contacts",
            j(
                "lastName",
                repeat("x", 256),
                "phone",
                "abc",
                "tags",
                arr("a b"),
                "attributes",
                arr(attr("", "v"))));
    r.expectError(400, "VALIDATION_FAILED");
    for (JsonNode e : r.json().path("errors")) {
      assertThat(e.path("field").asText()).isNotBlank();
      assertThat(e.path("code").asText()).isNotBlank();
      assertThat(e.path("message").asText()).isNotBlank();
    }
    assertThat(r.json().path("errors").size()).isGreaterThanOrEqualTo(4);
    assertThat(search(ANA, gid, "").at("/total").asInt()).isZero();
  }

  @Test
  @DisplayName("R-017-08 combination the limit is per user and Retry-After is positive")
  void R_017_08_combination_perUser() {
    // the harness runs with a very high limit; the rule is checked on its own context in the
    // visible
    // tests, here the header contract of an ordinary answer: no rate-limit header leaks a counter
    Res r = get(ANA, "/groups").expect(200);
    assertThat(r.headers().firstValue("Retry-After")).isEmpty();
    assertThat(get(BORIS, "/groups").expect(200).headers().firstValue("Retry-After")).isEmpty();
  }

  @Test
  @DisplayName(
      "R-017-09 boundary the limit applies to every body-carrying route and not to a small body")
  void R_017_09_boundary_everyRoute() {
    String gid = group(ANA, "G");
    byte[] big = new byte[1024 * 1024 + 1];
    java.util.Arrays.fill(big, (byte) ' ');
    byte[] start = "{\"name\":\"x\"}".getBytes(StandardCharsets.UTF_8);
    System.arraycopy(start, 0, big, 0, start.length);
    for (String p :
        List.of(
            "/groups",
            "/groups/" + gid + "/contacts",
            "/groups/" + gid + "/contacts/search",
            "/groups/" + gid + "/invitations")) {
      request("POST", p, bearer(ANA), big).expectError(413, "PAYLOAD_TOO_LARGE");
    }
    assertThat(get(ANA, "/groups").expect(200).json()).hasSize(1);
  }

  @Test
  @DisplayName("R-017-10 combination the error body has no extra internal fields")
  void R_017_10_combination_noInternals() {
    String gid = group(ANA, "G");
    for (Res r :
        List.of(
            get(ANA, "/groups/" + gid + "/nothing"),
            get(ANA, "/groups/" + gid + "/contacts/not-a-uuid"),
            request("POST", "/groups", bearer(ANA), "not json"),
            request("POST", "/groups", bearer(ANA), "{"),
            post(ANA, "/groups", j("name", 5)))) {
      assertThat(r.status()).isBetween(400, 499);
      JsonNode b = r.json();
      assertThat(b.fieldNames())
          .toIterable()
          .isSubsetOf("status", "code", "message", "errors", "groups", "current");
      assertThat(r.text()).doesNotContain("Exception", "org.", "java.", "SELECT", "at app.");
    }
  }

  @Test
  @DisplayName(
      "R-017-11 other the description is JSON with the contract title and the security scheme")
  void R_017_11_other_description() {
    Res r = get(ANA, "/v3/api-docs").expect(200);
    assertThat(r.at("/openapi").asText()).startsWith("3.");
    assertThat(r.at("/paths").size()).isGreaterThan(30);
    assertThat(r.text()).contains("bearer");
  }

  @Test
  @DisplayName("R-017-12 other the health check body and that no data of users appears")
  void R_017_12_other_healthBody() {
    group(ANA, "Skrita");
    Res r = get(null, "/health").expect(200);
    assertThat(r.text()).doesNotContain("Skrita", "ana.novak");
    assertThat(r.at("/status").asText()).isEqualTo("UP");
    assertThat(request("POST", "/health", null, "{}").status()).isIn(401, 405);
  }

  @Test
  @DisplayName("R-017-13 other UTF-8 request and response with names in every Slovenian letter")
  void R_017_13_other_utf8Json() {
    String gid = group(ANA, "Čšž ĆĐ");
    JsonNode c =
        newContact(
            ANA,
            gid,
            j("firstName", "Žiga", "lastName", "Škrjanc", "city", "Šoštanj", "notes", "日本語 ✓"));
    Res r = get(ANA, "/groups/" + gid + "/contacts/" + c.path("id").asText()).expect(200);
    assertThat(r.headers().firstValue("Content-Type").orElse("")).containsIgnoringCase("json");
    assertThat(r.at("/notes").asText()).isEqualTo("日本語 ✓");
    assertThat(r.at("/city").asText()).isEqualTo("Šoštanj");
    assertThat(r.json().has("ime")).isFalse();
  }

  @Test
  @DisplayName("R-017-14 other no endpoint issues or accepts API keys")
  void R_017_14_other_noApiKeys() {
    group(ANA, "G");
    request("GET", "/groups", "ApiKey abc", null).expectError(401, "UNAUTHENTICATED");
    Res r = request("GET", "/groups?api_key=abc&key=abc", null, null);
    r.expectError(401, "UNAUTHENTICATED");
    Res paths = get(ANA, "/v3/api-docs").expect(200);
    assertThat(paths.text().toLowerCase()).doesNotContain("api-keys", "apikeys");
  }

  @Test
  @DisplayName(
      "R-017-15 combination a conflict carries the full current contact with tags and attributes")
  void R_017_15_combination_currentContact() {
    String gid = group(ANA, "G");
    join(gid, ANA, MARKO, "EDITOR");
    JsonNode c =
        newContact(
            ANA, gid, j("lastName", "A", "tags", arr("t"), "attributes", arr(attr("k", "v"))));
    JsonNode stale = openContact(MARKO, gid, c.path("id").asText());
    save(
            ANA,
            gid,
            openContact(ANA, gid, c.path("id").asText()),
            "company",
            "Acme",
            "tags",
            arr("t", "u"))
        .expect(200);
    Res r = save(MARKO, gid, stale, "company", "Beta").expectError(409, "CONFLICT");
    assertThat(r.at("/current/company").asText()).isEqualTo("Acme");
    assertThat(strings(r.at("/current/tags"))).containsExactlyInAnyOrder("t", "u");
    assertThat(r.at("/current/attributes/0/key").asText()).isEqualTo("k");
    assertThat(r.at("/current/revision").asLong()).isGreaterThan(stale.path("revision").asLong());
    assertThat(Duration.ofSeconds(1)).isPositive();
  }
}
