package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CninfoPublicAnnouncementRequestManager {

    private final CninfoPublicAnnouncementPayloadManager payloadManager;

    public CninfoPublicAnnouncementRequestManager(CninfoPublicAnnouncementPayloadManager payloadManager) {
        this.payloadManager = payloadManager;
    }

    public CninfoPublicAnnouncementRequestManager(ObjectMapper objectMapper,
                                                  EventSourceRequestTemplateManager requestTemplateManager) {
        this(new CninfoPublicAnnouncementPayloadManager(objectMapper, requestTemplateManager));
    }

    public List<CninfoPublicAnnouncementRequest> buildRequests(EventSourceConfigItemVO sourceConfig,
                                                               MarketEventSourceSyncDTO request) {
        List<CninfoPublicAnnouncementRequest> requests = new ArrayList<>();
        for (String searchKeyword : payloadManager.resolveSearchKeywords(request)) {
            requests.add(new CninfoPublicAnnouncementRequest(
                    searchKeyword,
                    payloadManager.resolveRequestHeaders(sourceConfig, request, searchKeyword),
                    payloadManager.buildRequestBody(sourceConfig, request, searchKeyword)
            ));
        }
        return requests;
    }

    public List<EventSourceRequestDiagnosticItemVO> buildDiagnostics(String endpointUrl,
                                                                     int timeoutSeconds,
                                                                     EventSourceConfigItemVO sourceConfig,
                                                                     MarketEventSourceSyncDTO request) {
        return payloadManager.buildDiagnostics(endpointUrl, timeoutSeconds, sourceConfig, request);
    }

    public record CninfoPublicAnnouncementRequest(String searchKeyword,
                                                  Map<String, String> headers,
                                                  String body) {
    }
}
