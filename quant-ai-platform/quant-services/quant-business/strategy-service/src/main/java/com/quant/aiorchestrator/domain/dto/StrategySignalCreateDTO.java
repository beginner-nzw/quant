package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StrategySignalCreateDTO {
    private String signalId;
    private String taskId;
    private String signalType;
    private String entityCode;
    private String entityName;
    private LocalDate signalDate;
    private Integer signalScore;
    private String signalLevel;
    private String signalDirection;
    private String reasonSummary;
    private BigDecimal confidenceScore;
    private String sourceEventId;
    private String status;
    private String traceId;
    private String tenantId;
    private List<FactorDTO> factors;

    @Data
    public static class FactorDTO {
        private String factorCode;
        private String factorName;
        private String factorValue;
        private BigDecimal factorWeight;
        private String factorConclusion;
    }
}
