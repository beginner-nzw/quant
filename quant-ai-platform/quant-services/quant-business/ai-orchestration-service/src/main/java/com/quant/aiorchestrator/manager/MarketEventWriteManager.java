package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventRelationDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.mapper.MarketEventAnalysisMapper;
import com.quant.aiorchestrator.mapper.MarketEventRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MarketEventWriteManager {

    private final MarketEventAnalysisMapper marketEventAnalysisMapper;
    private final MarketEventRelationMapper marketEventRelationMapper;
    private final MarketEventNormalizationManager marketEventNormalizationManager;

    public void saveEventRelations(MarketEventDO event, List<MarketEventRelationDTO> extraRelations) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            return;
        }
        Map<String, MarketEventRelationDTO> relations = new LinkedHashMap<>();
        MarketEventRelationDTO primary = new MarketEventRelationDTO();
        primary.setRelationType(marketEventNormalizationManager.normalizeRelationType(event.getTargetType()));
        primary.setRelationCode(marketEventNormalizationManager.normalizeTargetCode(event.getTargetCode()));
        primary.setRelationName(marketEventNormalizationManager.trimToNull(event.getTargetName()));
        primary.setRelationWeight(java.math.BigDecimal.ONE);
        if (StringUtils.hasText(primary.getRelationCode())) {
            relations.put(primary.getRelationType() + "|" + primary.getRelationCode(), primary);
        }
        if (extraRelations != null) {
            for (MarketEventRelationDTO relation : extraRelations) {
                if (relation == null || !StringUtils.hasText(relation.getRelationCode())) {
                    continue;
                }
                relations.put(relation.getRelationType() + "|" + relation.getRelationCode(), relation);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (MarketEventRelationDTO relation : relations.values()) {
            MarketEventRelationDO entity = new MarketEventRelationDO();
            entity.setEventId(event.getEventId());
            entity.setRelationType(relation.getRelationType());
            entity.setRelationCode(relation.getRelationCode());
            entity.setRelationName(relation.getRelationName());
            entity.setRelationWeight(relation.getRelationWeight());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setDeleted(0);
            marketEventRelationMapper.insert(entity);
        }
    }

    public void saveEventAnalysis(MarketEventDO event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        MarketEventAnalysisDO analysis = new MarketEventAnalysisDO();
        analysis.setAnalysisId(UUID.randomUUID().toString());
        analysis.setEventId(event.getEventId());
        analysis.setAnalysisVersion("1.0");
        analysis.setImpactDirection(resolveEventImpactDirection(event));
        analysis.setImpactLevel(marketEventNormalizationManager.normalizeImpactLevel(event.getImpactLevel()));
        analysis.setShortTermView(buildEventShortTermView(event));
        analysis.setMidTermView(buildEventMidTermView(event));
        analysis.setRiskFlag(isRiskEvent(event) ? 1 : 0);
        analysis.setConfidenceScore(marketEventNormalizationManager.defaultConfidence(event.getConfidenceScore()));
        analysis.setAnalysisSummary(buildEventAnalysisSummary(event));
        analysis.setStatus("ACTIVE");
        analysis.setTenantId("default");
        analysis.setCreatedAt(now);
        analysis.setUpdatedAt(now);
        analysis.setDeleted(0);
        marketEventAnalysisMapper.insert(analysis);
    }

    private String resolveEventImpactDirection(MarketEventDO event) {
        if (event == null) {
            return "NEUTRAL";
        }
        String content = marketEventNormalizationManager.defaultIfBlank(event.getEventTitle(), "") + " " + marketEventNormalizationManager.defaultIfBlank(event.getEventSummary(), "");
        String normalized = content.toLowerCase(Locale.ROOT);
        int negativeHit = countKeywords(normalized, List.of("risk", "warning", "loss", "decline", "downside", "miss", "pressure", "default", "investigation", "penalty"));
        int positiveHit = countKeywords(normalized, List.of("growth", "beat", "increase", "upside", "approval", "buyback", "dividend", "profit", "contract"));
        if ("RISK_ALERT".equalsIgnoreCase(event.getEventType()) || negativeHit > positiveHit) {
            return "NEGATIVE";
        }
        if (positiveHit > negativeHit && !"HIGH".equalsIgnoreCase(event.getImpactLevel())) {
            return "POSITIVE";
        }
        return "NEUTRAL";
    }

    private String buildEventShortTermView(MarketEventDO event) {
        String direction = resolveEventImpactDirection(event);
        if ("NEGATIVE".equals(direction) || "HIGH".equalsIgnoreCase(event.getImpactLevel())) {
            return "High attention event; validate price reaction, liquidity and risk exposure before follow-up.";
        }
        if ("POSITIVE".equals(direction)) {
            return "Potential catalyst; compare disclosure quality, valuation sensitivity and near-term confirmation signals.";
        }
        return "Monitor event propagation and wait for confirmed market or disclosure signals.";
    }

    private String buildEventMidTermView(MarketEventDO event) {
        if (event != null && "POLICY".equalsIgnoreCase(event.getEventType())) {
            return "Track policy implementation path and affected related entities from authoritative event relations.";
        }
        if (event != null && "EARNINGS".equalsIgnoreCase(event.getEventType())) {
            return "Track revisions to earnings expectations and follow-up reports generated from this event.";
        }
        return "Track related tasks, reports, risk warnings and strategy signals linked by event_id.";
    }

    private String buildEventAnalysisSummary(MarketEventDO event) {
        if (event == null) {
            return null;
        }
        return String.format(
                "%s/%s event for %s, direction=%s, confidence=%s, provenance=%s",
                marketEventNormalizationManager.defaultIfBlank(event.getEventType(), "OTHER"),
                marketEventNormalizationManager.defaultIfBlank(event.getImpactLevel(), "MEDIUM"),
                marketEventNormalizationManager.defaultIfBlank(event.getTargetCode(), "-"),
                resolveEventImpactDirection(event),
                marketEventNormalizationManager.defaultConfidence(event.getConfidenceScore()).toPlainString(),
                marketEventNormalizationManager.defaultIfBlank(event.getProvenanceType(), "-")
        );
    }

    private boolean isRiskEvent(MarketEventDO event) {
        if (event == null) {
            return false;
        }
        return "RISK_ALERT".equalsIgnoreCase(event.getEventType())
                || "HIGH".equalsIgnoreCase(event.getImpactLevel())
                || "NEGATIVE".equalsIgnoreCase(resolveEventImpactDirection(event));
    }

    private int countKeywords(String content, List<String> keywords) {
        if (!StringUtils.hasText(content) || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && content.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }
}
