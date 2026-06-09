package com.quant.aiorchestrator.risk;

import java.util.Map;
import java.util.Set;

public interface StrategySignalReadPort {

    Map<String, StrategySignalReadProjection> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds);
}
