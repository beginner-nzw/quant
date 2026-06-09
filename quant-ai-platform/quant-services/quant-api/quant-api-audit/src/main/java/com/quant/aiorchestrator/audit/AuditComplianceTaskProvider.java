package com.quant.aiorchestrator.audit;

import java.util.Map;
import java.util.Set;

public interface AuditComplianceTaskProvider {
    Map<String, AuditComplianceTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds);
}
