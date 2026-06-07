package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class MarketEventBatchPreviewManager {

    private static final String AUTO_TRIGGER_DISABLED = "DISABLED";
    private static final String AUTO_TRIGGER_NO_MATCH = "NO_MATCH";
    private static final String AUTO_TRIGGER_WILL_TRIGGER = "WILL_TRIGGER";
    private static final String AUTO_TRIGGER_SKIPPED_DUPLICATE = "SKIPPED_DUPLICATE";
    private static final String AUTO_TRIGGER_INVALID = "INVALID";
    private static final String DUPLICATE_SOURCE_EXISTING = "EXISTING_EVENT";
    private static final String DUPLICATE_SOURCE_BATCH = "SAME_BATCH";

    private final EventAutoTriggerConfigService eventAutoTriggerConfigService;
    private final MarketEventNormalizationManager marketEventNormalizationManager;

    public MarketEventBatchPreviewResultVO previewImportMarketEvents(MarketEventBatchImportDTO dto,
                                                                     Function<MarketEventCreateDTO, MarketEventDO> duplicateResolver,
                                                                     Function<Exception, String> exceptionMessageResolver) {
        List<MarketEventCreateDTO> events = dto == null || dto.getEvents() == null ? List.of() : dto.getEvents();
        if (events.isEmpty()) {
            throw new BizException("MARKET_EVENT_BATCH_IMPORT_EMPTY", "market event batch import is empty");
        }

        EventAutoTriggerConfigService.EventAutoTriggerConfig autoTriggerConfig = eventAutoTriggerConfigService.loadConfig();
        boolean autoTriggerEnabled = autoTriggerConfig != null && Boolean.TRUE.equals(autoTriggerConfig.getEnabled());
        Map<String, Integer> batchDuplicateTracker = new LinkedHashMap<>();
        List<MarketEventBatchPreviewItemVO> items = new java.util.ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        int duplicateCount = 0;
        int autoTriggerCandidateCount = 0;

        for (int i = 0; i < events.size(); i++) {
            MarketEventCreateDTO current = events.get(i);
            MarketEventBatchPreviewItemVO item = new MarketEventBatchPreviewItemVO();
            item.setItemNo(i + 1);
            item.setTargetCode(current == null ? null : marketEventNormalizationManager.trimToNull(current.getTargetCode()));
            item.setTargetName(current == null ? null : marketEventNormalizationManager.trimToNull(current.getTargetName()));
            item.setEventTitle(current == null ? null : marketEventNormalizationManager.trimToNull(current.getEventTitle()));

            try {
                MarketEventCreateDTO prepared = marketEventNormalizationManager.prepareCreateInput(current);
                item.setValid(true);
                item.setNormalizedTargetCode(prepared.getTargetCode());
                item.setNormalizedEventType(prepared.getEventType());
                item.setNormalizedImpactLevel(prepared.getImpactLevel());
                item.setNormalizedEventStatus(prepared.getEventStatus());
                item.setNormalizedSourceChannel(prepared.getSourceChannel());
                item.setNormalizedFingerprint(marketEventNormalizationManager.buildDuplicateFingerprint(prepared));
                item.setProvenanceType(prepared.getProvenanceType());
                item.setConfidenceScore(prepared.getConfidenceScore());

                String duplicateFingerprint = marketEventNormalizationManager.buildDuplicateFingerprint(prepared);
                Integer firstSeenIndex = duplicateFingerprint == null ? null : batchDuplicateTracker.get(duplicateFingerprint);
                MarketEventDO duplicated = firstSeenIndex == null ? duplicateResolver.apply(prepared) : null;
                String duplicateSource = firstSeenIndex != null ? DUPLICATE_SOURCE_BATCH : duplicated != null ? DUPLICATE_SOURCE_EXISTING : null;

                item.setDuplicate(StringUtils.hasText(duplicateSource));
                item.setImportable(!StringUtils.hasText(duplicateSource));
                item.setDuplicateSource(duplicateSource);
                item.setExistingEventId(duplicated == null ? null : duplicated.getEventId());
                if (!StringUtils.hasText(duplicateSource) && duplicateFingerprint != null) {
                    batchDuplicateTracker.put(duplicateFingerprint, i + 1);
                } else if (StringUtils.hasText(duplicateSource)) {
                    duplicateCount++;
                }

                EventAutoTriggerConfigService.EventAutoTriggerRule matchedRule = null;
                String autoTriggerStatus;
                if (StringUtils.hasText(duplicateSource)) {
                    autoTriggerStatus = AUTO_TRIGGER_SKIPPED_DUPLICATE;
                } else if (!autoTriggerEnabled) {
                    autoTriggerStatus = AUTO_TRIGGER_DISABLED;
                } else {
                    matchedRule = resolveMatchedRule(autoTriggerConfig, prepared.getEventType(), prepared.getImpactLevel());
                    if (matchedRule == null) {
                        autoTriggerStatus = AUTO_TRIGGER_NO_MATCH;
                    } else {
                        autoTriggerStatus = AUTO_TRIGGER_WILL_TRIGGER;
                        autoTriggerCandidateCount++;
                    }
                }

                item.setAutoTriggerStatus(autoTriggerStatus);
                item.setAutoTriggerRuleCode(matchedRule == null ? null : matchedRule.getRuleCode());
                item.setEstimatedTaskType(matchedRule == null ? null : matchedRule.getTaskType());
                item.setMessage(resolvePreviewMessage(duplicateSource, duplicated, firstSeenIndex, autoTriggerStatus, matchedRule));
                validCount++;
            } catch (Exception e) {
                item.setValid(false);
                item.setImportable(false);
                item.setDuplicate(false);
                item.setInvalidField(resolvePreviewInvalidField(e));
                item.setAutoTriggerStatus(AUTO_TRIGGER_INVALID);
                item.setMessage(marketEventNormalizationManager.trimMessage(exceptionMessageResolver.apply(e), 255));
                invalidCount++;
            }
            items.add(item);
        }

        MarketEventBatchPreviewResultVO result = new MarketEventBatchPreviewResultVO();
        result.setTotalCount(events.size());
        result.setValidCount(validCount);
        result.setInvalidCount(invalidCount);
        result.setDuplicateCount(duplicateCount);
        result.setAutoTriggerCandidateCount(autoTriggerCandidateCount);
        result.setItems(items);
        return result;
    }

    private EventAutoTriggerConfigService.EventAutoTriggerRule resolveMatchedRule(
            EventAutoTriggerConfigService.EventAutoTriggerConfig config,
            String eventType,
            String impactLevel
    ) {
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return null;
        }
        return config.getRules().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .filter(item -> matchesIgnoreCase(item.getEventTypes(), eventType))
                .filter(item -> matchesIgnoreCase(item.getImpactLevels(), impactLevel))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesIgnoreCase(List<String> expectedValues, String actualValue) {
        if (expectedValues == null || expectedValues.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(actualValue)) {
            return false;
        }
        return expectedValues.stream().anyMatch(item -> actualValue.equalsIgnoreCase(item));
    }

    private String resolvePreviewMessage(String duplicateSource,
                                         MarketEventDO duplicated,
                                         Integer firstSeenIndex,
                                         String autoTriggerStatus,
                                         EventAutoTriggerConfigService.EventAutoTriggerRule matchedRule) {
        if (DUPLICATE_SOURCE_BATCH.equalsIgnoreCase(duplicateSource)) {
            return firstSeenIndex == null ? "duplicate in current batch" : "duplicate with batch item " + firstSeenIndex;
        }
        if (DUPLICATE_SOURCE_EXISTING.equalsIgnoreCase(duplicateSource)) {
            return duplicated == null ? "existing market event found" : "existing market event found: " + duplicated.getEventId();
        }
        if (AUTO_TRIGGER_DISABLED.equalsIgnoreCase(autoTriggerStatus)) {
            return "event auto trigger is disabled";
        }
        if (AUTO_TRIGGER_NO_MATCH.equalsIgnoreCase(autoTriggerStatus)) {
            return "no auto trigger rule matched";
        }
        if (AUTO_TRIGGER_WILL_TRIGGER.equalsIgnoreCase(autoTriggerStatus)) {
            return matchedRule == null ? "will enter auto trigger queue" : "will enter auto trigger queue, rule: " + matchedRule.getRuleCode();
        }
        return "preview validation passed";
    }

    private String resolvePreviewInvalidField(Exception e) {
        if (!(e instanceof BizException bizException)) {
            return null;
        }
        String code = marketEventNormalizationManager.trimToNull(bizException.getCode());
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "MARKET_EVENT_EMPTY", "MARKET_EVENT_BATCH_IMPORT_EMPTY" -> "events";
            case "MARKET_EVENT_TARGET_CODE_EMPTY" -> "targetCode";
            case "MARKET_EVENT_TARGET_NAME_EMPTY" -> "targetName";
            case "MARKET_EVENT_TYPE_EMPTY" -> "eventType";
            case "MARKET_EVENT_TITLE_EMPTY" -> "eventTitle";
            case "MARKET_EVENT_SUMMARY_EMPTY" -> "eventSummary";
            case "MARKET_EVENT_IMPACT_EMPTY" -> "impactLevel";
            case "MARKET_EVENT_OCCURRED_AT_EMPTY" -> "occurredAt";
            default -> null;
        };
    }
}
