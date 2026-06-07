package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MarketEventCreateManager {

    private final MarketEventMapper marketEventMapper;
    private final MarketEventNormalizationManager marketEventNormalizationManager;
    private final MarketEventWriteManager marketEventWriteManager;

    public CreateOutcome createMarketEvent(MarketEventCreateDTO dto) {
        validateCreateInput(dto);
        MarketEventCreateDTO prepared = marketEventNormalizationManager.prepareCreateInput(dto);
        MarketEventDO duplicated = findDuplicatedEvent(prepared);
        if (duplicated != null) {
            return CreateOutcome.duplicate(prepared, duplicated, buildCreateResult(duplicated, true, "duplicated market event"));
        }

        MarketEventDO event = buildEvent(prepared);
        marketEventMapper.insert(event);
        marketEventWriteManager.saveEventRelations(event, prepared.getRelations());
        marketEventWriteManager.saveEventAnalysis(event);
        return CreateOutcome.created(prepared, event, buildCreateResult(event, false, "market event created"));
    }

    public MarketEventDO findDuplicatedEvent(MarketEventCreateDTO dto) {
        String targetCode = marketEventNormalizationManager.normalizeTargetCode(dto.getTargetCode());
        String eventType = marketEventNormalizationManager.normalizeEventType(dto.getEventType());
        String sourceUrl = marketEventNormalizationManager.trimToNull(dto.getSourceUrl());
        String eventTitle = marketEventNormalizationManager.trimToNull(dto.getEventTitle());
        LocalDateTime occurredAt = dto.getOccurredAt();
        String normalizedFingerprint = marketEventNormalizationManager.buildDuplicateFingerprint(dto);

        if (StringUtils.hasText(normalizedFingerprint)) {
            MarketEventDO duplicated = marketEventMapper.selectOne(
                    new LambdaQueryWrapper<MarketEventDO>()
                            .eq(MarketEventDO::getDeleted, 0)
                            .eq(MarketEventDO::getNormalizedFingerprint, normalizedFingerprint)
                            .last("limit 1")
            );
            if (duplicated != null) {
                return duplicated;
            }
        }

        LambdaQueryWrapper<MarketEventDO> wrapper = new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)
                .eq(MarketEventDO::getTargetCode, targetCode)
                .orderByDesc(MarketEventDO::getCreatedAt, MarketEventDO::getId);
        if (StringUtils.hasText(sourceUrl)) {
            wrapper.eq(MarketEventDO::getSourceUrl, sourceUrl);
        } else {
            wrapper.eq(MarketEventDO::getOccurredAt, occurredAt);
        }

        return marketEventMapper.selectList(wrapper).stream()
                .filter(item -> isDuplicatedEvent(item, eventType, sourceUrl, eventTitle, occurredAt))
                .findFirst()
                .orElse(null);
    }

    public MarketEventCreateResultVO buildCreateResult(MarketEventDO event, boolean duplicate, String message) {
        MarketEventCreateResultVO result = new MarketEventCreateResultVO();
        result.setEventId(event == null ? null : event.getEventId());
        result.setDuplicate(duplicate);
        result.setNormalizedFingerprint(event == null ? null : marketEventNormalizationManager.trimToNull(event.getNormalizedFingerprint()));
        result.setAutoTriggerStatus(event == null ? null : marketEventNormalizationManager.trimToNull(event.getAutoTriggerStatus()));
        result.setAutoTriggerTaskId(event == null ? null : marketEventNormalizationManager.trimToNull(event.getAutoTriggerTaskId()));
        result.setAutoTriggerMessage(event == null ? null : marketEventNormalizationManager.trimToNull(event.getAutoTriggerMessage()));
        result.setAutoTriggerReason(event == null ? null : marketEventNormalizationManager.trimToNull(event.getAutoTriggerReason()));
        result.setAutoTriggerFailureCode(event == null ? null : marketEventNormalizationManager.trimToNull(event.getAutoTriggerFailureCode()));
        result.setMessage(marketEventNormalizationManager.trimToNull(message));
        return result;
    }

    private void validateCreateInput(MarketEventCreateDTO dto) {
        if (dto == null) {
            throw new BizException("MARKET_EVENT_EMPTY", "market event content cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "target code cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "target name cannot be empty");
        }
        if (!StringUtils.hasText(dto.getEventType())) {
            throw new BizException("MARKET_EVENT_TYPE_EMPTY", "event type cannot be empty");
        }
        if (!StringUtils.hasText(dto.getEventTitle())) {
            throw new BizException("MARKET_EVENT_TITLE_EMPTY", "event title cannot be empty");
        }
        if (!StringUtils.hasText(dto.getEventSummary())) {
            throw new BizException("MARKET_EVENT_SUMMARY_EMPTY", "event summary cannot be empty");
        }
        if (!StringUtils.hasText(dto.getImpactLevel())) {
            throw new BizException("MARKET_EVENT_IMPACT_EMPTY", "impact level cannot be empty");
        }
        if (dto.getOccurredAt() == null) {
            throw new BizException("MARKET_EVENT_OCCURRED_AT_EMPTY", "event occurred time cannot be empty");
        }
    }

    private MarketEventDO buildEvent(MarketEventCreateDTO prepared) {
        LocalDateTime now = LocalDateTime.now();
        MarketEventDO event = new MarketEventDO();
        event.setEventId(UUID.randomUUID().toString());
        event.setTargetType(prepared.getTargetType());
        event.setTargetCode(prepared.getTargetCode());
        event.setTargetName(prepared.getTargetName());
        event.setEventType(prepared.getEventType());
        event.setEventTitle(prepared.getEventTitle());
        event.setEventSummary(prepared.getEventSummary());
        event.setSourceChannel(prepared.getSourceChannel());
        event.setSourceUrl(prepared.getSourceUrl());
        event.setNormalizedFingerprint(marketEventNormalizationManager.buildDuplicateFingerprint(prepared));
        event.setProvenanceType(prepared.getProvenanceType());
        event.setProvenanceRef(prepared.getProvenanceRef());
        event.setProvenanceDetail(prepared.getProvenanceDetail());
        event.setConfidenceScore(prepared.getConfidenceScore());
        event.setImpactLevel(prepared.getImpactLevel());
        event.setEventStatus(prepared.getEventStatus());
        event.setOccurredAt(prepared.getOccurredAt());
        event.setCreatedBy(String.valueOf(SecurityUtils.currentUserId()));
        event.setCreatedAt(now);
        event.setDeleted(0);
        return event;
    }

    private boolean isDuplicatedEvent(MarketEventDO existing,
                                      String expectedEventType,
                                      String sourceUrl,
                                      String eventTitle,
                                      LocalDateTime occurredAt) {
        if (existing == null || occurredAt == null) {
            return false;
        }
        if (!Objects.equals(marketEventNormalizationManager.normalizeEventType(existing.getEventType()), expectedEventType)) {
            return false;
        }
        if (StringUtils.hasText(sourceUrl)) {
            return sourceUrl.equalsIgnoreCase(marketEventNormalizationManager.defaultIfBlank(existing.getSourceUrl(), ""));
        }
        return eventTitle != null
                && eventTitle.equalsIgnoreCase(marketEventNormalizationManager.defaultIfBlank(existing.getEventTitle(), ""))
                && Objects.equals(existing.getOccurredAt(), occurredAt);
    }

    public record CreateOutcome(
            MarketEventCreateDTO prepared,
            MarketEventDO event,
            MarketEventCreateResultVO result,
            boolean duplicate
    ) {
        private static CreateOutcome duplicate(MarketEventCreateDTO prepared, MarketEventDO event, MarketEventCreateResultVO result) {
            return new CreateOutcome(prepared, event, result, true);
        }

        private static CreateOutcome created(MarketEventCreateDTO prepared, MarketEventDO event, MarketEventCreateResultVO result) {
            return new CreateOutcome(prepared, event, result, false);
        }
    }
}
