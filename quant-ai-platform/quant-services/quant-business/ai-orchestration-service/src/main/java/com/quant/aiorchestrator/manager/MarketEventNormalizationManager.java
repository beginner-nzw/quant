package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventRelationDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.vo.MarketEventRelationVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MarketEventNormalizationManager {

    private final MarketEventNormalizationRuleManager ruleManager;

    public MarketEventNormalizationManager() {
        this(new MarketEventNormalizationRuleManager());
    }

    public MarketEventCreateDTO prepareCreateInput(MarketEventCreateDTO dto) {
        validateCreateInput(dto);

        MarketEventCreateDTO prepared = new MarketEventCreateDTO();
        prepared.setTargetType(normalizeTargetType(dto.getTargetType()));
        prepared.setTargetCode(normalizeTargetCode(dto.getTargetCode()));
        prepared.setTargetName(trimToNull(dto.getTargetName()));
        prepared.setEventType(normalizeEventType(dto.getEventType()));
        prepared.setEventTitle(trimToNull(dto.getEventTitle()));
        prepared.setEventSummary(trimToNull(dto.getEventSummary()));
        prepared.setSourceChannel(normalizeSourceChannel(dto.getSourceChannel(), prepared.getEventType()));
        prepared.setSourceUrl(trimToNull(dto.getSourceUrl()));
        prepared.setProvenanceType(normalizeProvenanceType(dto.getProvenanceType(), prepared.getSourceChannel()));
        prepared.setProvenanceRef(trimToNull(dto.getProvenanceRef()));
        prepared.setProvenanceDetail(trimMessage(dto.getProvenanceDetail(), 1000));
        prepared.setConfidenceScore(normalizeConfidenceScore(dto.getConfidenceScore()));
        prepared.setImpactLevel(normalizeImpactLevel(dto.getImpactLevel()));
        prepared.setEventStatus(normalizeEventStatus(dto.getEventStatus()));
        prepared.setOccurredAt(dto.getOccurredAt());
        prepared.setRelations(normalizeRelations(dto.getRelations()));
        return prepared;
    }

    public void validateCreateInput(MarketEventCreateDTO dto) {
        if (dto == null) {
            throw new BizException("MARKET_EVENT_EMPTY", "Market event content cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "Target code cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "Target name cannot be empty");
        }
        if (!StringUtils.hasText(dto.getEventType())) {
            throw new BizException("MARKET_EVENT_TYPE_EMPTY", "Event type cannot be empty");
        }
        if (!StringUtils.hasText(dto.getEventTitle())) {
            throw new BizException("MARKET_EVENT_TITLE_EMPTY", "Event title cannot be empty");
        }
        if (!StringUtils.hasText(dto.getEventSummary())) {
            throw new BizException("MARKET_EVENT_SUMMARY_EMPTY", "Event summary cannot be empty");
        }
        if (!StringUtils.hasText(dto.getImpactLevel())) {
            throw new BizException("MARKET_EVENT_IMPACT_EMPTY", "Impact level cannot be empty");
        }
        if (dto.getOccurredAt() == null) {
            throw new BizException("MARKET_EVENT_OCCURRED_AT_EMPTY", "Occurred time cannot be empty");
        }
    }

    public List<MarketEventRelationDTO> normalizeRelations(List<MarketEventRelationDTO> relations) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        Map<String, MarketEventRelationDTO> normalized = new LinkedHashMap<>();
        for (MarketEventRelationDTO relation : relations) {
            if (relation == null) {
                continue;
            }
            String relationType = normalizeRelationType(relation.getRelationType());
            String relationCode = normalizeTargetCode(relation.getRelationCode());
            if (!StringUtils.hasText(relationCode)) {
                continue;
            }
            MarketEventRelationDTO item = new MarketEventRelationDTO();
            item.setRelationType(relationType);
            item.setRelationCode(relationCode);
            item.setRelationName(trimToNull(relation.getRelationName()));
            item.setRelationWeight(relation.getRelationWeight());
            normalized.put(relationType + "|" + relationCode, item);
        }
        return List.copyOf(normalized.values());
    }

    public String buildDuplicateFingerprint(MarketEventCreateDTO dto) {
        if (dto == null || dto.getOccurredAt() == null) {
            return null;
        }
        String targetCode = normalizeTargetCode(dto.getTargetCode());
        String eventType = normalizeEventType(dto.getEventType());
        String sourceUrl = trimToNull(dto.getSourceUrl());
        if (StringUtils.hasText(sourceUrl)) {
            return ruleManager.sha256(String.join("|", defaultIfBlank(targetCode, ""), defaultIfBlank(eventType, ""), sourceUrl.toLowerCase(Locale.ROOT)));
        }
        String eventTitle = trimToNull(dto.getEventTitle());
        if (!StringUtils.hasText(eventTitle)) {
            return null;
        }
        return ruleManager.sha256(String.join("|", defaultIfBlank(targetCode, ""), defaultIfBlank(eventType, ""), eventTitle.toLowerCase(Locale.ROOT), String.valueOf(dto.getOccurredAt())));
    }

    public List<MarketEventRelationVO> toRelationVOs(List<MarketEventRelationDO> relations) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        return relations.stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    MarketEventRelationVO vo = new MarketEventRelationVO();
                    vo.setRelationType(normalizeRelationType(item.getRelationType()));
                    vo.setRelationCode(normalizeTargetCode(item.getRelationCode()));
                    vo.setRelationName(trimToNull(item.getRelationName()));
                    vo.setRelationWeight(item.getRelationWeight());
                    return vo;
                })
                .toList();
    }

    public String trimToNull(String value) {
        return ruleManager.trimToNull(value);
    }

    public String trimMessage(String message, int maxLength) {
        return ruleManager.trimMessage(message, maxLength);
    }

    public String normalizeTargetType(String value) {
        return ruleManager.normalizeTargetType(value);
    }

    public String normalizeRelationType(String value) {
        return ruleManager.normalizeRelationType(value);
    }

    public String normalizeTargetCode(String value) {
        return ruleManager.normalizeTargetCode(value);
    }

    public String normalizeEventType(String value) {
        return ruleManager.normalizeEventType(value);
    }

    public String normalizeImpactLevel(String value) {
        return ruleManager.normalizeImpactLevel(value);
    }

    public String normalizeEventStatus(String value) {
        return ruleManager.normalizeEventStatus(value);
    }

    public String normalizeSourceChannel(String value, String eventType) {
        return ruleManager.normalizeSourceChannel(value, eventType);
    }

    public String normalizeProvenanceType(String value, String sourceChannel) {
        return ruleManager.normalizeProvenanceType(value, sourceChannel);
    }

    public BigDecimal normalizeConfidenceScore(BigDecimal value) {
        return ruleManager.normalizeConfidenceScore(value);
    }

    public BigDecimal defaultConfidence(BigDecimal value) {
        return ruleManager.defaultConfidence(value);
    }

    public String defaultIfBlank(String value, String fallback) {
        return ruleManager.defaultIfBlank(value, fallback);
    }
}
