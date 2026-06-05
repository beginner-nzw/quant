package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.EventAutoTaskDispatchService;
import com.quant.aiorchestrator.service.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.ServiceActor;
import com.quant.common.security.ServiceActorSigner;
import com.quant.common.security.SecurityConstants;
import com.quant.common.web.RequestHeaderConstants;
import com.quant.common.web.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventAutoTaskDispatchServiceImpl implements EventAutoTaskDispatchService {

    private static final String SERVICE_PRINCIPAL = "ai-orchestration-service";
    private static final String AUTO_TRIGGER_ACTOR = "market-event-auto-dispatcher";
    private static final String AUTO_TRIGGER_ROLE = "EVENT_AUTO_DISPATCHER";

    private final ObjectMapper objectMapper;

    @Value("${quant.ai.research-task-service-base-url:http://127.0.0.1:8081}")
    private String researchTaskServiceBaseUrl;

    @Value("${quant.security.service-actor.secret:}")
    private String serviceActorSecret;

    public String createFollowUpTask(MarketEventDO event, EventAutoTriggerConfigService.EventAutoTriggerRule rule) {
        if (event == null || rule == null) {
            return null;
        }

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

        String baseUrl = trimTrailingSlash(researchTaskServiceBaseUrl);
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BizException("EVENT_AUTO_TRIGGER_SERIALIZE_FAILED", "自动触发任务请求序列化失败");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/research/tasks"))
                .header("Content-Type", "application/json")
                .header(SecurityConstants.HEADER_USER_ID, "system")
                .header(SecurityConstants.HEADER_USER_ROLE, "ADMIN")
                .header(RequestHeaderConstants.HEADER_TRACE_ID, TraceContext.resolveTraceId(TraceContext.currentTraceId()))
                .headers(buildServiceActorHeaders(event))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("EVENT_AUTO_TRIGGER_HTTP_FAILED", "自动触发任务调用失败，HTTP状态码: " + response.statusCode());
            }

            JsonNode body = objectMapper.readTree(response.body());
            boolean success = body.path("success").asBoolean(false);
            if (!success) {
                throw new BizException(
                        "EVENT_AUTO_TRIGGER_CREATE_FAILED",
                        "自动触发任务失败: " + body.path("message").asText("unknown")
                );
            }

            String taskId = body.path("data").asText(null);
            if (!StringUtils.hasText(taskId)) {
                throw new BizException("EVENT_AUTO_TRIGGER_TASK_ID_EMPTY", "自动触发任务成功但未返回任务ID");
            }
            return taskId;
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("EVENT_AUTO_TRIGGER_REQUEST_FAILED", "自动触发任务请求失败");
        }
    }

    public String[] buildServiceActorHeaders(MarketEventDO event) {
        if (!StringUtils.hasText(serviceActorSecret)) {
            throw new BizException(
                    "EVENT_AUTO_TRIGGER_SERVICE_IDENTITY_NOT_CONFIGURED",
                    "自动触发任务缺少服务间身份密钥配置"
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

    private String resolveTaskTitle(MarketEventDO event, EventAutoTriggerConfigService.EventAutoTriggerRule rule) {
        String template = defaultValue(rule.getTitleTemplate(), "{targetName}事件跟踪研究");
        return template
                .replace("{targetName}", defaultValue(event.getTargetName(), "标的"))
                .replace("{targetCode}", defaultValue(event.getTargetCode(), ""))
                .replace("{eventTitle}", defaultValue(event.getEventTitle(), "市场事件"))
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

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
