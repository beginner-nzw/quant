package com.quant.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;

public class ServiceActorContextFilter extends OncePerRequestFilter {

    private final String secret;
    private final long clockSkewSeconds;
    private final Clock clock;

    public ServiceActorContextFilter(String secret, long clockSkewSeconds) {
        this(secret, clockSkewSeconds, Clock.systemUTC());
    }

    ServiceActorContextFilter(String secret, long clockSkewSeconds, Clock clock) {
        this.secret = secret;
        this.clockSkewSeconds = Math.max(1L, clockSkewSeconds);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            ServiceActor actor = readVerifiedActor(request);
            if (actor != null) {
                ServiceActorContext.set(actor);
            }
            filterChain.doFilter(request, response);
        } finally {
            ServiceActorContext.clear();
        }
    }

    private ServiceActor readVerifiedActor(HttpServletRequest request) {
        String servicePrincipal = text(request.getHeader(SecurityConstants.HEADER_SERVICE_PRINCIPAL));
        String actorId = text(request.getHeader(SecurityConstants.HEADER_SERVICE_ACTOR_ID));
        String actorRole = text(request.getHeader(SecurityConstants.HEADER_SERVICE_ACTOR_ROLE));
        String timestampHeader = text(request.getHeader(SecurityConstants.HEADER_SERVICE_TIMESTAMP));
        String signature = text(request.getHeader(SecurityConstants.HEADER_SERVICE_SIGNATURE));
        if (servicePrincipal == null || actorId == null || actorRole == null || timestampHeader == null || signature == null) {
            return null;
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException ignored) {
            return null;
        }
        long now = clock.millis();
        if (Math.abs(now - timestamp) > clockSkewSeconds * 1000L) {
            return null;
        }

        ServiceActor actor = new ServiceActor(
                servicePrincipal,
                actorId,
                actorRole,
                text(request.getHeader(SecurityConstants.HEADER_SERVICE_ORIGINAL_ACTOR_ID)),
                text(request.getHeader(SecurityConstants.HEADER_SERVICE_ORIGINAL_ACTOR_ROLE))
        );
        return ServiceActorSigner.verify(actor, timestamp, signature, secret) ? actor : null;
    }

    private String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
