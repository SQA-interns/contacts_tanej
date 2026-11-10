package app.contactgroups.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContactPageTest {

  @Test
  @DisplayName("EX-009-01 EX-009-09: 25 matches are one page, 26 are two, none is page 1")
  void lastPageFollowsThePageSize() {
    assertThat(ContactPage.lastPage(0)).isEqualTo(1);
    assertThat(ContactPage.lastPage(25)).isEqualTo(1);
    assertThat(ContactPage.lastPage(26)).isEqualTo(2);
    assertThat(ContactPage.lastPage(60)).isEqualTo(3);
  }
}
