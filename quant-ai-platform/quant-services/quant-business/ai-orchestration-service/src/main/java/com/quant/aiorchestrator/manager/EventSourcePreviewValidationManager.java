package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class EventSourcePreviewValidationManager {

    private final EventSourceConfigService eventSourceConfigService;

    public EventSourceConfigItemVO resolveValidatedSourceConfig(String sourceCode, MarketEventSourceSyncDTO dto) {
        if (!StringUtils.hasText(sourceCode)) {
            throw new BizException("MARKET_EVENT_SOURCE_CODE_EMPTY", "事件源编码不能为空");
        }
        if (dto == null) {
            throw new BizException("MARKET_EVENT_SOURCE_PREVIEW_EMPTY", "事件源预览请求不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "标的代码不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "标的名称不能为空");
        }

        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(sourceCode);
        if (sourceConfig == null) {
            throw new BizException("MARKET_EVENT_SOURCE_NOT_FOUND", "事件源配置不存在");
        }
        return sourceConfig;
    }
}
