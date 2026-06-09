package com.quant.aiorchestrator.audit;

import java.util.Map;
import java.util.Set;

public interface HumanReviewQueueTaskProvider {
    Map<String, HumanReviewQueueTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds);
}
