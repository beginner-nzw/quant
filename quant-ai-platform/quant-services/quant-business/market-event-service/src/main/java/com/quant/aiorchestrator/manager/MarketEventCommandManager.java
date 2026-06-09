package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryAppender;
import com.quant.aiorchestrator.service.MarketEventStandardizedPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketEventCommandManager {

    private final MarketEventAutoTriggerService marketEventAutoTriggerService;
    private final MarketEventIngestHistoryAppender marketEventIngestHistoryAppender;
    private final MarketEventStandardizedPublisherService marketEventStandardizedPublisherService;
    private final MarketEventNormalizationManager marketEventNormalizationManager;
    private final MarketEventCreateManager marketEventCreateManager;

    public MarketEventCreateResultVO createMarketEvent(MarketEventCreateDTO dto, boolean recordHistory) {
        MarketEventCreateManager.CreateOutcome outcome = marketEventCreateManager.createMarketEvent(dto);
        if (outcome.duplicate()) {
            if (recordHistory) {
                appendManualCreateHistory(outcome.prepared(), outcome.result());
            }
            return outcome.result();
        }

        MarketEventDO event = outcome.event();
        marketEventAutoTriggerService.prepareAutoTrigger(event);
        marketEventStandardizedPublisherService.publish(event);
        MarketEventCreateResultVO result = marketEventCreateManager.buildCreateResult(event, false, outcome.result().getMessage());
        if (recordHistory) {
            appendManualCreateHistory(outcome.prepared(), result);
        }
        return result;
    }

    private void appendManualCreateHistory(MarketEventCreateDTO prepared, MarketEventCreateResultVO result) {
        String sourceDetail = String.format(
                "%s / %s",
                marketEventNormalizationManager.defaultIfBlank(prepared == null ? null : prepared.getTargetCode(), "-"),
                marketEventNormalizationManager.defaultIfBlank(prepared == null ? null : prepared.getTargetName(), "-")
        );
        boolean duplicate = result != null && Boolean.TRUE.equals(result.getDuplicate());
        marketEventIngestHistoryAppender.appendHistory(
                "MANUAL_CREATE",
                "manual create",
                "MANUAL_CREATE",
                "manual create",
                "MANUAL",
                prepared == null ? null : prepared.getSourceChannel(),
                sourceDetail,
                1,
                1,
                0,
                duplicate ? 1 : 0,
                result != null && marketEventAutoTriggerService.shouldCountAsQueued(result.getAutoTriggerStatus()) ? 1 : 0,
                duplicate ? "manual create duplicated" : "manual create success"
        );
    }
}
