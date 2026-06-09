package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CninfoProxyEventRequestManager {

    private final ObjectMapper objectMapper;
    private final EventSourceRequestTemplateManager requestTemplateManager;

    public String resolveRequestMethod(EventSourceConfigItemVO sourceConfig) {
        return defaultValue(sourceConfig == null ? null : sourceConfig.getRequestMethod(), "GET").toUpperCase(Locale.ROOT);
    }

    public int resolveTimeoutSeconds(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
    }

    public HttpRequest buildRequest(String endpointUrl,
                                    String requestMethod,
                                    int timeoutSeconds,
                                    MarketEventSourceSyncDTO request,
                                    EventSourceConfigItemVO sourceConfig) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson()));

        if ("POST".equalsIgnoreCase(requestMethod)) {
            Object bodyObject = resolveRequestBody(sourceConfig, request);
            String body;
            try {
                body = objectMapper.writeValueAsString(bodyObject);
            } catch (Exception e) {
                throw new BizException("CNINFO_PROXY_REQUEST_SERIALIZE_FAILED", "Cninfo proxy request serialization failed");
            }
            return builder
                    .uri(URI.create(endpointUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        }
        return builder
                .uri(URI.create(appendQueryParams(endpointUrl, sourceConfig, request)))
                .GET()
                .build();
    }

    public EventSourceRequestDiagnosticItemVO buildProxyRequestDiagnostic(String endpointUrl,
                                                                         String requestMethod,
                                                                         int timeoutSeconds,
                                                                         EventSourceConfigItemVO sourceConfig,
                                                                         MarketEventSourceSyncDTO request) {
        String method = "POST".equalsIgnoreCase(requestMethod) ? "POST" : "GET";
        EventSourceRequestDiagnosticItemVO proxyRequest = new EventSourceRequestDiagnosticItemVO();
        proxyRequest.setStageCode("PROXY_REQUEST");
        proxyRequest.setStageName("Proxy Request");
        proxyRequest.setRequestMethod(method);
        proxyRequest.setRequestTimeoutSeconds(timeoutSeconds);
        proxyRequest.setRequestUrl("POST".equals(method) ? endpointUrl : appendQueryParams(endpointUrl, sourceConfig, request));
        proxyRequest.setRequestHeadersJson(requestTemplateManager.formatMaskedHeadersJson(parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson())));
        if ("POST".equals(method)) {
            proxyRequest.setRequestBodyJson(formatJsonSafely(resolveRequestBody(sourceConfig, request)));
        }
        return proxyRequest;
    }

    private Object resolveRequestBody(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        Map<String, Object> defaults = buildDefaultPayload(request, sourceConfig);
        Object rendered = requestTemplateManager.renderRequestTemplate(
                sourceConfig == null ? null : sourceConfig.getRequestBodyJson(),
                request,
                sourceConfig,
                defaults,
                "CNINFO_PROXY_REQUEST_BODY_INVALID",
                "Cninfo proxy request body parsing failed"
        );
        if (rendered == null) {
            return defaults;
        }
        if (rendered instanceof Map<?, ?> renderedMap) {
            Map<String, Object> merged = new LinkedHashMap<>(defaults);
            renderedMap.forEach((key, value) -> merged.put(String.valueOf(key), value));
            return merged;
        }
        return rendered;
    }

    private String appendQueryParams(String endpointUrl,
                                     EventSourceConfigItemVO sourceConfig,
                                     MarketEventSourceSyncDTO request) {
        return requestTemplateManager.appendQueryParams(
                endpointUrl,
                sourceConfig,
                request,
                buildDefaultPayload(request, sourceConfig),
                "CNINFO_PROXY_REQUEST_QUERY_INVALID",
                "Cninfo proxy query config parsing failed"
        );
    }

    private Map<String, Object> buildDefaultPayload(MarketEventSourceSyncDTO request, EventSourceConfigItemVO sourceConfig) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceCode", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceCode(), "CNINFO_ANNOUNCEMENT_PROXY"));
        payload.put("targetType", defaultValue(request == null ? null : request.getTargetType(), "STOCK"));
        payload.put("targetCode", defaultValue(request == null ? null : request.getTargetCode(), ""));
        payload.put("targetName", defaultValue(request == null ? null : request.getTargetName(), ""));
        payload.put("itemCount", request == null || request.getItemCount() == null ? 10 : request.getItemCount());
        payload.put("defaultEventType", defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "ANNOUNCEMENT"));
        payload.put("defaultImpactLevel", defaultValue(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "HIGH"));
        payload.put("sourceChannel", defaultValue(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "EXCHANGE_FEED"));
        return payload;
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (builder == null || headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                builder.header(key.trim(), value.trim());
            }
        });
    }

    private Map<String, String> parseHeaders(String rawHeadersJson) {
        return requestTemplateManager.parseHeaders(
                rawHeadersJson,
                "CNINFO_PROXY_REQUEST_HEADERS_INVALID",
                "Cninfo proxy request headers parsing failed",
                "Cninfo proxy request headers must be a JSON object"
        );
    }

    private String formatJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
