package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StrategySignalFactorItemVO {
    private String factorId;
    private String signalId;
    private String factorCode;
    private String factorName;
    private String factorValue;
    private BigDecimal factorWeight;
    private String factorConclusion;
    private LocalDateTime createdAt;
}
