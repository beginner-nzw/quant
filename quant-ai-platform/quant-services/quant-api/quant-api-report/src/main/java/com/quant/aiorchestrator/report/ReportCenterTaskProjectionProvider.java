package com.quant.aiorchestrator.report;

import java.util.Map;
import java.util.Set;

public interface ReportCenterTaskProjectionProvider {
    Map<String, ReportCenterTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds);
}
