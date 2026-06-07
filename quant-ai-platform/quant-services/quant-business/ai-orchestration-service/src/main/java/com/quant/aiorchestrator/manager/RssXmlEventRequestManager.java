package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RssXmlEventRequestManager {

    private final EventSourceRequestTemplateManager requestTemplateManager;

    public String resolveRequestMethod(EventSourceConfigItemVO sourceConfig) {
        String requestMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getRequestMethod(), "GET").toUpperCase(Locale.ROOT);
        if (!"GET".equals(requestMethod)) {
            throw new BizException("EVENT_SOURCE_REQUEST_METHOD_UNSUPPORTED", "RSS source only supports GET requests");
        }
        return requestMethod;
    }

    public int resolveTimeoutSeconds(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
    }

    public HttpRequest buildRequest(String endpointUrl,
                                    int timeoutSeconds,
                                    EventSourceConfigItemVO sourceConfig,
                                    MarketEventSourceSyncDTO request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(appendQueryParams(endpointUrl, sourceConfig, request)))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson()));
        return builder.GET().build();
    }

    public HttpClient buildHttpClient(int timeoutSeconds) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public EventSourceRequestDiagnosticItemVO buildDiagnostic(String endpointUrl,
                                                             int timeoutSeconds,
                                                             EventSourceConfigItemVO sourceConfig,
                                                             MarketEventSourceSyncDTO request) {
        EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
        item.setStageCode("PRIMARY_REQUEST");
        item.setStageName("RSS Request");
        item.setRequestMethod("GET");
        item.setRequestTimeoutSeconds(timeoutSeconds);
        item.setRequestUrl(appendQueryParams(endpointUrl, sourceConfig, request));
        item.setRequestHeadersJson(requestTemplateManager.formatMaskedHeadersJson(parseHeaders(sourceConfig == null ? null : sourceConfig.getRequestHeadersJson())));
        return item;
    }

    public String buildHttpFailureMessage(HttpResponse<String> response) {
        if (response == null) {
            return "RSS source sync failed";
        }
        StringBuilder message = new StringBuilder("RSS source sync failed, HTTP status: ")
                .append(response.statusCode());
        String location = response.headers()
                .firstValue("Location")
                .orElse(null);
        if (StringUtils.hasText(location)) {
            message.append(", Location: ").append(location);
        }
        return message.toString();
    }

    private String appendQueryParams(String endpointUrl,
                                     EventSourceConfigItemVO sourceConfig,
                                     MarketEventSourceSyncDTO request) {
        return requestTemplateManager.appendQueryParams(
                endpointUrl,
                sourceConfig,
                request,
                "EVENT_SOURCE_REQUEST_QUERY_INVALID",
                "RSS query config parsing failed"
        );
    }

    private Map<String, String> parseHeaders(String rawHeadersJson) {
        return requestTemplateManager.parseHeaders(
                rawHeadersJson,
                "EVENT_SOURCE_REQUEST_HEADERS_INVALID",
                "RSS request headers parsing failed",
                "RSS request headers must be a JSON object"
        );
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
}
