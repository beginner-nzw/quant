package com.quant.aiorchestrator.audit;

import java.util.Map;
import java.util.Set;

public interface AuditComplianceRiskProvider {
    Map<String, AuditComplianceRiskProjection> loadLatestRiskWarningMapByTaskIds(Set<String> taskIds);
}
