package com.quant.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class JwtTokenValidator {

    private final AuthProperties properties;
    private final Clock clock;

    public JwtTokenValidator(AuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtTokenValidator(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public AuthenticatedUser validate(String authorizationHeader) {
        String token = bearerToken(authorizationHeader);
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw invalid("JWT must contain header, payload and signature");
        }

        Map<String, Object> header = parseJsonPart(parts[0], "JWT header is not valid JSON");
        Map<String, Object> payload = parseJsonPart(parts[1], "JWT payload is not valid JSON");
        String algorithm = stringClaim(header, "alg");
        if (!"HS256".equals(algorithm)) {
            throw invalid("Only HS256 JWT validation is supported by the current boundary");
        }
        verifySignature(parts[0], parts[1], parts[2]);
        verifyRegisteredClaims(payload);

        String userId = stringClaim(payload, properties.getUserIdClaim());
        String userRole = stringClaim(payload, properties.getRoleClaim());
        if (userId == null || userId.isBlank()) {
            throw invalid("JWT user id claim is missing");
        }
        if (userRole == null || userRole.isBlank()) {
            throw invalid("JWT role claim is missing");
        }
        return new AuthenticatedUser(userId.trim(), userRole.trim());
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new JwtValidationException("missing_token", "Authorization bearer token is required");
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw invalid("Authorization header must use Bearer scheme");
        }
        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isBlank()) {
            throw new JwtValidationException("missing_token", "Authorization bearer token is required");
        }
        return token;
    }

    private Map<String, Object> parseJsonPart(String encoded, String message) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            Object parsed = JsonObjectParser.parse(new String(bytes, StandardCharsets.UTF_8));
            if (parsed instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMap = (Map<String, Object>) map;
                return typedMap;
            }
            throw invalid(message);
        } catch (IllegalArgumentException ex) {
            throw invalid(message);
        }
    }

    private void verifySignature(String encodedHeader, String encodedPayload, String encodedSignature) {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new JwtValidationException("jwt_not_configured", "JWT validation secret is not configured");
        }
        String signedContent = encodedHeader + "." + encodedPayload;
        byte[] expected = HmacSha256.sign(signedContent, secret);
        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(encodedSignature);
        } catch (IllegalArgumentException ex) {
            throw invalid("JWT signature is not base64url encoded");
        }
        if (!java.security.MessageDigest.isEqual(expected, actual)) {
            throw invalid("JWT signature is invalid");
        }
    }

    private void verifyRegisteredClaims(Map<String, Object> payload) {
        long now = Instant.now(clock).getEpochSecond();
        long skew = Math.max(0L, properties.getClockSkewSeconds());
        Long expiresAt = longClaim(payload, "exp");
        if (expiresAt == null) {
            throw invalid("JWT exp claim is required");
        }
        if (expiresAt + skew < now) {
            throw new JwtValidationException("expired_token", "JWT token is expired");
        }
        Long notBefore = longClaim(payload, "nbf");
        if (notBefore != null && notBefore - skew > now) {
            throw invalid("JWT token is not active yet");
        }
        Long issuedAt = longClaim(payload, "iat");
        if (issuedAt != null && issuedAt - skew > now) {
            throw invalid("JWT token issue time is in the future");
        }
        String issuer = trimToNull(properties.getIssuer());
        if (issuer != null && !issuer.equals(stringClaim(payload, "iss"))) {
            throw invalid("JWT issuer is invalid");
        }
        String audience = trimToNull(properties.getAudience());
        if (audience != null && !audienceMatches(payload.get("aud"), audience)) {
            throw invalid("JWT audience is invalid");
        }
    }

    private boolean audienceMatches(Object value, String expectedAudience) {
        if (value instanceof String stringValue) {
            return expectedAudience.equals(stringValue);
        }
        if (value instanceof List<?> listValue) {
            return listValue.stream().anyMatch(item -> expectedAudience.equals(String.valueOf(item)));
        }
        return false;
    }

    private String stringClaim(Map<String, Object> claims, String claimName) {
        if (claimName == null || claimName.isBlank()) {
            return null;
        }
        Object value = claims.get(claimName);
        return value == null ? null : String.valueOf(value);
    }

    private Long longClaim(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private JwtValidationException invalid(String message) {
        return new JwtValidationException("invalid_token", message);
    }
}
