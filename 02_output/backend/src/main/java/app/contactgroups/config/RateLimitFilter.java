package app.contactgroups.config;

import app.contactgroups.api.ErrorResponse;
import app.contactgroups.domain.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Answers 429 with {@code Retry-After} when a user sends more than the allowed requests in a minute
 * (R-017-08). The key is the user id of the verified token, else the remote address.
 */
class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimiter limiter;
  private final ObjectMapper mapper;

  RateLimitFilter(RateLimiter limiter, ObjectMapper mapper) {
    this.limiter = limiter;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long wait = limiter.secondsToWait(key(request));
    if (wait > 0) {
      response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(wait));
      ErrorWriter.write(
          mapper,
          response,
          ErrorResponse.of(ErrorCode.RATE_LIMITED, "Too many requests. Try again later."));
      return;
    }
    chain.doFilter(request, response);
  }

  private static String key(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth instanceof JwtAuthenticationToken jwt
        ? "user:" + jwt.getToken().getSubject()
        : "address:" + request.getRemoteAddr();
  }
}
