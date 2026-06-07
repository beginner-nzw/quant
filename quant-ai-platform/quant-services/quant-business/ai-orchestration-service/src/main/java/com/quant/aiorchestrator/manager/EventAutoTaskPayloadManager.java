package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EventAutoTaskPayloadManager {

    public Map<String, Object> buildPayload(MarketEventDO event, EventAutoTriggerConfigService.EventAutoTriggerRule rule) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskType", defaultValue(rule.getTaskType(), "FOLLOW_UP_RESEARCH"));
        payload.put("taskTitle", resolveTaskTitle(event, rule));
        payload.put("targetType", defaultValue(event.getTargetType(), "STOCK"));
        payload.put("targetCode", event.getTargetCode());
        payload.put("targetName", event.getTargetName());
        payload.put("priority", defaultValue(rule.getPriority(), resolvePriorityByImpact(event.getImpactLevel())));
        payload.put("sourceChannel", defaultValue(rule.getSourceChannel(), "EVENT_AUTO"));
        payload.put("sourceEventId", event.getEventId());
        payload.put("sourceDomain", "MARKET_EVENT");
        payload.put("analysisScope", defaultValue(rule.getAnalysisScope(), "INTELLIGENCE_FOLLOW_UP"));
        return payload;
    }

    private String resolveTaskTitle(MarketEventDO event, EventAutoTriggerConfigService.EventAutoTriggerRule rule) {
        String template = defaultValue(rule.getTitleTemplate(), "{targetName}浜嬩欢璺熻釜鐮旂┒");
        return template
                .replace("{targetName}", defaultValue(event.getTargetName(), "鏍囩殑"))
                .replace("{targetCode}", defaultValue(event.getTargetCode(), ""))
                .replace("{eventTitle}", defaultValue(event.getEventTitle(), "甯傚満浜嬩欢"))
                .trim();
    }

    private String resolvePriorityByImpact(String impactLevel) {
        if ("HIGH".equalsIgnoreCase(impactLevel)) {
            return "HIGH";
        }
        if ("LOW".equalsIgnoreCase(impactLevel)) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
