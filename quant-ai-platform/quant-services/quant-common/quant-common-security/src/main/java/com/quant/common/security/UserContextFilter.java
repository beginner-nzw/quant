package com.quant.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UserContextFilter extends OncePerRequestFilter {

    private final AuthProperties authProperties;
    private final JwtTokenValidator jwtTokenValidator;
    private final UserProfileSource userProfileSource;

    public UserContextFilter() {
        this(defaultDemoProperties());
    }

    public UserContextFilter(AuthProperties authProperties) {
        this(authProperties, new InMemoryUserProfileSource());
    }

    public UserContextFilter(AuthProperties authProperties, UserProfileSource userProfileSource) {
        this.authProperties = authProperties == null ? new AuthProperties() : authProperties;
        this.jwtTokenValidator = new JwtTokenValidator(this.authProperties);
        this.userProfileSource = userProfileSource == null ? new InMemoryUserProfileSource() : userProfileSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        AuthenticatedUser authenticatedUser;
        try {
            authenticatedUser = authenticate(request);
        } catch (JwtValidationException ex) {
            writeUnauthorized(response, ex);
            return;
        }

        UserProfile profile = userProfileSource.findByUserId(authenticatedUser.userId())
                .orElseGet(() -> UserProfile.unknown(authenticatedUser.userId()));
        UserContext.set(authenticatedUser.userId(), authenticatedUser.userRole(), profile);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private AuthenticatedUser authenticate(HttpServletRequest request) {
        AuthMode mode = authProperties.authMode();
        if (mode == AuthMode.DEMO) {
            return demoHeaderUser(request);
        }

        String authorization = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        if (mode == AuthMode.JWT_WITH_DEMO_FALLBACK && (authorization == null || authorization.isBlank())) {
            return demoHeaderUser(request);
        }

        return jwtTokenValidator.validate(authorization);
    }

    private AuthenticatedUser demoHeaderUser(HttpServletRequest request) {
        String userId = request.getHeader(SecurityConstants.HEADER_USER_ID);
        String userRole = request.getHeader(SecurityConstants.HEADER_USER_ROLE);

        if (userId == null || userId.isBlank()) {
            userId = SecurityConstants.DEFAULT_USER_ID;
        }
        if (userRole == null || userRole.isBlank()) {
            userRole = SecurityConstants.DEFAULT_USER_ROLE;
        }
        return new AuthenticatedUser(userId, userRole);
    }

    private void writeUnauthorized(HttpServletResponse response, JwtValidationException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"" + escapeJson(ex.getMessage())
                + "\",\"authError\":\"" + escapeJson(ex.getErrorCode()) + "\"}";
        response.getWriter().write(body);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static AuthProperties defaultDemoProperties() {
        AuthProperties properties = new AuthProperties();
        properties.setMode(AuthMode.DEMO.name());
        return properties;
    }
}
