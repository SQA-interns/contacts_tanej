package app.contactgroups.config;

import app.contactgroups.api.ErrorResponse;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless bearer-token security (AR-05, SR-01), CORS for the one allowed origin (SR-05) and the
 * standard security headers (SR-06). Only {@code /health} is public (R-017-01, R-017-12).
 */
@Configuration
public class SecurityConfig {

  @Bean
  JwtDecoder jwtDecoder(AppProperties properties) {
    return FirebaseJwtDecoders.create(properties);
  }

  @Bean
  SecurityFilterChain filterChain(
      HttpSecurity http, AppProperties properties, ObjectMapper mapper, UserService users)
      throws Exception {
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(cors(properties)))
        .headers(
            h ->
                h.contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                    .permissionsPolicyHeader(
                        p -> p.policy("camera=(), microphone=(), geolocation=()")))
        .authorizeHttpRequests(
            a -> a.requestMatchers("/health").permitAll().anyRequest().authenticated())
        .oauth2ResourceServer(
            o -> o.jwt(Customizer.withDefaults()).authenticationEntryPoint(entryPoint(mapper)))
        .exceptionHandling(e -> e.accessDeniedHandler(accessDenied(mapper)))
        .addFilterAfter(new VerifiedEmailFilter(mapper), BearerTokenAuthenticationFilter.class)
        .addFilterAfter(new UserSyncFilter(users, mapper), VerifiedEmailFilter.class);
    return http.build();
  }

  private static CorsConfigurationSource cors(AppProperties properties) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(properties.allowedOrigin()));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
    config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.RETRY_AFTER));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  private static AuthenticationEntryPoint entryPoint(ObjectMapper mapper) {
    return (request, response, e) ->
        ErrorWriter.write(
            mapper, response, ErrorResponse.of(ErrorCode.UNAUTHENTICATED, "Sign in to continue."));
  }

  private static AccessDeniedHandler accessDenied(ObjectMapper mapper) {
    return (request, response, e) ->
        ErrorWriter.write(
            mapper, response, ErrorResponse.of(ErrorCode.FORBIDDEN, "You may not do this."));
  }
}
