package com.quant.aiorchestrator.risk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StrategySignalReadProjection(
        Long id,
        String signalId,
        String taskId,
        String signalType,
        String entityCode,
        String entityName,
        LocalDate signalDate,
        Integer signalScore,
        String signalLevel,
        String signalDirection,
        String reasonSummary,
        BigDecimal confidenceScore,
        String sourceEventId,
        String status,
        LocalDateTime createdAt
) {
}
