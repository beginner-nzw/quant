package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.manager.CsrcRiskDetailFetchManager.DetailContent;
import com.quant.aiorchestrator.manager.CsrcRiskExtractionManager.RiskExtraction;
import com.quant.aiorchestrator.manager.CsrcRiskLinkProjectionManager.RiskLink;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CsrcRiskEventItemAssembler {

    private final CsrcRiskExtractionManager extractionManager;

    public CsrcRiskEventItemAssembler() {
        this(new CsrcRiskExtractionManager());
    }

    public CsrcRiskEventItemAssembler(CsrcRiskExtractionManager extractionManager) {
        this.extractionManager = extractionManager;
    }

    public MarketEventCreateDTO toMarketEvent(RiskLink link,
                                              DetailContent detail,
                                              EventSourceConfigItemVO sourceConfig,
                                              MarketEventSourceSyncDTO request,
                                              boolean targetMatched) {
        String title = defaultIfBlank(detail.title(), link.title());
        String occurredDate = defaultIfBlank(detail.occurredDate(), link.occurredDate());
        RiskExtraction riskExtraction = extractionManager.extractRiskDetail(title, detail.content());
        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        dto.setTargetType(request == null ? "STOCK" : defaultIfBlank(request.getTargetType(), "STOCK"));
        dto.setTargetCode(request == null ? null : trimToNull(request.getTargetCode()));
        dto.setTargetName(request == null ? null : trimToNull(request.getTargetName()));
        dto.setEventType(defaultIfBlank(sourceConfig == null ? null : sourceConfig.getDefaultEventType(), "RISK_ALERT"));
        dto.setEventTitle(title);
        dto.setEventSummary(buildSummary(title, occurredDate, detail.content(), riskExtraction, targetMatched));
        dto.setSourceChannel(defaultIfBlank(sourceConfig == null ? null : sourceConfig.getSourceChannel(), "RISK_MONITOR"));
        dto.setSourceUrl(link.href());
        dto.setImpactLevel(resolveImpactLevel(title, detail.content(), riskExtraction, sourceConfig));
        dto.setEventStatus("ACTIVE");
        dto.setOccurredAt(parseDateTime(occurredDate));
        return dto;
    }

    public String combineText(String... values) {
        return extractionManager.combineText(values);
    }

    private String buildSummary(String title,
                                String occurredDate,
                                String content,
                                RiskExtraction riskExtraction,
                                boolean targetMatched) {
        StringBuilder summary = new StringBuilder();
        if (targetMatched) {
            summary.append("证监会公开监管风险信息");
        } else {
            summary.append("证监会最新监管风险背景信息，未命中该标的精确监管记录");
        }
        if (StringUtils.hasText(occurredDate)) {
            summary.append("（").append(occurredDate.trim()).append("）");
        }
        summary.append("：").append(title);
        String structuredDetail = extractionManager.buildStructuredRiskDetail(riskExtraction, targetMatched);
        if (StringUtils.hasText(structuredDetail)) {
            summary.append("。结构化要素：").append(structuredDetail);
        }
        String excerpt = extractionManager.buildRiskContentExcerpt(content, riskExtraction);
        if (StringUtils.hasText(excerpt)) {
            summary.append("。正文摘要：").append(excerpt);
        }
        return summary.toString();
    }

    private String resolveImpactLevel(String title,
                                      String content,
                                      RiskExtraction riskExtraction,
                                      EventSourceConfigItemVO sourceConfig) {
        String combinedText = combineText(
                title,
                content,
                riskExtraction == null ? null : riskExtraction.regulatoryType(),
                riskExtraction == null ? null : riskExtraction.penaltyAmount()
        );
        if (extractionManager.containsAnyKeyword(
                combinedText,
                List.of("行政处罚", "处罚决定", "市场禁入", "立案调查", "刑事", "移送司法", "罚没")
        )) {
            return "HIGH";
        }
        if (extractionManager.containsAnyKeyword(
                combinedText,
                List.of("警示函", "责令改正", "监管措施", "监管谈话", "纪律处分")
        )) {
            return "MEDIUM";
        }
        return defaultIfBlank(sourceConfig == null ? null : sourceConfig.getDefaultImpactLevel(), "HIGH");
    }

    private LocalDateTime parseDateTime(String rawValue) {
        String value = trimToNull(rawValue);
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.of(9, 0));
        } catch (Exception ignored) {
        }
        return LocalDateTime.now();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
