package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DuplicateDetectorTest {

  private static DuplicateCandidate c(
      String first, String last, String email, String company, String phone) {
    return new DuplicateCandidate(UUID.randomUUID(), first, last, email, company, phone);
  }

  private static List<DuplicateMatch> detect(DuplicateCandidate... cs) {
    return DuplicateDetector.detect(List.of(cs));
  }

  @Test
  void sameEmailIsCertain() {
    var a = c("", "a", "x@e.com", "", "");
    var b = c("", "b", "x@e.com", "", "");
    List<DuplicateMatch> sets = detect(a, b);
    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).kind()).isEqualTo(DuplicateKind.CERTAIN);
    assertThat(sets.get(0).reasons()).containsExactly(DuplicateReason.SAME_EMAIL);
    assertThat(sets.get(0).ids()).containsExactlyInAnyOrder(a.id(), b.id());
  }

  @Test
  void nameAndCompanyIsProbable() {
    var sets = detect(c("ana", "novak", "", "acme", ""), c("ana", "novak", "", "acme", ""));
    assertThat(sets.get(0).kind()).isEqualTo(DuplicateKind.PROBABLE);
    assertThat(sets.get(0).reasons()).containsExactly(DuplicateReason.SAME_NAME_AND_COMPANY);
  }

  @Test
  void nameAndPhoneIgnoresFormatting() {
    var sets =
        detect(
            c("ana", "novak", "", "", "+386 41 555 123"),
            c("ana", "novak", "", "", "+386 (41) 555-123"));
    assertThat(sets.get(0).reasons()).containsExactly(DuplicateReason.SAME_NAME_AND_PHONE);
  }

  @Test
  void differentDigitsDoNotMatch() {
    assertThat(
            detect(
                c("ana", "novak", "", "", "+386 41 555 123"),
                c("ana", "novak", "", "", "041 555 123")))
        .isEmpty();
  }

  @Test
  void nameAloneCompanyAloneOrPhoneAloneNeverMatch() {
    assertThat(detect(c("ana", "novak", "", "a", "1"), c("ana", "novak", "", "b", "2"))).isEmpty();
    assertThat(detect(c("", "", "", "acme", ""), c("", "", "", "acme", ""))).isEmpty();
    assertThat(detect(c("", "", "", "", "+386"), c("", "", "", "", "+386"))).isEmpty();
  }

  @Test
  void needsFirstAndLastName() {
    assertThat(detect(c("", "novak", "", "acme", ""), c("", "novak", "", "acme", ""))).isEmpty();
  }

  @Test
  void chainFormsOneCertainSetWithBothReasons() {
    var a = c("ana", "novak", "x@e.com", "acme", "");
    var b = c("ana", "novak", "x@e.com", "beta", "");
    var d = c("ana", "novak", "", "beta", "");
    var sets = detect(a, b, d);
    assertThat(sets).hasSize(1);
    assertThat(sets.get(0).kind()).isEqualTo(DuplicateKind.CERTAIN);
    assertThat(sets.get(0).reasons())
        .containsExactly(DuplicateReason.SAME_EMAIL, DuplicateReason.SAME_NAME_AND_COMPANY);
    assertThat(sets.get(0).ids()).containsExactlyInAnyOrder(a.id(), b.id(), d.id());
  }

  @Test
  void setKeyIsIndependentOfOrder() {
    UUID x = UUID.randomUUID();
    UUID y = UUID.randomUUID();
    assertThat(DuplicateMatch.keyOf(List.of(x, y))).isEqualTo(DuplicateMatch.keyOf(List.of(y, x)));
    assertThat(DuplicateMatch.keyOf(List.of(x, y))).hasSize(64);
  }
}
