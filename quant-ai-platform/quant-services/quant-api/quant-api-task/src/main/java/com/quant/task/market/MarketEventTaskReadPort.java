package com.quant.task.market;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MarketEventTaskReadPort {

    long countDistinctSourceEvents(String sourceDomain);

    Map<String, MarketEventTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds);

    MarketEventTaskProjection selectLatestTaskBySourceEvent(String sourceDomain, String sourceEventId);

    Map<String, List<MarketEventTaskProjection>> loadFollowUpTasksBySourceEvents(String sourceDomain,
                                                                                 List<String> sourceEventIds);

    List<MarketEventTaskProjection> loadFollowUpTasks(String sourceDomain,
                                                      Set<String> sourceTaskIds,
                                                      Set<String> sourceReportIds);
}
