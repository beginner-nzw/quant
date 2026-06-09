package com.quant.task.workbench;

import java.math.BigDecimal;

public record ResearchWorkbenchStrategyProjection(
        String taskId,
        String signalDirection,
        String signalLevel,
        Integer signalScore,
        BigDecimal confidenceScore,
        String reasonSummary
) {
}
