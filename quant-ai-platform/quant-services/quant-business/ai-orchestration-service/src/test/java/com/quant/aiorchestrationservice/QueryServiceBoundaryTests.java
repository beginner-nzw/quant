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
                implRoot.resolve("ReportQueryServiceImpl.java"),
                implRoot.resolve("RiskQueryServiceImpl.java"),
                implRoot.resolve("StrategyQueryServiceImpl.java"),
                implRoot.resolve("MarketQueryServiceImpl.java"),
                implRoot.resolve("AuditComplianceQueryServiceImpl.java"),
                implRoot.resolve("ModelAgentConfigDashboardQueryServiceImpl.java"),
                implRoot.resolve("ResearchWorkbenchQueryServiceImpl.java")
        );

        for (Path serviceFile : domainQueryServices) {
            String source = Files.readString(serviceFile);
            assertFalse(source.contains("TaskQueryService"), serviceFile + " must not import or inject TaskQueryService");
        }
    }

    @Test
    void researchWorkbenchDoesNotKeepDomainReadModelEntryPoints() throws Exception {
        Path workbenchService = resolveSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl/ResearchWorkbenchQueryServiceImpl.java");
        String source = Files.readString(workbenchService);

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
                "workbench must stay confined to controller/query service/DTO/VO display surfaces: "
                        + unexpectedReferences);
    }

    @Test
    void researchWorkbenchAggregationDoesNotWriteDomainFactsOrPublishEvents() throws Exception {
        Path workbenchService = resolveSourceRoot()
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
        return relativePath.equals("controller/ResearchWorkbenchController.java")
                || relativePath.equals("manager/ResearchWorkbenchProjectionManager.java")
                || relativePath.equals("manager/ResearchWorkbenchItemAssembler.java")
                || relativePath.equals("manager/ResearchWorkbenchReadManager.java")
                || relativePath.equals("manager/ResearchWorkbenchDispositionManager.java")
                || relativePath.equals("service/ResearchWorkbenchQueryService.java")
                || relativePath.equals("service/impl/ResearchWorkbenchQueryServiceImpl.java")
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
}
