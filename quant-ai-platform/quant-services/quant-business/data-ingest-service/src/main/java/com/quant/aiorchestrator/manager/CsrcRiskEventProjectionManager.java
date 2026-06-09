package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.dataingest.SourceRawPayload;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.manager.CsrcRiskDetailFetchManager.DetailContent;
import com.quant.aiorchestrator.manager.CsrcRiskLinkProjectionManager.RiskLink;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CsrcRiskEventProjectionManager {

    private final CsrcRiskDetailFetchManager detailFetchManager;
    private final CsrcRiskTargetMatchManager targetMatchManager;
    private final CsrcRiskLinkProjectionManager linkProjectionManager;
    private final CsrcRiskEventItemAssembler itemAssembler;

    public List<MarketEventCreateDTO> parseResponse(SourceRawPayload rawPayload,
                                                    String endpointUrl,
                                                    HttpClient client,
                                                    int timeoutSeconds,
                                                    EventSourceConfigItemVO sourceConfig,
                                                    MarketEventSourceSyncDTO request) {
        return parseResponse(rawPayload == null ? null : rawPayload.getBody(), endpointUrl, client, timeoutSeconds, sourceConfig, request);
    }

    private List<MarketEventCreateDTO> parseResponse(String responseBody,
                                                     String endpointUrl,
                                                     HttpClient client,
                                                     int timeoutSeconds,
                                                     EventSourceConfigItemVO sourceConfig,
                                                     MarketEventSourceSyncDTO request) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BizException("CSRC_RISK_RESPONSE_EMPTY", "CSRC risk response body is empty");
        }

        org.jsoup.nodes.Document document = Jsoup.parse(responseBody, endpointUrl);
        List<RiskLink> links = linkProjectionManager.resolveRiskLinks(document);
        if (links.isEmpty()) {
            throw new BizException("CSRC_RISK_RESPONSE_ITEMS_EMPTY", "CSRC risk page contains no risk links");
        }

        int itemCount = request == null || request.getItemCount() == null || request.getItemCount() <= 0
                ? 10 : request.getItemCount();
        int maxScanCount = Math.max(itemCount * 8, 24);
        List<String> targetTokens = targetMatchManager.buildTargetTokens(request);
        List<MarketEventCreateDTO> result = new ArrayList<>();

        for (int index = 0; index < links.size() && index < maxScanCount; index++) {
            RiskLink link = links.get(index);
            if (result.size() >= itemCount) {
                break;
            }

            DetailContent detail = detailFetchManager.fetchDetail(link.href(), client, timeoutSeconds, sourceConfig, request);
            String combinedText = itemAssembler.combineText(link.title(), detail.title(), detail.content());
            if (!targetTokens.isEmpty() && !targetMatchManager.containsAnyToken(combinedText, targetTokens)) {
                continue;
            }
            result.add(itemAssembler.toMarketEvent(link, detail, sourceConfig, request, true));
        }

        if (result.isEmpty()) {
            return buildLatestRiskFallbackEvents(links, client, timeoutSeconds, sourceConfig, request, itemCount, maxScanCount);
        }
        return result;
    }

    private List<MarketEventCreateDTO> buildLatestRiskFallbackEvents(List<RiskLink> links,
                                                                     HttpClient client,
                                                                     int timeoutSeconds,
                                                                     EventSourceConfigItemVO sourceConfig,
                                                                     MarketEventSourceSyncDTO request,
                                                                     int itemCount,
                                                                     int maxScanCount) {
        List<MarketEventCreateDTO> result = new ArrayList<>();
        for (int index = 0; index < links.size() && index < maxScanCount; index++) {
            RiskLink link = links.get(index);
            DetailContent detail = detailFetchManager.fetchDetail(link.href(), client, timeoutSeconds, sourceConfig, request);
            result.add(itemAssembler.toMarketEvent(link, detail, sourceConfig, request, false));
            if (result.size() >= itemCount) {
                break;
            }
        }
        if (result.isEmpty()) {
            throw new BizException("CSRC_RISK_RESPONSE_ITEMS_EMPTY", "CSRC risk page returned no importable risk items");
        }
        return result;
    }
}
