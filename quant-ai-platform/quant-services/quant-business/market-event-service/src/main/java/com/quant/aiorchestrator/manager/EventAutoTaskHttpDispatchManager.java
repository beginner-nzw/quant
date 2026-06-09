package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityConstants;
import com.quant.common.web.RequestHeaderConstants;
import com.quant.common.web.TraceContext;
import com.quant.config.port.EventAutoTriggerConfigPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventAutoTaskHttpDispatchManager {

    private final ObjectMapper objectMapper;
    private final EventAutoTaskPayloadManager payloadManager;
    private final EventAutoTaskServiceActorManager serviceActorManager;

    public String createFollowUpTask(MarketEventDO event,
                                     EventAutoTriggerConfigPort.EventAutoTriggerRule rule,
                                     String researchTaskServiceBaseUrl,
                                     String serviceActorSecret) {
        Map<String, Object> payload = payloadManager.buildPayload(event, rule);
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BizException("EVENT_AUTO_TRIGGER_SERIALIZE_FAILED", "鑷姩瑙﹀彂浠诲姟璇锋眰搴忓垪鍖栧け璐?");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(researchTaskServiceBaseUrl) + "/api/research/tasks"))
                .header("Content-Type", "application/json")
                .header(SecurityConstants.HEADER_USER_ID, "system")
                .header(SecurityConstants.HEADER_USER_ROLE, "ADMIN")
                .header(RequestHeaderConstants.HEADER_TRACE_ID, TraceContext.resolveTraceId(TraceContext.currentTraceId()))
                .headers(serviceActorManager.buildServiceActorHeaders(event, serviceActorSecret))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("EVENT_AUTO_TRIGGER_HTTP_FAILED", "鑷姩瑙﹀彂浠诲姟璋冪敤澶辫触锛孒TTP鐘舵€佺爜: " + response.statusCode());
            }

            JsonNode body = objectMapper.readTree(response.body());
            boolean success = body.path("success").asBoolean(false);
            if (!success) {
                throw new BizException(
                        "EVENT_AUTO_TRIGGER_CREATE_FAILED",
                        "鑷姩瑙﹀彂浠诲姟澶辫触: " + body.path("message").asText("unknown")
                );
            }

            String taskId = body.path("data").asText(null);
            if (!StringUtils.hasText(taskId)) {
                throw new BizException("EVENT_AUTO_TRIGGER_TASK_ID_EMPTY", "鑷姩瑙﹀彂浠诲姟鎴愬姛浣嗘湭杩斿洖浠诲姟ID");
            }
            return taskId;
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("EVENT_AUTO_TRIGGER_REQUEST_FAILED", "鑷姩瑙﹀彂浠诲姟璇锋眰澶辫触");
        }
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
}
