package com.quant.aiorchestrator.audit;

import java.util.List;

public interface AuditComplianceReportProvider {
    List<AuditComplianceReportProjection> listAuditComplianceReports();
}
