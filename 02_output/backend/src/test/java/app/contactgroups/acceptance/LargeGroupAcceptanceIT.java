package app.contactgroups.acceptance;

import static app.contactgroups.acceptance.Who.ANA;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * A group of 10,001 contacts, built once for the class: EX-009-08 and EX-010-07 (under one second,
 * NFR-01), AC-011-09 and EX-011-01 (the limit of 10,000 contacts of an export, NFR-15). The tests
 * only read.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LargeGroupAcceptanceIT extends AcceptanceBase {

  private String gid;

  @Override
  protected boolean emptyBeforeEachTest() {
    return false;
  }

  @BeforeAll
  void bigGroup() {
    emptyAllTables(jdbc);
    gid = group(ANA, "Velika skupina");
    bulkContacts(
        ANA,
        gid,
        10_001,
        i ->
            j(
                "firstName",
                "Ime",
                "lastName",
                String.format("Priimek%05d", i),
                "country",
                "SI",
                "tags",
                i == 0 ? arr("t1", "special") : arr("t1")));
  }

  private long millis(ObjectNode request) {
    long start = System.nanoTime();
    post(ANA, "/groups/" + gid + "/contacts/search", request).expect(200);
    return (System.nanoTime() - start) / 1_000_000;
  }

  @Test
  void EX_009_08_aSearchInAGroupOf10000ContactsAnswersInUnderOneSecond() {
    ObjectNode request = j("q", "priimek0123");
    millis(request);
    for (int i = 0; i < 5; i++) {
      assertThat(millis(request)).as("search time in ms").isLessThan(1000);
    }
    JsonNode r = search(ANA, gid, request);
    assertThat(r.at("/total").asInt()).isEqualTo(10);
  }

  @Test
  void EX_010_07_threeConditionsInAGroupOf10000ContactsAnswerInUnderOneSecond() {
    ObjectNode request =
        j(
            "conditions",
            arr(
                j("type", "TAGS_ALL", "tags", arr("t1")),
                j("type", "TAG_NOT", "tag", "special"),
                j("type", "COUNTRY", "country", "SI")));
    millis(request);
    for (int i = 0; i < 5; i++) {
      assertThat(millis(request)).as("filter time in ms").isLessThan(1000);
    }
    assertThat(search(ANA, gid, request).at("/total").asInt()).isEqualTo(10_000);
  }

  @Test
  void AC_011_09_anExportOf10001ContactsIsRefusedAndNoFileIsCreated() {
    Res r = post(ANA, "/groups/" + gid + "/exports", j("language", "sl", "filter", j()));
    r.expectFieldCode("TOO_MANY_CONTACTS");
    assertThat(r.headers().firstValue("Content-Disposition")).isEmpty();
  }

  @Test
  void EX_011_01_anExportOfExactly10000ContactsIsDoneAndOf10001Refused() {
    Res ok =
        post(
                ANA,
                "/groups/" + gid + "/exports",
                j(
                    "language",
                    "sl",
                    "filter",
                    j("conditions", arr(j("type", "TAG_NOT", "tag", "special")))))
            .expect(200);
    try (Workbook w = new Workbook(ok)) {
      assertThat(w.dataRows()).isEqualTo(10_000);
    }
    post(ANA, "/groups/" + gid + "/exports", j("language", "sl", "filter", j()))
        .expectFieldCode("TOO_MANY_CONTACTS");
  }
}
