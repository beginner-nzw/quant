package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.dataingest.SourceProvenance;
import com.quant.aiorchestrator.dataingest.SourceRawPayload;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.aiorchestrator.manager.RssXmlEventProjectionManager;
import com.quant.aiorchestrator.manager.RssXmlEventRequestManager;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RssXmlEventSourceSyncAdapter implements EventSourceSyncAdapter {

    private static final String INGEST_MODE = "RSS_XML";
    private final RssXmlEventRequestManager requestManager;
    private final RssXmlEventProjectionManager projectionManager;

    @Override
    public boolean supports(EventSourceConfigItemVO sourceConfig) {
        return sourceConfig != null
                && StringUtils.hasText(sourceConfig.getIngestMode())
                && INGEST_MODE.equalsIgnoreCase(sourceConfig.getIngestMode().trim());
    }

    @Override
    public SourceRawPayload fetchRaw(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getEndpointUrl());
        if (!StringUtils.hasText(endpointUrl)) {
            throw new BizException("EVENT_SOURCE_ENDPOINT_URL_EMPTY", "RSS endpoint URL cannot be empty");
        }

        String requestMethod = requestManager.resolveRequestMethod(sourceConfig);

        int timeoutSeconds = requestManager.resolveTimeoutSeconds(sourceConfig);
        HttpRequest httpRequest = requestManager.buildRequest(endpointUrl, timeoutSeconds, sourceConfig, request);
        HttpClient client = requestManager.buildHttpClient(timeoutSeconds);
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("EVENT_SOURCE_HTTP_FAILED", requestManager.buildHttpFailureMessage(response));
            }
            return SourceRawPayload.builder()
                    .provenance(SourceProvenance.from(sourceConfig, request == null ? null : request.getTargetCode()))
                    .httpStatus(response.statusCode())
                    .requestMethod(requestMethod)
                    .requestUrl(httpRequest.uri().toString())
                    .body(response.body())
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("EVENT_SOURCE_HTTP_REQUEST_FAILED", "RSS source request failed");
        }
    }

    @Override
    public List<MarketEventCreateDTO> standardize(SourceRawPayload rawPayload,
                                                  EventSourceConfigItemVO sourceConfig,
                                                  MarketEventSourceSyncDTO request) {
        return projectionManager.parseResponse(rawPayload == null ? null : rawPayload.getBody(), sourceConfig, request);
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getEndpointUrl());
        if (!StringUtils.hasText(endpointUrl)) {
            throw new BizException("EVENT_SOURCE_ENDPOINT_URL_EMPTY", "RSS endpoint URL cannot be empty");
        }

        requestManager.resolveRequestMethod(sourceConfig);

        int timeoutSeconds = requestManager.resolveTimeoutSeconds(sourceConfig);
        return List.of(requestManager.buildDiagnostic(endpointUrl, timeoutSeconds, sourceConfig, request));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

}
