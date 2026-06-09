package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;

public interface CninfoProxyAnnouncementService {

    CninfoProxyAnnouncementResponseVO previewAnnouncements(MarketEventSourceSyncDTO dto);

    EventSourceRequestDiagnosticItemVO buildUpstreamRequestDiagnosticItem(EventSourceConfigItemVO sourceConfig,
                                                                          MarketEventSourceSyncDTO dto);
}
