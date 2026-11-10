package app.contactgroups.config;

import app.contactgroups.api.ErrorResponse;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs for a verified user: refuses the token of a deleted account (R-016-06) and otherwise keeps
 * the user record up to date with the token (ADR-02).
 */
class UserSyncFilter extends OncePerRequestFilter {

  private final UserService users;
  private final ObjectMapper mapper;

  UserSyncFilter(UserService users, ObjectMapper mapper) {
    this.users = users;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken token) {
      Jwt jwt = token.getToken();
      if (users.isDeleted(jwt.getSubject())) {
        ErrorWriter.write(
            mapper, response, ErrorResponse.of(ErrorCode.UNAUTHENTICATED, "Sign in to continue."));
        return;
      }
      users.sync(jwt.getSubject(), jwt.getClaimAsString("name"), jwt.getClaimAsString("email"));
    }
    chain.doFilter(request, response);
  }
}
