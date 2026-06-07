package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityConstants;
import com.quant.common.security.ServiceActor;
import com.quant.common.security.ServiceActorSigner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EventAutoTaskServiceActorManager {

    private static final String SERVICE_PRINCIPAL = "ai-orchestration-service";
    private static final String AUTO_TRIGGER_ACTOR = "market-event-auto-dispatcher";
    private static final String AUTO_TRIGGER_ROLE = "EVENT_AUTO_DISPATCHER";

    public String[] buildServiceActorHeaders(MarketEventDO event, String serviceActorSecret) {
        if (!StringUtils.hasText(serviceActorSecret)) {
            throw new BizException(
                    "EVENT_AUTO_TRIGGER_SERVICE_IDENTITY_NOT_CONFIGURED",
                    "鑷姩瑙﹀彂浠诲姟缂哄皯鏈嶅姟闂磋韩浠藉瘑閽ラ厤缃?"
            );
        }
        long timestamp = System.currentTimeMillis();
        ServiceActor actor = new ServiceActor(
                SERVICE_PRINCIPAL,
                AUTO_TRIGGER_ACTOR,
                AUTO_TRIGGER_ROLE,
                defaultValue(event.getCreatedBy(), "system"),
                "SYSTEM"
        );
        return new String[]{
                SecurityConstants.HEADER_SERVICE_PRINCIPAL, actor.servicePrincipal(),
                SecurityConstants.HEADER_SERVICE_ACTOR_ID, actor.actorId(),
                SecurityConstants.HEADER_SERVICE_ACTOR_ROLE, actor.actorRole(),
                SecurityConstants.HEADER_SERVICE_ORIGINAL_ACTOR_ID, actor.originalActorId(),
                SecurityConstants.HEADER_SERVICE_ORIGINAL_ACTOR_ROLE, actor.originalActorRole(),
                SecurityConstants.HEADER_SERVICE_TIMESTAMP, String.valueOf(timestamp),
                SecurityConstants.HEADER_SERVICE_SIGNATURE, ServiceActorSigner.sign(actor, timestamp, serviceActorSecret)
        };
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
