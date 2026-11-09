package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static app.contactgroups.acceptance.Who.BORIS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * US-017 rate limit: AC-017-08 and EX-017-04. The limit is the default of 100 requests per user and
 * minute (R-017-08); the requests are sent in parallel so that they fall inside one minute.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.rate-limit-per-minute=100", "app.trash-cleanup-cron=0 30 3 * * *"})
class Us017RateLimitAcceptanceIT extends AcceptanceBase {

  private List<Res> burst(Who who, int n) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(10);
    try {
      List<Callable<Res>> calls = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        calls.add(() -> get(who, "/groups"));
      }
      List<Res> out = new ArrayList<>();
      for (Future<Res> f : pool.invokeAll(calls)) {
        out.add(f.get());
      }
      return out;
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void AC_017_08_the101stRequestWithinAMinuteGets429WithRetryAfter() throws Exception {
    List<Res> answers = new ArrayList<>(List.of(get(ANA, "/groups")));
    answers.addAll(burst(ANA, 100));
    long refused = answers.stream().filter(r -> r.status() == 429).count();
    assertThat(refused).as("requests answered with 429").isEqualTo(1);
    Res limited = answers.stream().filter(r -> r.status() == 429).findFirst().orElseThrow();
    assertThat(limited.code()).isEqualTo("RATE_LIMITED");
    assertThat(limited.headers().firstValue("Retry-After")).isPresent();
    assertThat(Integer.parseInt(limited.headers().firstValue("Retry-After").orElseThrow()))
        .isBetween(1, 60);
    assertThat(answers.stream().filter(r -> r.status() == 200).count()).isEqualTo(100);
  }

  @Test
  void EX_017_04_exactly100RequestsInAMinuteAreAllAnsweredNormally() throws Exception {
    List<Res> answers = new ArrayList<>(List.of(get(BORIS, "/groups")));
    answers.addAll(burst(BORIS, 99));
    assertThat(answers).hasSize(100).allMatch(r -> r.status() == 200);
  }
}
