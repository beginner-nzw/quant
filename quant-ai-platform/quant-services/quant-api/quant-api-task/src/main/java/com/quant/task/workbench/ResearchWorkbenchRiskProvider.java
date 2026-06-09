package com.quant.task.workbench;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ResearchWorkbenchRiskProvider {
    Map<String, ResearchWorkbenchRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds);

    Map<String, List<ResearchWorkbenchRiskDetailProjection>> loadRiskWarningDetailMapByWarningIds(Set<String> warningIds);
}
