package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.dataingest.SourceProvenance;
import com.quant.aiorchestrator.dataingest.SourceRawPayload;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.aiorchestrator.manager.CsrcRiskEventProjectionManager;
import com.quant.aiorchestrator.manager.CsrcRiskRequestManager;
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
public class CsrcRiskHtmlSyncAdapter implements EventSourceSyncAdapter {

    private static final String INGEST_MODE = "CSRC_RISK_HTML";
    private static final String DEFAULT_ENDPOINT_URL = "https://www.csrc.gov.cn/csrc/zwxx/index.shtml";
    private final CsrcRiskRequestManager requestManager;
    private final CsrcRiskEventProjectionManager projectionManager;

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
        int timeoutSeconds = requestManager.resolveTimeoutSeconds(sourceConfig);

        HttpClient client = requestManager.buildHttpClient(sourceConfig, timeoutSeconds);
        HttpRequest httpRequest = requestManager.buildRequest(endpointUrl, timeoutSeconds, sourceConfig, request);
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("CSRC_RISK_HTTP_FAILED", "CSRC risk source sync failed, HTTP status: " + response.statusCode());
            }
            return SourceRawPayload.builder()
                    .provenance(SourceProvenance.from(sourceConfig, request == null ? null : request.getTargetCode()))
                    .httpStatus(response.statusCode())
                    .requestMethod("GET")
                    .requestUrl(httpRequest.uri().toString())
                    .body(response.body())
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("CSRC_RISK_REQUEST_FAILED", requestManager.buildRequestFailureMessage("CSRC risk source request failed", e));
        }
    }

    @Override
    public List<MarketEventCreateDTO> standardize(SourceRawPayload rawPayload,
                                                  EventSourceConfigItemVO sourceConfig,
                                                  MarketEventSourceSyncDTO request) {
        String endpointUrl = rawPayload != null && StringUtils.hasText(rawPayload.getRequestUrl())
                ? rawPayload.getRequestUrl()
                : StringUtils.hasText(sourceConfig == null ? null : sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        int timeoutSeconds = requestManager.resolveTimeoutSeconds(sourceConfig);
        HttpClient client = requestManager.buildHttpClient(sourceConfig, timeoutSeconds);
        return projectionManager.parseResponse(rawPayload, endpointUrl, client, timeoutSeconds, sourceConfig, request);
    }

    @Override
    public List<EventSourceRequestDiagnosticItemVO> diagnose(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO request) {
        String endpointUrl = StringUtils.hasText(sourceConfig.getEndpointUrl())
                ? sourceConfig.getEndpointUrl().trim() : DEFAULT_ENDPOINT_URL;
        int timeoutSeconds = requestManager.resolveTimeoutSeconds(sourceConfig);
        return requestManager.buildDiagnostics(endpointUrl, timeoutSeconds, sourceConfig, request);
    }

}
