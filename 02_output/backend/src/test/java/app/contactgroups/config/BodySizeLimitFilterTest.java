package app.contactgroups.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** R-017-09, EX-017-05: a body over 1 MB is refused with 413; exactly 1 MB is processed. */
class BodySizeLimitFilterTest {

  private static final int MB = 1024 * 1024;
  private final BodySizeLimitFilter filter = new BodySizeLimitFilter(new ObjectMapper());

  private static MockHttpServletRequest post(byte[] body, boolean declareLength) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/groups") {
          @Override
          public long getContentLengthLong() {
            return declareLength ? body.length : -1;
          }
        };
    request.setContent(body);
    return request;
  }

  @Test
  void aDeclaredLengthOverTheLimitIsRefusedBeforeAnythingRuns() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(post(new byte[MB + 1], true), response, chain);
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).contains("\"code\":\"PAYLOAD_TOO_LARGE\"");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void aDeclaredLengthOfExactlyTheLimitPasses() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(post(new byte[MB], true), response, chain);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void aBodyOfUnknownLengthIsCutWhenItGrowsOverTheLimit() throws Exception {
    AtomicReference<ServletRequest> seen = new AtomicReference<>();
    MockHttpServletRequest request = post(new byte[MB + 1], false);
    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (req, res) -> {
          seen.set(req);
        });
    ServletRequest wrapped = seen.get();
    assertThatThrownBy(() -> wrapped.getInputStream().readAllBytes())
        .isInstanceOfSatisfying(
            ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE));
  }

  @Test
  void aBodyOfUnknownLengthOfExactlyTheLimitIsRead() throws Exception {
    AtomicReference<ServletRequest> seen = new AtomicReference<>();
    filter.doFilter(
        post(new byte[MB], false),
        new MockHttpServletResponse(),
        (req, res) -> {
          seen.set(req);
        });
    assertThatCode(() -> assertThat(seen.get().getInputStream().readAllBytes()).hasSize(MB))
        .doesNotThrowAnyException();
  }
}
