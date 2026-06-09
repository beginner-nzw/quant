package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.dataingest.SourceProvenance;
import com.quant.aiorchestrator.dataingest.SourceRawPayload;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.aiorchestrator.manager.CninfoPublicAnnouncementProjectionManager;
import com.quant.aiorchestrator.manager.CninfoPublicAnnouncementRequestManager;
import com.quant.aiorchestrator.manager.CninfoPublicAnnouncementRequestManager.CninfoPublicAnnouncementRequest;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CninfoPublicAnnouncementSyncAdapter implements EventSourceSyncAdapter {

    private static final String INGEST_MODE = "CNINFO_PUBLIC_CRAWLER";
    private static final String DEFAULT_ENDPOINT_URL = "https://www.cninfo.com.cn/new/hisAnnouncement/query";

    private final CninfoPublicAnnouncementRequestManager requestManager;
    private final CninfoPublicAnnouncementProjectionManager projectionManager;

    @Override
    public boolean supports(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig != null
                && StringUtils.hasText(sourceConfig.getIngestMode())
                && INGEST_MODE.equalsIgnoreCase(sourceConfig.getIngestMode().trim());
    }

    @Override
    public SourceRawPayload fetchRaw(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = StringUtils.hasText(sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        int timeoutSeconds = sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .build();

        try {
            for (CninfoPublicAnnouncementRequest announcementRequest : requestManager.buildRequests(sourceConfig, request)) {
                HttpRequest httpRequest = buildRequest(endpointUrl, timeoutSeconds, announcementRequest.headers(), announcementRequest.body());
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new BizException("CNINFO_PUBLIC_HTTP_FAILED", "巨潮公开公告检索失败，HTTP 状态码: " + response.statusCode());
                }
                List<MarketEventCreateDTO> events = projectionManager.parseResponse(response.body(), request, sourceConfig, endpointUrl);
                if (!events.isEmpty()) {
                    return SourceRawPayload.builder()
                            .provenance(SourceProvenance.from(sourceConfig, request == null ? null : request.getTargetCode()))
                            .httpStatus(response.statusCode())
                            .requestMethod("POST")
                            .requestUrl(endpointUrl)
                            .body(response.body())
                            .build();
                }
            }
            throw new BizException("CNINFO_PUBLIC_ITEMS_EMPTY", "巨潮公开公告未返回公告");
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("CNINFO_PUBLIC_REQUEST_FAILED", "巨潮公开公告请求失败");
        }
    }

    @Override
    public List<MarketEventCreateDTO> standardize(SourceRawPayload rawPayload,
                                                  EventSourceConfigItemVO sourceConfig,
                                                  MarketEventSourceSyncDTO request) {
        String endpointUrl = StringUtils.hasText(sourceConfig == null ? null : sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        return projectionManager.parseResponse(rawPayload == null ? null : rawPayload.getBody(), request, sourceConfig, endpointUrl);
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = StringUtils.hasText(sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        int timeoutSeconds = sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();
        return requestManager.buildDiagnostics(endpointUrl, timeoutSeconds, sourceConfig, request);
    }

    private HttpRequest buildRequest(String endpointUrl,
                                     int timeoutSeconds,
                                     Map<String, String> headers,
                                     String requestBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, headers);
        return builder.POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)).build();
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
