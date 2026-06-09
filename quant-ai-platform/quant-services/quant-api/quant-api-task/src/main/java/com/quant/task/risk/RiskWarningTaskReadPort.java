package com.quant.task.risk;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RiskWarningTaskReadPort {
    Map<String, RiskWarningTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds);

    List<RiskWarningTaskProjection> loadRiskWarningFollowUpTasks();

    List<RiskWarningTaskProjection> loadFollowUpTasksBySourceDomain(String sourceDomain);
}
