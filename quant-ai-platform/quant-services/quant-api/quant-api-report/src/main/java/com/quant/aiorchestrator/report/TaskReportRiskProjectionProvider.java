package com.quant.aiorchestrator.report;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TaskReportRiskProjectionProvider {
    Map<String, TaskReportRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds);

    Map<String, List<TaskReportRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds);
}
