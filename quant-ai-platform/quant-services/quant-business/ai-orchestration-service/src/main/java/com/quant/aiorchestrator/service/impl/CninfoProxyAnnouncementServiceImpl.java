package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.aiorchestrator.manager.CninfoProxyAnnouncementManager;
import com.quant.aiorchestrator.manager.CninfoProxyAnnouncementPreviewManager;
import com.quant.aiorchestrator.service.CninfoProxyAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CninfoProxyAnnouncementServiceImpl implements CninfoProxyAnnouncementService {

    private final CninfoProxyAnnouncementPreviewManager previewManager;
    private final CninfoProxyAnnouncementManager cninfoProxyAnnouncementManager;

    @Override
    public CninfoProxyAnnouncementResponseVO previewAnnouncements(MarketEventSourceSyncDTO dto) {
        return previewManager.previewAnnouncements(dto);
    }

    @Override
    public EventSourceRequestDiagnosticItemVO buildUpstreamRequestDiagnosticItem(EventSourceConfigItemVO sourceConfig,
                                                                                 MarketEventSourceSyncDTO dto) {
        return cninfoProxyAnnouncementManager.buildUpstreamRequestDiagnosticItem(sourceConfig, dto);
    }
}
