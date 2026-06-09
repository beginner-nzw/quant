package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class MarketEventBatchImportManager {

    private final MarketEventNormalizationManager marketEventNormalizationManager;

    public MarketEventBatchImportResultVO executeBatchImport(MarketEventBatchImportDTO dto,
                                                             BatchImportSource source,
                                                             Function<MarketEventCreateDTO, MarketEventCreateResultVO> createEvent,
                                                             Function<List<MarketEventCreateDTO>, String> defaultSourceDetailResolver,
                                                             Function<Exception, String> exceptionMessageResolver,
                                                             BatchIngestHistoryAppender historyAppender) {
        List<MarketEventCreateDTO> events = dto == null || dto.getEvents() == null ? List.of() : dto.getEvents();
        if (events.isEmpty()) {
            throw new com.quant.common.core.exception.BizException("MARKET_EVENT_BATCH_IMPORT_EMPTY", "鎵归噺瀵煎叆浜嬩欢涓嶈兘涓虹┖");
        }

        List<MarketEventBatchImportItemVO> items = new java.util.ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int duplicateCount = 0;
        int autoTriggeredCount = 0;

        for (int i = 0; i < events.size(); i++) {
            MarketEventCreateDTO item = events.get(i);
            MarketEventBatchImportItemVO resultItem = new MarketEventBatchImportItemVO();
            resultItem.setItemNo(i + 1);
            resultItem.setTargetCode(item == null ? null : marketEventNormalizationManager.trimToNull(item.getTargetCode()));
            resultItem.setTargetName(item == null ? null : marketEventNormalizationManager.trimToNull(item.getTargetName()));
            resultItem.setEventTitle(item == null ? null : marketEventNormalizationManager.trimToNull(item.getEventTitle()));

            try {
                MarketEventCreateResultVO created = createEvent.apply(item);
                resultItem.setSuccess(true);
                resultItem.setDuplicate(created != null && Boolean.TRUE.equals(created.getDuplicate()));
                resultItem.setEventId(created == null ? null : created.getEventId());
                resultItem.setNormalizedFingerprint(created == null ? null : created.getNormalizedFingerprint());
                resultItem.setAutoTriggerStatus(created == null ? null : created.getAutoTriggerStatus());
                resultItem.setAutoTriggerTaskId(created == null ? null : created.getAutoTriggerTaskId());
                resultItem.setAutoTriggerReason(created == null ? null : created.getAutoTriggerReason());
                resultItem.setAutoTriggerFailureCode(created == null ? null : created.getAutoTriggerFailureCode());
                resultItem.setMessage(resolveBatchImportMessage(created));
                successCount++;
                if (created != null && Boolean.TRUE.equals(created.getDuplicate())) {
                    duplicateCount++;
                }
                if (created != null
                        && !Boolean.TRUE.equals(created.getDuplicate())
                        && shouldCountAsQueued(created.getAutoTriggerStatus())) {
                    autoTriggeredCount++;
                }
            } catch (Exception e) {
                resultItem.setSuccess(false);
                resultItem.setDuplicate(false);
                resultItem.setMessage(marketEventNormalizationManager.trimMessage(exceptionMessageResolver.apply(e), 255));
                failedCount++;
            }
            items.add(resultItem);
        }

        MarketEventBatchImportResultVO result = new MarketEventBatchImportResultVO();
        result.setTotalCount(events.size());
        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);
        result.setDuplicateCount(duplicateCount);
        result.setAutoTriggeredCount(autoTriggeredCount);
        result.setItems(items);
        historyAppender.append(
                source.sourceType(),
                source.sourceLabel(),
                source.sourceCode(),
                source.sourceName(),
                source.sourceCategory(),
                source.sourceChannel(),
                StringUtils.hasText(source.sourceDetail()) ? source.sourceDetail() : defaultSourceDetailResolver.apply(events),
                result
        );
        return result;
    }

    private boolean shouldCountAsQueued(String autoTriggerStatus) {
        return "SUCCESS".equalsIgnoreCase(autoTriggerStatus)
                || "WILL_TRIGGER".equalsIgnoreCase(autoTriggerStatus);
    }

    private String resolveBatchImportMessage(MarketEventCreateResultVO created) {
        if (created == null) {
            return "浜嬩欢瀵煎叆鎴愬姛";
        }
        if (StringUtils.hasText(created.getMessage())) {
            return created.getMessage().trim();
        }
        return "浜嬩欢瀵煎叆鎴愬姛";
    }

    public record BatchImportSource(String sourceType,
                                    String sourceLabel,
                                    String sourceCode,
                                    String sourceName,
                                    String sourceCategory,
                                    String sourceChannel,
                                    String sourceDetail) {
    }

    @FunctionalInterface
    public interface BatchIngestHistoryAppender {
        void append(String sourceType,
                    String sourceLabel,
                    String sourceCode,
                    String sourceName,
                    String sourceCategory,
                    String sourceChannel,
                    String sourceDetail,
                    MarketEventBatchImportResultVO result);
    }
}
