package com.quant.aiorchestrator.report;

import java.util.Map;
import java.util.Set;

public interface ReportCenterRiskProjectionProvider {
    Map<String, ReportCenterRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds);
}
