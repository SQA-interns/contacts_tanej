package app.contactgroups.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Logs refusals (401, 403, 404, 429) with method, path template, status and user id (SR-11). Never
 * the query, a body, a header or a token, so no secret and no personal data reaches the log.
 */
class RequestLogFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(RequestLogFilter.class);
  private static final Set<Integer> LOGGED = Set.of(401, 403, 404, 429);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      chain.doFilter(request, response);
    } finally {
      if (LOGGED.contains(response.getStatus())) {
        LOG.info(
            "Request refused method={} path={} status={} user={}",
            request.getMethod(),
            template(request),
            response.getStatus(),
            user());
      }
    }
  }

  private static String template(HttpServletRequest request) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    return pattern == null ? "(no match)" : pattern.toString();
  }

  private static String user() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth == null || !auth.isAuthenticated() ? "-" : auth.getName();
  }
}
