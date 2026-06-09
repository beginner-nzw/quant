package com.quant.task.workbench;

import java.util.Map;
import java.util.Set;

public interface ResearchWorkbenchStrategyProvider {
    Map<String, ResearchWorkbenchStrategyProjection> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds);
}
