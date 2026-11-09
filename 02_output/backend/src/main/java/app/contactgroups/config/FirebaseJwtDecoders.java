package app.contactgroups.config;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Builds the decoder for Firebase ID tokens (SR-01): signature against Google's published keys,
 * issuer, audience and expiry. When the Authentication Emulator host is set, the emulator's
 * unsigned tokens are accepted with the same issuer, audience and expiry checks (test and local
 * only; the environment table of {@code environments.md} keeps the host empty in production).
 */
final class FirebaseJwtDecoders {

  static final String JWK_SET_URI =
      "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

  private FirebaseJwtDecoders() {}

  static JwtDecoder create(AppProperties properties) {
    OAuth2TokenValidator<Jwt> validator = validator(properties.firebaseProjectId());
    if (properties.emulatorEnabled()) {
      return token -> {
        Jwt jwt = parseUnsigned(token);
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        if (result.hasErrors()) {
          throw new JwtException("Invalid token");
        }
        return jwt;
      };
    }
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
    decoder.setJwtValidator(validator);
    return decoder;
  }

  static OAuth2TokenValidator<Jwt> validator(String projectId) {
    OAuth2TokenValidator<Jwt> issuer =
        jwt ->
            ("https://securetoken.google.com/" + projectId).equals(jwt.getIssuer().toString())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "iss", null));
    OAuth2TokenValidator<Jwt> audience =
        jwt ->
            jwt.getAudience() != null && jwt.getAudience().contains(projectId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "aud", null));
    return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience);
  }

  private static Jwt parseUnsigned(String token) {
    try {
      JWT parsed = JWTParser.parse(token);
      Map<String, Object> headers = new LinkedHashMap<>(parsed.getHeader().toJSONObject());
      Map<String, Object> claims = new LinkedHashMap<>(parsed.getJWTClaimsSet().getClaims());
      Instant issuedAt = parsed.getJWTClaimsSet().getIssueTime().toInstant();
      Instant expires = parsed.getJWTClaimsSet().getExpirationTime().toInstant();
      return new Jwt(token, issuedAt, expires, headers, claims);
    } catch (ParseException | RuntimeException e) {
      throw new JwtException("Invalid token", e);
    }
  }
}
