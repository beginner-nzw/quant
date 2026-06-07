package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
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
import java.util.List;
import java.util.Map;

@Component
public class GovCnPolicyRequestManager {

    private final GovCnPolicyRequestPayloadManager payloadManager;

    public GovCnPolicyRequestManager(ObjectMapper objectMapper) {
        this(new GovCnPolicyRequestPayloadManager(objectMapper));
    }

    public GovCnPolicyRequestManager(GovCnPolicyRequestPayloadManager payloadManager) {
        this.payloadManager = payloadManager;
    }

    public HttpRequest buildRequest(String endpointUrl,
                                    int timeoutSeconds,
                                    EventSourceConfigItemVO sourceConfig,
                                    MarketEventSourceSyncDTO request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(payloadManager.appendQueryParams(endpointUrl, sourceConfig, request)))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, payloadManager.resolveRequestHeaders(sourceConfig, request));
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
                ? "Gov Policy Request (SSL Verify Disabled)" : "Gov Policy Request");
        item.setRequestMethod("GET");
        item.setRequestTimeoutSeconds(timeoutSeconds);
        item.setRequestUrl(payloadManager.appendQueryParams(endpointUrl, sourceConfig, request));
        item.setRequestHeadersJson(payloadManager.formatJsonSafely(payloadManager.maskSensitiveHeaders(payloadManager.resolveRequestHeaders(sourceConfig, request))));
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
            throw new BizException("GOV_POLICY_SSL_CONTEXT_FAILED", buildRequestFailureMessage("Gov policy SSL context initialization failed", e));
        }
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
