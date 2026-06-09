package com.quant.task.workbench;

public record ResearchWorkbenchRiskDetailProjection(
        String warningId,
        String detailDesc,
        String indicatorName,
        String indicatorValue
) {
}
