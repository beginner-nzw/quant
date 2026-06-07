package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementItemVO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CninfoProxyAnnouncementPreviewManager {

    private static final String SOURCE_CODE = "CNINFO_ANNOUNCEMENT_PROXY";

    private final EventSourceConfigService eventSourceConfigService;
    private final CninfoProxyAnnouncementManager cninfoProxyAnnouncementManager;

    public CninfoProxyAnnouncementResponseVO previewAnnouncements(MarketEventSourceSyncDTO dto) {
        validateRequest(dto);

        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(SOURCE_CODE);
        if (!StringUtils.hasText(sourceConfig == null ? null : sourceConfig.getUpstreamUrl())) {
            throw new BizException("CNINFO_PROXY_UPSTREAM_URL_EMPTY", "cninfo proxy upstream url is not configured");
        }

        List<CninfoProxyAnnouncementItemVO> items = cninfoProxyAnnouncementManager.loadUpstreamAnnouncements(sourceConfig, dto);
        if (items.isEmpty()) {
            throw new BizException("CNINFO_PROXY_UPSTREAM_ITEMS_EMPTY", "cninfo proxy upstream returned no announcements");
        }

        CninfoProxyAnnouncementResponseVO response = new CninfoProxyAnnouncementResponseVO();
        response.setSourceCode(SOURCE_CODE);
        response.setSourceName(defaultValue(sourceConfig == null ? null : sourceConfig.getSourceName(), "cninfo announcement proxy"));
        response.setTargetCode(dto.getTargetCode().trim());
        response.setTargetName(dto.getTargetName().trim());
        response.setItemCount(items.size());
        response.setItems(items);
        return response;
    }

    private void validateRequest(MarketEventSourceSyncDTO dto) {
        if (dto == null) {
            throw new BizException("CNINFO_PROXY_REQUEST_EMPTY", "cninfo proxy request cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "target code cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "target name cannot be empty");
        }
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
