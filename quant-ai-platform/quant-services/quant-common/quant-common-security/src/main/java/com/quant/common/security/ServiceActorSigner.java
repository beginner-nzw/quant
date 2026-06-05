package com.quant.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ServiceActorSigner {

    public static String sign(ServiceActor actor, long timestamp, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("service actor secret is not configured");
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(HmacSha256.sign(canonical(actor, timestamp), secret));
    }

    public static boolean verify(ServiceActor actor, long timestamp, String signature, String secret) {
        if (signature == null || signature.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        String expected = sign(actor, timestamp, secret);
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String canonical(ServiceActor actor, long timestamp) {
        return value(actor.servicePrincipal()) + "\n"
                + value(actor.actorId()) + "\n"
                + value(actor.actorRole()) + "\n"
                + value(actor.originalActorId()) + "\n"
                + value(actor.originalActorRole()) + "\n"
                + timestamp;
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private ServiceActorSigner() {
    }
}
