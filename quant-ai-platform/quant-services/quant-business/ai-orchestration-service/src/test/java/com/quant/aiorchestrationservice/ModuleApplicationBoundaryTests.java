package com.quant.aiorchestrationservice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleApplicationBoundaryTests {

    @Test
    void documentedBusinessAndJobModulesExposeApplicationEntrypoints() {
        Path root = resolveServicesRoot();

        List<String> applicationEntrypoints = List.of(
                "quant-business/user-service/src/main/java/com/quant/user/UserServiceApplication.java",
                "quant-business/market-event-service/src/main/java/com/quant/marketevent/MarketEventServiceApplication.java",
                "quant-business/research-task-service/src/main/java/com/quant/task/ResearchTaskServiceApplication.java",
                "quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/AiOrchestrationServiceApplication.java",
                "quant-business/strategy-service/src/main/java/com/quant/strategy/StrategyServiceApplication.java",
                "quant-business/risk-service/src/main/java/com/quant/risk/RiskServiceApplication.java",
                "quant-business/report-service/src/main/java/com/quant/report/ReportServiceApplication.java",
                "quant-business/audit-service/src/main/java/com/quant/audit/AuditServiceApplication.java",
                "quant-business/subscription-service/src/main/java/com/quant/subscription/SubscriptionServiceApplication.java",
                "quant-business/data-ingest-service/src/main/java/com/quant/dataingest/DataIngestServiceApplication.java",
                "quant-business/config-service/src/main/java/com/quant/config/ConfigServiceApplication.java",
                "quant-job/dashboard-metric-job/src/main/java/com/quant/job/dashboard/DashboardMetricJobApplication.java",
                "quant-job/cache-refresh-job/src/main/java/com/quant/job/cache/CacheRefreshJobApplication.java",
                "quant-job/retry-compensation-job/src/main/java/com/quant/job/retry/RetryCompensationJobApplication.java"
        );

        for (String applicationEntrypoint : applicationEntrypoints) {
            assertTrue(Files.exists(root.resolve(applicationEntrypoint)),
                    applicationEntrypoint + " must exist as a documented module service boundary");
        }
    }

    private Path resolveServicesRoot() {
        Path parentRoot = Path.of("../..").normalize();
        if (Files.exists(parentRoot.resolve("pom.xml")) && Files.exists(parentRoot.resolve("quant-business"))) {
            return parentRoot;
        }
        return Path.of(".");
    }
}
