package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityConstants;
import com.quant.common.security.SecurityUtils;
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
public class EventAutoTaskDispatchService {

    private final ObjectMapper objectMapper;

    @Value("${quant.ai.research-task-service-base-url:http://127.0.0.1:8081}")
    private String researchTaskServiceBaseUrl;

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
                .header(SecurityConstants.HEADER_USER_ID, defaultValue(event.getCreatedBy(), defaultValue(SecurityUtils.currentUserId(), "system")))
                .header(SecurityConstants.HEADER_USER_ROLE, defaultValue(SecurityUtils.currentUserRole(), "ADMIN"))
                .header(RequestHeaderConstants.HEADER_TRACE_ID, TraceContext.resolveTraceId(TraceContext.currentTraceId()))
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
