package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CsrcRiskRequestManager {

    private final EventSourceRequestTemplateManager requestTemplateManager;

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
        applyHeaders(builder, resolveRequestHeaders(sourceConfig, request));
        return builder.GET().build();
    }

    public HttpClient buildHttpClient(EventSourceConfigItemVO sourceConfig, int timeoutSeconds) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1);
        if (sourceConfig != null && Boolean.FALSE.equals(sourceConfig.getSslVerify())) {
            builder.sslContext(buildTrustAllSslContext());
        }
        return builder.build();
    }

    public List<EventSourceRequestDiagnosticItemVO> buildDiagnostics(String endpointUrl,
                                                                     int timeoutSeconds,
                                                                     EventSourceConfigItemVO sourceConfig,
                                                                     MarketEventSourceSyncDTO request) {
        EventSourceRequestDiagnosticItemVO item = new EventSourceRequestDiagnosticItemVO();
        item.setStageCode("PRIMARY_REQUEST");
        item.setStageName(sourceConfig != null && Boolean.FALSE.equals(sourceConfig.getSslVerify())
                ? "CSRC Risk Request (SSL Verify Disabled)" : "CSRC Risk Request");
        item.setRequestMethod("GET");
        item.setRequestTimeoutSeconds(timeoutSeconds);
        item.setRequestUrl(appendQueryParams(endpointUrl, sourceConfig, request));
        item.setRequestHeadersJson(requestTemplateManager.formatMaskedHeadersJson(resolveRequestHeaders(sourceConfig, request)));
        return List.of(item);
    }

    public String buildRequestFailureMessage(String prefix, Exception e) {
        if (e == null) {
            return prefix;
        }
        StringBuilder message = new StringBuilder(prefix)
                .append(": ")
                .append(e.getClass().getSimpleName());
        if (StringUtils.hasText(e.getMessage())) {
            message.append(" - ").append(e.getMessage());
        }
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            message.append("; cause=")
                    .append(cause.getClass().getSimpleName());
            if (StringUtils.hasText(cause.getMessage())) {
                message.append(" - ").append(cause.getMessage());
            }
        }
        return message.toString();
    }

    private SSLContext buildTrustAllSslContext() {
        try {
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new BizException("CSRC_RISK_SSL_CONTEXT_FAILED", buildRequestFailureMessage("CSRC risk SSL context initialization failed", e));
        }
    }

    private Map<String, String> resolveRequestHeaders(EventSourceConfigItemVO sourceConfig,
                                                      MarketEventSourceSyncDTO request) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Referer", "https://www.csrc.gov.cn/");
        headers.putAll(requestTemplateManager.parseHeaders(
                sourceConfig == null ? null : sourceConfig.getRequestHeadersJson(),
                "CSRC_RISK_REQUEST_HEADERS_INVALID",
                "CSRC risk request headers parsing failed",
                "CSRC risk request headers must be a JSON object"
        ));
        return headers;
    }

    private String appendQueryParams(String endpointUrl,
                                     EventSourceConfigItemVO sourceConfig,
                                     MarketEventSourceSyncDTO request) {
        return requestTemplateManager.appendQueryParams(
                endpointUrl,
                sourceConfig,
                request,
                "CSRC_RISK_REQUEST_QUERY_INVALID",
                "CSRC risk request query parsing failed"
        );
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (builder == null || headers == null) {
            return;
        }
        headers.forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                builder.header(key.trim(), value.trim());
            }
        });
    }
}
