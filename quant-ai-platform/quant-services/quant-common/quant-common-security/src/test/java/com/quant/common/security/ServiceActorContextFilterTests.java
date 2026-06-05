package com.quant.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceActorContextFilterTests {

    private static final String SECRET = "internal-service-secret";
    private static final long NOW = 1778131148942L;

    @Test
    void validSignedServiceActorHeadersPopulateContextAndThenClearIt() throws Exception {
        ServiceActor actor = new ServiceActor(
                "ai-orchestration-service",
                "market-event-auto-dispatcher",
                "EVENT_AUTO_DISPATCHER",
                "system",
                "SYSTEM"
        );
        MockHttpServletRequest request = signedRequest(actor, NOW, ServiceActorSigner.sign(actor, NOW, SECRET));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServiceActor> observed = new AtomicReference<>();

        filter().doFilter(request, response, chain(() -> observed.set(ServiceActorContext.get())));

        assertEquals(actor, observed.get());
        assertNull(ServiceActorContext.get());
    }

    @Test
    void invalidSignatureDoesNotPopulateServiceActorContext() throws Exception {
        ServiceActor actor = new ServiceActor(
                "ai-orchestration-service",
                "market-event-auto-dispatcher",
                "EVENT_AUTO_DISPATCHER",
                "system",
                "SYSTEM"
        );
        MockHttpServletRequest request = signedRequest(actor, NOW, "bad-signature");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServiceActor> observed = new AtomicReference<>();

        filter().doFilter(request, response, chain(() -> observed.set(ServiceActorContext.get())));

        assertNull(observed.get());
        assertNull(ServiceActorContext.get());
    }

    private ServiceActorContextFilter filter() {
        return new ServiceActorContextFilter(
                SECRET,
                60L,
                Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
        );
    }

    private MockHttpServletRequest signedRequest(ServiceActor actor, long timestamp, String signature) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.HEADER_SERVICE_PRINCIPAL, actor.servicePrincipal());
        request.addHeader(SecurityConstants.HEADER_SERVICE_ACTOR_ID, actor.actorId());
        request.addHeader(SecurityConstants.HEADER_SERVICE_ACTOR_ROLE, actor.actorRole());
        request.addHeader(SecurityConstants.HEADER_SERVICE_ORIGINAL_ACTOR_ID, actor.originalActorId());
        request.addHeader(SecurityConstants.HEADER_SERVICE_ORIGINAL_ACTOR_ROLE, actor.originalActorRole());
        request.addHeader(SecurityConstants.HEADER_SERVICE_TIMESTAMP, String.valueOf(timestamp));
        request.addHeader(SecurityConstants.HEADER_SERVICE_SIGNATURE, signature);
        return request;
    }

    private FilterChain chain(ThrowingRunnable assertion) {
        return (request, response) -> assertion.run();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws ServletException, java.io.IOException;
    }
}
