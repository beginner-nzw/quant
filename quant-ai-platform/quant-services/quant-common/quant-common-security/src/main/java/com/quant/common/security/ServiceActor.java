package com.quant.common.security;

public record ServiceActor(
        String servicePrincipal,
        String actorId,
        String actorRole,
        String originalActorId,
        String originalActorRole
) {
}
