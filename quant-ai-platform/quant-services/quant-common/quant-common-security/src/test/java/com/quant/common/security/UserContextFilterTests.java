package com.quant.common.security;

import com.quant.common.core.exception.BizException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserContextFilterTests {

    private static final String SECRET = "phase-one-demo-secret";

    @Test
    void jwtModeMapsValidatedTokenToUserContextAndIgnoresFrontendIdentityHeaders() throws Exception {
        UserContextFilter filter = new UserContextFilter(jwtProperties(AuthMode.JWT));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, "Bearer "
                + token("{\"sub\":\"jwt-user\",\"role\":\"ADMIN\",\"exp\":" + futureExp() + "}"));
        request.addHeader(SecurityConstants.HEADER_USER_ID, "header-user");
        request.addHeader(SecurityConstants.HEADER_USER_ROLE, "USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, assertingChain(() -> {
            invoked.set(true);
            assertEquals("jwt-user", SecurityUtils.currentUserId());
            assertEquals("ADMIN", SecurityUtils.currentUserRole());
            assertEquals(UserProfileStatus.UNKNOWN, SecurityUtils.currentUserStatus());
            assertEquals(List.of(), SecurityUtils.currentUserRoles());
            assertFalse(SecurityUtils.isAdmin());
            assertThrows(BizException.class, () -> new RoleChecker().requireAny(UserRoleEnum.ADMIN));
        }));

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
        assertContextCleared();
    }

    @Test
    void jwtModeResolvesRolesFromBackendProfileSourceByUserId() throws Exception {
        UserContextFilter filter = new UserContextFilter(jwtProperties(AuthMode.JWT));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, "Bearer "
                + token("{\"sub\":\"admin\",\"role\":\"USER\",\"exp\":" + futureExp() + "}"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, assertingChain(() -> {
            assertEquals("admin", SecurityUtils.currentUserId());
            assertEquals("USER", SecurityUtils.currentUserRole());
            assertEquals("Platform Admin", SecurityUtils.currentDisplayName());
            assertEquals(UserProfileStatus.ACTIVE, SecurityUtils.currentUserStatus());
            assertEquals(List.of("ADMIN"), SecurityUtils.currentUserRoles());
            assertTrue(SecurityUtils.isAdmin());
            assertDoesNotThrow(() -> new RoleChecker().requireAny(UserRoleEnum.ADMIN));
        }));

        assertEquals(200, response.getStatus());
        assertContextCleared();
    }

    @Test
    void disabledProfileDoesNotGrantRoleAuthority() throws Exception {
        UserContextFilter filter = new UserContextFilter(jwtProperties(AuthMode.JWT));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, "Bearer "
                + token("{\"sub\":\"disabled_user\",\"role\":\"ADMIN\",\"exp\":" + futureExp() + "}"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, assertingChain(() -> {
            assertEquals("disabled_user", SecurityUtils.currentUserId());
            assertEquals("ADMIN", SecurityUtils.currentUserRole());
            assertEquals(UserProfileStatus.DISABLED, SecurityUtils.currentUserStatus());
            assertEquals(List.of("RESEARCHER"), SecurityUtils.currentUserRoles());
            assertFalse(SecurityUtils.isAdmin());
            assertThrows(BizException.class, () -> new RoleChecker().requireAny(UserRoleEnum.ADMIN));
        }));

        assertEquals(200, response.getStatus());
        assertContextCleared();
    }

    @Test
    void demoModePreservesHeaderCompatibilityAndDefaults() throws Exception {
        UserContextFilter filter = new UserContextFilter(demoProperties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_USER_ID, "demo-user");
        request.addHeader(SecurityConstants.HEADER_USER_ROLE, "COMPLIANCE_AUDITOR");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, assertingChain(() -> {
            assertEquals("demo-user", SecurityUtils.currentUserId());
            assertEquals("COMPLIANCE_AUDITOR", SecurityUtils.currentUserRole());
            assertEquals(UserProfileStatus.UNKNOWN, SecurityUtils.currentUserStatus());
            assertEquals(List.of(), SecurityUtils.currentUserRoles());
            assertFalse(SecurityUtils.isReviewer());
            assertThrows(BizException.class, () -> new RoleChecker().requireAny(UserRoleEnum.ADMIN));
        }));

        assertEquals(200, response.getStatus());
        assertContextCleared();

        MockHttpServletRequest missingHeaderRequest = new MockHttpServletRequest();
        MockHttpServletResponse missingHeaderResponse = new MockHttpServletResponse();
        filter.doFilter(missingHeaderRequest, missingHeaderResponse, assertingChain(() -> {
            assertEquals(SecurityConstants.DEFAULT_USER_ID, SecurityUtils.currentUserId());
            assertEquals(SecurityConstants.DEFAULT_USER_ROLE, SecurityUtils.currentUserRole());
            assertEquals(UserProfileStatus.UNKNOWN, SecurityUtils.currentUserStatus());
        }));
        assertEquals(200, missingHeaderResponse.getStatus());
        assertContextCleared();
    }

    @Test
    void jwtWithDemoFallbackUsesDemoHeadersOnlyWhenTokenIsMissing() throws Exception {
        UserContextFilter filter = new UserContextFilter(jwtProperties(AuthMode.JWT_WITH_DEMO_FALLBACK));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_USER_ID, "fallback-user");
        request.addHeader(SecurityConstants.HEADER_USER_ROLE, "PM");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, assertingChain(() -> {
            assertEquals("fallback-user", SecurityUtils.currentUserId());
            assertEquals("PM", SecurityUtils.currentUserRole());
            assertTrue(UserRoleEnum.USER.matches(SecurityUtils.currentUserRole()));
            assertEquals(List.of(), SecurityUtils.currentUserRoles());
        }));

        assertEquals(200, response.getStatus());
        assertContextCleared();

        MockHttpServletRequest invalidTokenRequest = new MockHttpServletRequest();
        invalidTokenRequest.addHeader(SecurityConstants.HEADER_AUTHORIZATION, "Bearer invalid-token");
        invalidTokenRequest.addHeader(SecurityConstants.HEADER_USER_ID, "fallback-user");
        invalidTokenRequest.addHeader(SecurityConstants.HEADER_USER_ROLE, "ADMIN");
        MockHttpServletResponse invalidTokenResponse = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(invalidTokenRequest, invalidTokenResponse, assertingChain(() -> invoked.set(true)));

        assertFalse(invoked.get());
        assertEquals(401, invalidTokenResponse.getStatus());
        assertTrue(invalidTokenResponse.getContentAsString().contains("invalid_token"));
        assertContextCleared();
    }

    @Test
    void jwtModeRejectsMissingInvalidExpiredAndUnconfiguredTokens() throws Exception {
        assertUnauthorized(jwtProperties(AuthMode.JWT), request -> {
        }, "missing_token");
        assertUnauthorized(jwtProperties(AuthMode.JWT), request ->
                request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, "Bearer invalid-token"), "invalid_token");
        assertUnauthorized(jwtProperties(AuthMode.JWT), request ->
                request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, "Bearer "
                        + token("{\"sub\":\"jwt-user\",\"role\":\"ADMIN\",\"exp\":" + pastExp() + "}")), "expired_token");

        AuthProperties unconfigured = new AuthProperties();
        unconfigured.setMode(AuthMode.JWT.name());
        assertUnauthorized(unconfigured, request ->
                request.addHeader(SecurityConstants.HEADER_AUTHORIZATION, "Bearer "
                        + token("{\"sub\":\"jwt-user\",\"role\":\"ADMIN\",\"exp\":" + futureExp() + "}")), "jwt_not_configured");
    }

    private void assertUnauthorized(AuthProperties properties,
                                    RequestCustomizer customizer,
                                    String expectedAuthError) throws Exception {
        UserContextFilter filter = new UserContextFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        customizer.customize(request);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, assertingChain(() -> invoked.set(true)));

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains(expectedAuthError));
        assertContextCleared();
    }

    private FilterChain assertingChain(ThrowingRunnable assertion) {
        return (request, response) -> assertion.run();
    }

    private AuthProperties jwtProperties(AuthMode mode) {
        AuthProperties properties = new AuthProperties();
        properties.setMode(mode.name());
        properties.setJwtSecret(SECRET);
        return properties;
    }

    private AuthProperties demoProperties() {
        AuthProperties properties = new AuthProperties();
        properties.setMode(AuthMode.DEMO.name());
        return properties;
    }

    private String token(String payloadJson) {
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode(payloadJson);
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(HmacSha256.sign(header + "." + payload, SECRET));
        return header + "." + payload + "." + signature;
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private long futureExp() {
        return Instant.now().plusSeconds(3600).getEpochSecond();
    }

    private long pastExp() {
        return Instant.now().minusSeconds(3600).getEpochSecond();
    }

    private void assertContextCleared() {
        assertEquals(null, UserContext.getUserId());
        assertEquals(null, UserContext.getUserRole());
        assertEquals(null, UserContext.getStatus());
        assertEquals(List.of(), UserContext.getRoles());
    }

    @FunctionalInterface
    private interface RequestCustomizer {
        void customize(MockHttpServletRequest request);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws ServletException, java.io.IOException;
    }
}
