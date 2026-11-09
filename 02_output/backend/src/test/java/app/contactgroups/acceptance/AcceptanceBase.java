package app.contactgroups.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Harness of the acceptance tests (phase 3): the whole backend runs in the test process on a
 * throwaway PostgreSQL (Testcontainers) and is reached only over HTTP, as the web app reaches it.
 * People sign in with the unsigned tokens of the Firebase Authentication Emulator, which the
 * backend accepts when the emulator host is set (environments.md, id-token-claims.schema.json). The
 * clock is the injectable {@link Clock} of ADR-08; the trash cleanup runs every second. Between
 * tests every table is emptied, so each test starts on an empty system (NFR-13).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.rate-limit-per-minute=100000", "app.trash-cleanup-cron=* * * * * *"})
@Import(AcceptanceBase.TestClockConfiguration.class)
public abstract class AcceptanceBase {

  public static final String PROJECT = "demo-contacts";
  protected static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17.6");

  static {
    POSTGRES.start();
  }

  /** A clock that the tests move without waiting. */
  public static final class MutableClock extends Clock {
    private volatile Duration offset = Duration.ZERO;

    public void advance(Duration d) {
      offset = offset.plus(d);
    }

    public void reset() {
      offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return Instant.now().plus(offset);
    }
  }

  @TestConfiguration
  public static class TestClockConfiguration {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock();
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.firebase-project-id", () -> PROJECT);
    registry.add("app.firebase-auth-emulator-host", () -> "localhost:9099");
    registry.add("app.allowed-origin", () -> "http://localhost:5173");
  }

  @LocalServerPort protected int port;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected MutableClock clock;

  /** A test class that builds a large data set once may keep it between its tests. */
  protected boolean emptyBeforeEachTest() {
    return true;
  }

  @BeforeEach
  void emptySystem() {
    clock.reset();
    if (emptyBeforeEachTest()) {
      emptyAllTables(jdbc);
    }
  }

  /** Creates n contacts through the API with 8 requests in parallel; returns their ids. */
  protected List<String> bulkContacts(
      Who who, String gid, int n, java.util.function.IntFunction<ObjectNode> body) {
    java.util.concurrent.ExecutorService pool =
        java.util.concurrent.Executors.newFixedThreadPool(8);
    try {
      List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();
      for (int i = 0; i < n; i++) {
        final int k = i;
        futures.add(
            pool.submit(
                () ->
                    post(who, "/groups/" + gid + "/contacts", body.apply(k))
                        .expect(201)
                        .at("/id")
                        .asText()));
      }
      List<String> ids = new java.util.ArrayList<>();
      for (var f : futures) {
        ids.add(f.get());
      }
      return ids;
    } catch (Exception e) {
      throw new AssertionError("Creating the contacts failed: " + e, e);
    } finally {
      pool.shutdownNow();
    }
  }

  @AfterEach
  void resetClock() {
    clock.reset();
  }

  static void emptyAllTables(JdbcTemplate jdbc) {
    jdbc.execute(
        "DO $$ DECLARE r record; BEGIN FOR r IN SELECT tablename FROM pg_tables WHERE"
            + " schemaname = 'public' AND tablename <> 'flyway_schema_history' LOOP EXECUTE"
            + " 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE'; END LOOP; END $$");
  }

  /** Moves the test clock to the given instant, for example {@code 2026-07-01T22:30:00Z}. */
  protected void clockAt(String instant) {
    clock.advance(Duration.between(clock.instant(), Instant.parse(instant)));
  }

  // ---------------------------------------------------------------- tokens

  /** The unsigned ID token the emulator issues for the person, as of the test clock. */
  protected String token(Who who) {
    Instant now = clock.instant();
    return token(who, PROJECT, now.minusSeconds(5), now.plus(Duration.ofDays(400)), now);
  }

  /** A token with its own project, issue time, expiry and time of the last sign-in. */
  protected static String token(
      Who who, String project, Instant issuedAt, Instant expires, Instant authTime) {
    ObjectNode claims = JSON.createObjectNode();
    claims.put("iss", "https://securetoken.google.com/" + project);
    claims.put("aud", project);
    claims.put("sub", who.uid());
    claims.put("iat", issuedAt.getEpochSecond());
    claims.put("exp", expires.getEpochSecond());
    claims.put("auth_time", authTime.getEpochSecond());
    claims.put("email", who.email());
    claims.put("email_verified", who.verified());
    claims.put("name", who.name());
    Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
    return enc.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
        + "."
        + enc.encodeToString(claims.toString().getBytes(StandardCharsets.UTF_8))
        + ".";
  }

  // ---------------------------------------------------------------- HTTP

  protected Res get(Who who, String path) {
    return request("GET", path, who == null ? null : "Bearer " + token(who), null);
  }

  protected Res post(Who who, String path, Object body) {
    return request("POST", path, who == null ? null : "Bearer " + token(who), body);
  }

  protected Res put(Who who, String path, Object body) {
    return request("PUT", path, who == null ? null : "Bearer " + token(who), body);
  }

  protected Res patch(Who who, String path, Object body) {
    return request("PATCH", path, who == null ? null : "Bearer " + token(who), body);
  }

  protected Res delete(Who who, String path) {
    return request("DELETE", path, who == null ? null : "Bearer " + token(who), null);
  }

  /** Any method, with the Authorization header as given (null: none) and a JSON body. */
  protected Res request(String method, String path, String authorization, Object body) {
    byte[] bytes =
        body == null
            ? null
            : body instanceof byte[] b ? b : body.toString().getBytes(StandardCharsets.UTF_8);
    HttpRequest.Builder b =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(120))
            .method(
                method,
                bytes == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(bytes));
    if (bytes != null) {
      b.header("Content-Type", "application/json");
    }
    if (authorization != null) {
      b.header("Authorization", authorization);
    }
    try {
      HttpResponse<byte[]> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
      return new Res(r.statusCode(), r.body(), r.headers());
    } catch (Exception e) {
      throw new AssertionError("The request " + method + " " + path + " failed: " + e, e);
    }
  }

  // ---------------------------------------------------------------- JSON

  /** An object from key and value pairs: {@code j("firstName", "Ana", "tags", arr("a"))}. */
  protected static ObjectNode j(Object... pairs) {
    ObjectNode n = JSON.createObjectNode();
    for (int i = 0; i < pairs.length; i += 2) {
      n.set((String) pairs[i], JSON.valueToTree(pairs[i + 1]));
    }
    return n;
  }

  protected static ArrayNode arr(Object... values) {
    ArrayNode a = JSON.createArrayNode();
    for (Object v : values) {
      a.add(JSON.valueToTree(v));
    }
    return a;
  }

  /** An attribute as the contract writes it. */
  protected static ObjectNode attr(String key, String value) {
    return j("key", key, "value", value);
  }

  protected static String repeat(String s, int n) {
    return s.repeat(n);
  }

  /** The elements of an array of texts, in order. */
  protected static List<String> strings(JsonNode array) {
    List<String> out = new java.util.ArrayList<>();
    for (JsonNode n : array) {
      out.add(n.asText());
    }
    return out;
  }

  protected static List<String> values(JsonNode array, String field) {
    return array.findValuesAsText(field);
  }

  // ---------------------------------------------------------------- setting the scene

  protected String group(Who owner, String name) {
    Res r = post(owner, "/groups", j("name", name)).expect(201);
    return r.json().path("id").asText();
  }

  /** The Owner invites the person with the role; the person's first request accepts it. */
  protected void join(String gid, Who owner, Who who, String role) {
    post(owner, "/groups/" + gid + "/invitations", j("email", who.email(), "role", role))
        .expect(201);
    get(who, "/me").expect(200);
    Res groups = get(who, "/groups").expect(200);
    boolean listed = false;
    for (JsonNode g : groups.json()) {
      listed |= gid.equals(g.path("id").asText()) && role.equals(g.path("role").asText());
    }
    assertThat(listed).as("%s is a %s of the group after signing in", who.email(), role).isTrue();
  }

  protected JsonNode newContact(Who who, String gid, ObjectNode body) {
    return post(who, "/groups/" + gid + "/contacts", body).expect(201).json();
  }

  protected JsonNode newContact(Who who, String gid, String first, String last) {
    return newContact(who, gid, j("firstName", first, "lastName", last));
  }

  protected JsonNode openContact(Who who, String gid, String contactId) {
    return get(who, "/groups/" + gid + "/contacts/" + contactId).expect(200).json();
  }

  /** The same contact with fields changed, ready to be saved with its revision. */
  protected ObjectNode edit(JsonNode contact, Object... pairs) {
    ObjectNode n = ((ObjectNode) contact.deepCopy());
    for (String drop :
        List.of("id", "state", "trashedAt", "daysLeft", "createdAt", "versionNumber")) {
      n.remove(drop);
    }
    for (int i = 0; i < pairs.length; i += 2) {
      n.set((String) pairs[i], JSON.valueToTree(pairs[i + 1]));
    }
    return n;
  }

  protected Res save(Who who, String gid, JsonNode contact, Object... pairs) {
    return put(
        who, "/groups/" + gid + "/contacts/" + contact.path("id").asText(), edit(contact, pairs));
  }

  protected JsonNode search(Who who, String gid, ObjectNode request) {
    return post(who, "/groups/" + gid + "/contacts/search", request).expect(200).json();
  }

  protected JsonNode search(Who who, String gid, String q) {
    return search(who, gid, j("q", q));
  }

  /** The e-mails or last names etc. of the rows of a page, in order. */
  protected static List<String> rowValues(JsonNode page, String field) {
    return page.path("items").findValuesAsText(field);
  }

  protected void trash(Who who, String gid, String contactId) {
    post(who, "/groups/" + gid + "/contacts/" + contactId + "/trash", null).expect(204);
  }

  protected Res history(Who who, String gid) {
    return get(who, "/groups/" + gid + "/history");
  }

  protected Res contactHistory(Who who, String gid, String contactId) {
    return get(who, "/groups/" + gid + "/contacts/" + contactId + "/history");
  }

  /** Entries of the group-wide History with the event type. */
  protected List<JsonNode> entries(Who who, String gid, String eventType) {
    JsonNode page = history(who, gid).expect(200).json();
    List<JsonNode> out = new java.util.ArrayList<>();
    for (JsonNode e : page.path("items")) {
      if (eventType.equals(e.path("eventType").asText())) {
        out.add(e);
      }
    }
    return out;
  }

  /** Waits until a condition holds (the scheduled cleanup runs every second). */
  protected void eventually(java.util.function.BooleanSupplier condition, String what) {
    long end = System.currentTimeMillis() + 20_000;
    while (System.currentTimeMillis() < end) {
      if (condition.getAsBoolean()) {
        return;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    assertThat(condition.getAsBoolean()).as(what).isTrue();
  }
}
