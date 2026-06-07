package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@Component
public class CninfoProxyUpstreamRequestManager {

    private final ObjectMapper objectMapper;
    private final CninfoProxyUpstreamPayloadManager payloadManager;

    public CninfoProxyUpstreamRequestManager(ObjectMapper objectMapper) {
        this(objectMapper, new CninfoProxyUpstreamPayloadManager(objectMapper));
    }

    public CninfoProxyUpstreamRequestManager(ObjectMapper objectMapper,
                                             CninfoProxyUpstreamPayloadManager payloadManager) {
        this.objectMapper = objectMapper;
        this.payloadManager = payloadManager;
    }

    public EventSourceRequestDiagnosticItemVO buildUpstreamRequestDiagnosticItem(EventSourceConfigItemVO sourceConfig,
                                                                                 MarketEventSourceSyncDTO dto) {
        String upstreamUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getUpstreamUrl());
        if (!StringUtils.hasText(upstreamUrl)) {
            return null;
        }

        String upstreamMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getUpstreamMethod(), "GET").toUpperCase(Locale.ROOT);
        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        String method = "POST".equalsIgnoreCase(upstreamMethod) ? "POST" : "GET";

        EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
        item.setStageCode("UPSTREAM_REQUEST");
        item.setStageName("上游请求");
        item.setRequestMethod(method);
        item.setRequestTimeoutSeconds(timeoutSeconds);
        item.setRequestUrl("POST".equals(method) ? upstreamUrl : payloadManager.appendQueryParams(upstreamUrl, sourceConfig, dto));
        item.setRequestHeadersJson(payloadManager.formatJsonSafely(payloadManager.maskSensitiveHeaders(payloadManager.parseHeaders(sourceConfig == null ? null : sourceConfig.getUpstreamHeadersJson()))));
        if ("POST".equals(method)) {
            item.setRequestBodyJson(payloadManager.formatJsonSafely(payloadManager.resolveUpstreamBody(sourceConfig, dto)));
        }
        return item;
    }

    public HttpRequest buildRequest(String upstreamUrl,
                                    String upstreamMethod,
                                    int timeoutSeconds,
                                    MarketEventSourceSyncDTO dto,
                                    EventSourceConfigItemVO sourceConfig) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, payloadManager.parseHeaders(sourceConfig == null ? null : sourceConfig.getUpstreamHeadersJson()));

        if ("POST".equalsIgnoreCase(upstreamMethod)) {
            Object bodyObject = payloadManager.resolveUpstreamBody(sourceConfig, dto);
            String body;
            try {
                body = objectMapper.writeValueAsString(bodyObject);
            } catch (Exception e) {
                throw new BizException("CNINFO_PROXY_UPSTREAM_SERIALIZE_FAILED", "巨潮上游请求序列化失败");
            }
            return builder
                    .uri(URI.create(upstreamUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        }

        return builder
                .uri(URI.create(payloadManager.appendQueryParams(upstreamUrl, sourceConfig, dto)))
                .GET()
                .build();
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

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
