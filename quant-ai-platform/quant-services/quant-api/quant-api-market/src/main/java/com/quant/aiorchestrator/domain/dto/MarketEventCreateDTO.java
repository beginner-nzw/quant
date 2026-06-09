package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MarketEventCreateDTO {
    private String targetCode;

    private String targetName;

    private String targetType = "STOCK";

    private String eventType;

    private String eventTitle;

    private String eventSummary;

    private String sourceChannel;

    private String sourceUrl;

    private String provenanceType;

    private String provenanceRef;

    private String provenanceDetail;

    private BigDecimal confidenceScore;

    private String impactLevel;

    private String eventStatus = "ACTIVE";

    private LocalDateTime occurredAt;

    private List<MarketEventRelationDTO> relations;
}
