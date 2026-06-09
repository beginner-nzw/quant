package com.quant.aiorchestrationservice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryServiceBoundaryTests {

    @Test
    void nonTaskDomainQueryServicesDoNotDependOnTaskQueryService() throws Exception {
        Path implRoot = resolveSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl");

        List<Path> domainQueryServices = List.of(
        );

        for (Path serviceFile : domainQueryServices) {
            String source = Files.readString(serviceFile);
            assertFalse(source.contains("TaskQueryService"), serviceFile + " must not import or inject TaskQueryService");
        }
    }

    @Test
    void reportQueryImplementationStaysOutOfAiOrchestrationService() throws Exception {
        Path aiImplRoot = resolveSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl");
        Path reportImplRoot = resolveReportServiceSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl");
        Path aiManagerRoot = resolveSourceRoot()
                .resolve("com/quant/aiorchestrator/manager");
        Path reportManagerRoot = resolveReportServiceSourceRoot()
                .resolve("com/quant/aiorchestrator/manager");

        assertFalse(Files.exists(aiImplRoot.resolve("ReportQueryServiceImpl.java")),
                "ReportQueryServiceImpl must stay out of ai-orchestration-service");
        assertTrue(Files.exists(reportImplRoot.resolve("ReportQueryServiceImpl.java")),
                "ReportQueryServiceImpl must be implemented by report-service");
        assertFalse(Files.exists(aiManagerRoot.resolve("ReportCenterProjectionManager.java")),
                "ReportCenterProjectionManager must stay out of ai-orchestration-service");
        assertTrue(Files.exists(reportManagerRoot.resolve("ReportCenterProjectionManager.java")),
                "ReportCenterProjectionManager must be implemented by report-service");
        assertFalse(Files.exists(aiManagerRoot.resolve("TaskReportProjectionManager.java")),
                "TaskReportProjectionManager must stay out of ai-orchestration-service");
        assertTrue(Files.exists(reportManagerRoot.resolve("TaskReportProjectionManager.java")),
                "TaskReportProjectionManager must be implemented by report-service");
        assertFalse(Files.exists(aiManagerRoot.resolve("TaskReportItemAssembler.java")),
                "TaskReportItemAssembler must stay out of ai-orchestration-service");
        assertTrue(Files.exists(reportManagerRoot.resolve("TaskReportItemAssembler.java")),
                "TaskReportItemAssembler must be implemented by report-service");
        assertFalse(Files.exists(aiManagerRoot.resolve("AuditComplianceProjectionManager.java")),
                "AuditComplianceProjectionManager must stay out of ai-orchestration-service");
        assertFalse(Files.exists(aiManagerRoot.resolve("AuditComplianceItemAssembler.java")),
                "AuditComplianceItemAssembler must stay out of ai-orchestration-service");
        Path auditManagerRoot = resolveAuditServiceSourceRoot()
                .resolve("com/quant/aiorchestrator/manager");
        assertTrue(Files.exists(auditManagerRoot.resolve("AuditComplianceProjectionManager.java")),
                "AuditComplianceProjectionManager must be implemented by audit-service");
        assertTrue(Files.exists(auditManagerRoot.resolve("AuditComplianceItemAssembler.java")),
                "AuditComplianceItemAssembler must be implemented by audit-service");
        assertFalse(Files.exists(aiManagerRoot.resolve("HumanReviewQueueManager.java")),
                "HumanReviewQueueManager must stay out of ai-orchestration-service");
        assertTrue(Files.exists(auditManagerRoot.resolve("HumanReviewQueueManager.java")),
                "HumanReviewQueueManager must be implemented by audit-service");
        assertFalse(Files.exists(aiManagerRoot.resolve("HumanReviewCommandManager.java")),
                "HumanReviewCommandManager must stay out of ai-orchestration-service");
        assertFalse(Files.exists(aiManagerRoot.resolve("HumanReviewDecisionManager.java")),
                "HumanReviewDecisionManager must stay out of ai-orchestration-service");
        assertTrue(Files.exists(auditManagerRoot.resolve("HumanReviewCommandManager.java")),
                "HumanReviewCommandManager must be implemented by audit-service");
    }

    @Test
    void auditServiceImplementationsStayOutOfAiOrchestrationService() throws Exception {
        Path aiImplRoot = resolveSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl");
        Path auditImplRoot = resolveAuditServiceSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl");

        List<String> auditOwnedImplementations = List.of(
                "AuditComplianceQueryServiceImpl.java",
                "HumanReviewServiceImpl.java"
        );

        for (String implementation : auditOwnedImplementations) {
            assertFalse(Files.exists(aiImplRoot.resolve(implementation)),
                    implementation + " must stay out of ai-orchestration-service");
            assertTrue(Files.exists(auditImplRoot.resolve(implementation)),
                    implementation + " must be implemented by audit-service");
        }
    }

    @Test
    void marketEventStandardizedConsumerStaysOutOfAiOrchestrationService() {
        Path aiConsumerRoot = resolveSourceRoot()
                .resolve("com/quant/aiorchestrator/consumer");
        Path marketEventConsumerRoot = resolveMarketEventServiceSourceRoot()
                .resolve("com/quant/aiorchestrator/consumer");

        assertFalse(Files.exists(aiConsumerRoot.resolve("MarketEventStandardizedConsumer.java")),
                "MarketEventStandardizedConsumer must stay out of ai-orchestration-service");
        assertTrue(Files.exists(marketEventConsumerRoot.resolve("MarketEventStandardizedConsumer.java")),
                "MarketEventStandardizedConsumer must be implemented by market-event-service");
    }

    @Test
    void researchWorkbenchDoesNotKeepDomainReadModelEntryPoints() throws Exception {
        Path workbenchService = resolveTaskServiceSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java");
        String source = Files.readString(workbenchService);
        assertFalse(Files.exists(resolveSourceRoot()
                        .resolve("com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java")),
                "ResearchWorkbenchQueryServiceImpl must stay out of ai-orchestration-service");

        List<String> forbiddenEntryPoints = List.of(
                "listRiskWarningRecords",
                "listStrategySignalRecords",
                "listReportCenterRecords",
                "listMarketIntelligenceRecords",
                "RiskWarningListItemVO",
                "StrategySignalListItemVO",
                "ReportCenterListItemVO",
                "MarketIntelligenceListItemVO"
        );

        for (String entryPoint : forbiddenEntryPoints) {
            assertFalse(source.contains(entryPoint),
                    workbenchService + " must keep workbench aggregation display-only: " + entryPoint);
        }
    }

    @Test
    void workbenchContractReferencesStayInsideDisplaySurface() throws Exception {
        Path sourceRoot = resolveSourceRoot().resolve("com/quant/aiorchestrator");
        Path taskSourceRoot = resolveTaskServiceSourceRoot().resolve("com/quant/aiorchestrator");
        List<String> unexpectedReferences = new ArrayList<>();
        List<String> workbenchTokens = List.of(
                "ResearchWorkbench",
                "research-workbench",
                "getResearchWorkbench"
        );

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            boolean referencesWorkbench = workbenchTokens.stream().anyMatch(source::contains);
                            if (referencesWorkbench && !isAllowedWorkbenchSurface(sourceRoot, path)) {
                                unexpectedReferences.add(sourceRoot.relativize(path).toString());
                            }
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        assertTrue(unexpectedReferences.isEmpty(),
                "workbench must stay out of ai-orchestration-service and be confined to controller/query service/DTO/VO display surfaces: "
                        + unexpectedReferences);
        for (String manager : List.of(
                "ResearchWorkbenchProjectionManager.java",
                "ResearchWorkbenchReadManager.java",
                "ResearchWorkbenchItemAssembler.java",
                "ResearchWorkbenchDispositionManager.java")) {
            assertTrue(Files.exists(taskSourceRoot.resolve("manager").resolve(manager)),
                    manager + " must be implemented by research-task-service");
        }
    }

    @Test
    void researchWorkbenchAggregationDoesNotWriteDomainFactsOrPublishEvents() throws Exception {
        Path workbenchService = resolveTaskServiceSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java");
        String source = Files.readString(workbenchService);

        List<Pattern> forbiddenWriteOrPublishPatterns = List.of(
                Pattern.compile("\\.insert\\s*\\("),
                Pattern.compile("\\.update\\s*\\("),
                Pattern.compile("\\.updateById\\s*\\("),
                Pattern.compile("\\.delete\\s*\\("),
                Pattern.compile("\\.deleteById\\s*\\("),
                Pattern.compile("opsForValue\\s*\\(\\s*\\)\\.set\\s*\\("),
                Pattern.compile("opsForZSet\\s*\\(\\s*\\)\\.add\\s*\\("),
                Pattern.compile("convertAndSend\\s*\\("),
                Pattern.compile("\\.send\\s*\\(")
        );

        for (Pattern forbiddenPattern : forbiddenWriteOrPublishPatterns) {
            assertFalse(forbiddenPattern.matcher(source).find(),
                    workbenchService + " must remain read-only display aggregation: "
                            + forbiddenPattern.pattern());
        }
    }

    private boolean isAllowedWorkbenchSurface(Path sourceRoot, Path sourceFile) {
        String relativePath = sourceRoot.relativize(sourceFile).toString().replace('\\', '/');
        return relativePath.equals("service/impl/ResearchWorkbenchQueryServiceImpl.java")
                || relativePath.equals("domain/dto/ResearchWorkbenchQueryDTO.java")
                || relativePath.startsWith("domain/vo/ResearchWorkbench");
    }

    private Path resolveSourceRoot() {
        Path moduleSourceRoot = Path.of("src/main/java");
        if (Files.exists(moduleSourceRoot)) {
            return moduleSourceRoot;
        }
        return Path.of("quant-business/ai-orchestration-service/src/main/java");
    }

    private Path resolveTaskServiceSourceRoot() {
        Path siblingTaskSourceRoot = Path.of("../research-task-service/src/main/java").normalize();
        if (Files.exists(siblingTaskSourceRoot)) {
            return siblingTaskSourceRoot;
        }
        return Path.of("quant-business/research-task-service/src/main/java");
    }

    private Path resolveAuditServiceSourceRoot() {
        Path siblingAuditSourceRoot = Path.of("../audit-service/src/main/java").normalize();
        if (Files.exists(siblingAuditSourceRoot)) {
            return siblingAuditSourceRoot;
        }
        return Path.of("quant-business/audit-service/src/main/java");
    }

    private Path resolveReportServiceSourceRoot() {
        Path siblingReportSourceRoot = Path.of("../report-service/src/main/java").normalize();
        if (Files.exists(siblingReportSourceRoot)) {
            return siblingReportSourceRoot;
        }
        return Path.of("quant-business/report-service/src/main/java");
    }

    private Path resolveMarketEventServiceSourceRoot() {
        Path siblingMarketEventSourceRoot = Path.of("../market-event-service/src/main/java").normalize();
        if (Files.exists(siblingMarketEventSourceRoot)) {
            return siblingMarketEventSourceRoot;
        }
        return Path.of("quant-business/market-event-service/src/main/java");
    }
}
