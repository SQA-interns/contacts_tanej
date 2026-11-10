package app.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.contactgroups.domain.ErrorCode;
import org.junit.jupiter.api.Test;

/** SR-03: the one 404 is built in one place, so every cause looks the same. */
class AccessServiceTest {

  @Test
  void notFoundIsTheFixed404WithoutDetails() {
    var first = AccessService.notFound();
    var second = AccessService.notFound();
    assertThat(first.code()).isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(first.getMessage()).isEqualTo(second.getMessage()).isEqualTo("Not found.");
    assertThat(first.fields()).isEmpty();
  }
}
