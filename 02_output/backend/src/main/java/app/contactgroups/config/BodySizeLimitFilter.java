package app.contactgroups.config;

import app.contactgroups.api.ErrorResponse;
import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses a request body over 1 MB with 413 and processes nothing (R-017-09, EX-017-05). A declared
 * length is checked up front; a body of unknown length is cut when it grows over the limit.
 */
class BodySizeLimitFilter extends OncePerRequestFilter {

  static final long LIMIT_BYTES = 1024L * 1024L;

  private final ObjectMapper mapper;

  BodySizeLimitFilter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > LIMIT_BYTES) {
      ErrorWriter.write(
          mapper,
          response,
          ErrorResponse.of(ErrorCode.PAYLOAD_TOO_LARGE, "The request is larger than 1 MB."));
      return;
    }
    chain.doFilter(new LimitedRequest(request), response);
  }

  /** The request whose body stream throws once more than the limit was read. */
  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private LimitedRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new LimitedStream(super.getInputStream());
    }
  }

  private static final class LimitedStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private long read;

    private LimitedStream(ServletInputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
      int value = delegate.read();
      if (value >= 0) {
        count(1);
      }
      return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = delegate.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    private void count(int n) {
      read += n;
      if (read > LIMIT_BYTES) {
        throw new ApiException(ErrorCode.PAYLOAD_TOO_LARGE, "The request is larger than 1 MB.");
      }
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener listener) {
      delegate.setReadListener(listener);
    }
  }
}
