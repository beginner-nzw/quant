package com.quant.common.model.message;

public final class AiTaskActorProvenanceSupport {

    public static final String IDENTITY_SOURCE_USER_CONTEXT = "USER_CONTEXT";
    public static final String IDENTITY_SOURCE_SYSTEM = "SYSTEM";
    public static final String IDENTITY_SOURCE_SERVICE = "SERVICE_PRINCIPAL";
    public static final String IDENTITY_SOURCE_KAFKA_PAYLOAD = "KAFKA_PAYLOAD";
    public static final String ROLE_SOURCE_USER_CONTEXT = "USER_CONTEXT";
    public static final String ROLE_SOURCE_SYSTEM_POLICY = "SYSTEM_POLICY";
    public static final String ACTOR_TYPE_USER = "USER";
    public static final String ACTOR_TYPE_SERVICE = "SERVICE";
    public static final String ACTOR_TYPE_SYSTEM = "SYSTEM";

    public static AiTaskActorProvenance userInitiated(String sourceService,
                                                      String userId,
                                                      String userRole) {
        AiTaskActorProvenance provenance = new AiTaskActorProvenance();
        provenance.setIdentitySource(IDENTITY_SOURCE_USER_CONTEXT);
        provenance.setRoleSource(ROLE_SOURCE_USER_CONTEXT);
        provenance.setServicePrincipal(sourceService);
        provenance.setSystemActor(actor(ACTOR_TYPE_SERVICE, sourceService, "SERVICE", sourceService));
        provenance.setOriginalActor(actor(ACTOR_TYPE_USER, defaultText(userId, "unknown"), userRole, sourceService));
        provenance.setDelegatedActor(provenance.getSystemActor());
        return provenance;
    }

    public static AiTaskActorProvenance systemInitiated(String sourceService,
                                                        String systemActorId,
                                                        String systemRole,
                                                        String originalActorId,
                                                        String originalActorRole) {
        AiTaskActorProvenance provenance = new AiTaskActorProvenance();
        provenance.setIdentitySource(IDENTITY_SOURCE_SYSTEM);
        provenance.setRoleSource(ROLE_SOURCE_SYSTEM_POLICY);
        provenance.setServicePrincipal(sourceService);
        provenance.setSystemActor(actor(ACTOR_TYPE_SERVICE, sourceService, "SERVICE", sourceService));
        provenance.setOriginalActor(actor(ACTOR_TYPE_SYSTEM, defaultText(originalActorId, systemActorId), originalActorRole, sourceService));
        provenance.setDelegatedActor(actor(ACTOR_TYPE_SYSTEM, defaultText(systemActorId, sourceService), systemRole, sourceService));
        return provenance;
    }

    public static AiTaskActorProvenance serviceCallback(String sourceService,
                                                        AiTaskActorProvenance inbound) {
        AiTaskActorProvenance provenance = new AiTaskActorProvenance();
        provenance.setIdentitySource(IDENTITY_SOURCE_KAFKA_PAYLOAD);
        provenance.setRoleSource(inbound == null ? ROLE_SOURCE_SYSTEM_POLICY : inbound.getRoleSource());
        provenance.setServicePrincipal(sourceService);
        provenance.setSystemActor(actor(ACTOR_TYPE_SERVICE, sourceService, "SERVICE", sourceService));
        provenance.setOriginalActor(inbound == null ? null : inbound.getOriginalActor());
        provenance.setDelegatedActor(actor(ACTOR_TYPE_SERVICE, sourceService, "SERVICE", sourceService));
        return provenance;
    }

    public static String identitySource(AiTaskActorProvenance provenance) {
        return provenance == null ? null : provenance.getIdentitySource();
    }

    public static String roleSource(AiTaskActorProvenance provenance) {
        return provenance == null ? null : provenance.getRoleSource();
    }

    public static String servicePrincipal(AiTaskActorProvenance provenance) {
        return provenance == null ? null : provenance.getServicePrincipal();
    }

    public static String originalActorId(AiTaskActorProvenance provenance) {
        return provenance == null || provenance.getOriginalActor() == null ? null : provenance.getOriginalActor().getActorId();
    }

    public static String delegatedActorId(AiTaskActorProvenance provenance) {
        return provenance == null || provenance.getDelegatedActor() == null ? null : provenance.getDelegatedActor().getActorId();
    }

    private static AiTaskActorProvenance.ActorRef actor(String actorType,
                                                       String actorId,
                                                       String actorRole,
                                                       String sourceService) {
        AiTaskActorProvenance.ActorRef actor = new AiTaskActorProvenance.ActorRef();
        actor.setActorType(actorType);
        actor.setActorId(actorId);
        actor.setActorRole(actorRole);
        actor.setSourceService(sourceService);
        return actor;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private AiTaskActorProvenanceSupport() {
    }
}
