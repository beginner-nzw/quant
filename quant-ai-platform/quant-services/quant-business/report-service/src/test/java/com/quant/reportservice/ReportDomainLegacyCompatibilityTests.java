package com.quant.reportservice;

import com.quant.aiorchestrator.report.StableReportContract;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportDomainLegacyCompatibilityTests {

    @Test
    void legacyReportControllerDelegatesToStableReportDomainBoundary() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/quant/aiorchestrator/controller/ReportController.java"));

        assertTrue(source.contains("ReportDomainService"));
        assertFalse(source.contains("private final ReportQueryService"));
        assertFalse(source.contains("private final TaskReportService"));
        assertTrue(source.contains("reportDomainService.pageReportCenter"));
        assertTrue(source.contains("reportDomainService.reviewReport"));
    }

    @Test
    void legacyReportEndpointsRemainRegisteredAsCompatibilitySurface() {
        assertTrue(StableReportContract.LEGACY_COMPAT_ENDPOINTS.contains("GET /api/tasks/report-center"));
        assertTrue(StableReportContract.LEGACY_COMPAT_ENDPOINTS.contains("POST /api/tasks/{taskId}/report/review"));
        assertTrue(StableReportContract.STABLE_ENDPOINTS.contains("GET /api/reports/center"));
        assertTrue(StableReportContract.STABLE_ENDPOINTS.contains("POST /api/reports/tasks/{taskId}/review"));
    }
}
