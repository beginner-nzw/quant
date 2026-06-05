package com.quant.common.model.message;

import lombok.Data;

@Data
public class AiTaskActorProvenance {

    private String identitySource;
    private String roleSource;
    private String servicePrincipal;
    private ActorRef systemActor;
    private ActorRef originalActor;
    private ActorRef delegatedActor;

    @Data
    public static class ActorRef {
        private String actorType;
        private String actorId;
        private String actorRole;
        private String sourceService;
    }
}
