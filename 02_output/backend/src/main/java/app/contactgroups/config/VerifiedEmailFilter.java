package app.contactgroups.config;

import app.contactgroups.api.ErrorResponse;
import app.contactgroups.domain.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A valid token whose e-mail is not verified gets 403 {@code EMAIL_NOT_VERIFIED} and nothing else
 * runs: no user record, no group, no invitation (BR-03, R-017-03).
 */
class VerifiedEmailFilter extends OncePerRequestFilter {

  private final ObjectMapper mapper;

  VerifiedEmailFilter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt
        && !Boolean.TRUE.equals(jwt.getToken().getClaim("email_verified"))) {
      ErrorWriter.write(
          mapper,
          response,
          ErrorResponse.of(ErrorCode.EMAIL_NOT_VERIFIED, "Verify your e-mail address first."));
      return;
    }
    chain.doFilter(request, response);
  }
}
