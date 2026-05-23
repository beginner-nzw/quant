package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.controller.AuditComplianceController;
import com.quant.aiorchestrator.controller.MarketEventController;
import com.quant.aiorchestrator.controller.MarketIntelligenceController;
import com.quant.aiorchestrator.controller.ModelAgentConfigController;
import com.quant.aiorchestrator.controller.ReportController;
import com.quant.aiorchestrator.controller.ResearchWorkbenchController;
import com.quant.aiorchestrator.controller.RiskWarningController;
import com.quant.aiorchestrator.controller.StrategySignalController;
import com.quant.aiorchestrator.controller.TaskQueryController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskControllerMappingTest {

    @Test
    void taskApiMappingsRemainStableAndDomainGrouped() {
        Set<String> expected = new TreeSet<>(Set.of(
                "GET /api/tasks -> TaskQueryController",
                "GET /api/tasks/failed -> TaskQueryController",
                "GET /api/tasks/stats -> TaskQueryController",
                "GET /api/tasks/{taskId} -> TaskQueryController",
                "GET /api/tasks/{taskId}/agents -> TaskQueryController",
                "GET /api/tasks/{taskId}/audits -> TaskQueryController",
                "GET /api/tasks/{taskId}/full -> TaskQueryController",
                "GET /api/tasks/{taskId}/retries -> TaskQueryController",
                "GET /api/tasks/{taskId}/state -> TaskQueryController",
                "GET /api/tasks/{taskId}/steps -> TaskQueryController",
                "GET /api/tasks/{taskId}/workflow -> TaskQueryController",
                "POST /api/tasks/{taskId}/cancel -> TaskQueryController",
                "POST /api/tasks/{taskId}/retry -> TaskQueryController",
                "GET /api/tasks/market-event-source-configs -> MarketEventController",
                "GET /api/tasks/market-event-stats -> MarketEventController",
                "GET /api/tasks/market-events -> MarketEventController",
                "GET /api/tasks/market-events/cninfo-proxy -> MarketEventController",
                "GET /api/tasks/market-events/ingest-history -> MarketEventController",
                "GET /api/tasks/market-events/{eventId} -> MarketEventController",
                "POST /api/tasks/market-events -> MarketEventController",
                "POST /api/tasks/market-events/batch-import -> MarketEventController",
                "POST /api/tasks/market-events/batch-import/preview -> MarketEventController",
                "POST /api/tasks/market-events/mock-ingest -> MarketEventController",
                "POST /api/tasks/market-events/source-diagnose/{sourceCode} -> MarketEventController",
                "POST /api/tasks/market-events/source-preview/{sourceCode} -> MarketEventController",
                "POST /api/tasks/market-events/source-sync/{sourceCode} -> MarketEventController",
                "GET /api/tasks/risk-warning-stats -> RiskWarningController",
                "GET /api/tasks/risk-warnings -> RiskWarningController",
                "GET /api/tasks/strategy-signal-stats -> StrategySignalController",
                "GET /api/tasks/strategy-signals -> StrategySignalController",
                "GET /api/tasks/strategy-signals/{signalId}/factors -> StrategySignalController",
                "POST /api/tasks/strategy-signals -> StrategySignalController",
                "POST /api/tasks/strategy-signals/{signalId}/status -> StrategySignalController",
                "GET /api/tasks/report-center -> ReportController",
                "GET /api/tasks/report-center-stats -> ReportController",
                "GET /api/tasks/report-review-stats -> ReportController",
                "GET /api/tasks/{taskId}/report -> ReportController",
                "GET /api/tasks/{taskId}/report/review-logs -> ReportController",
                "GET /api/tasks/{taskId}/report/versions -> ReportController",
                "GET /api/tasks/{taskId}/report/versions/compare -> ReportController",
                "GET /api/tasks/{taskId}/report/versions/{versionNo} -> ReportController",
                "POST /api/tasks/{taskId}/report/review -> ReportController",
                "GET /api/tasks/market-intelligence -> MarketIntelligenceController",
                "GET /api/tasks/market-intelligence-stats -> MarketIntelligenceController",
                "GET /api/tasks/audit-compliance -> AuditComplianceController",
                "GET /api/tasks/audit-compliance-stats -> AuditComplianceController",
                "GET /api/tasks/model-agent-config -> ModelAgentConfigController",
                "GET /api/tasks/role-access-configs -> ModelAgentConfigController",
                "POST /api/tasks/model-agent-config/agents/{agentCode} -> ModelAgentConfigController",
                "POST /api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode} -> ModelAgentConfigController",
                "POST /api/tasks/model-agent-config/event-sources/{sourceCode} -> ModelAgentConfigController",
                "POST /api/tasks/model-agent-config/model-strategies/{strategyCode} -> ModelAgentConfigController",
                "POST /api/tasks/model-agent-config/prompt-templates/{templateCode} -> ModelAgentConfigController",
                "POST /api/tasks/model-agent-config/role-access/{roleCode} -> ModelAgentConfigController",
                "POST /api/tasks/model-agent-config/workflows/{workflowCode} -> ModelAgentConfigController",
                "GET /api/tasks/research-workbench -> ResearchWorkbenchController"
        ));

        Set<String> actual = new TreeSet<>();
        actual.addAll(mappingsFor(TaskQueryController.class));
        actual.addAll(mappingsFor(MarketEventController.class));
        actual.addAll(mappingsFor(RiskWarningController.class));
        actual.addAll(mappingsFor(StrategySignalController.class));
        actual.addAll(mappingsFor(ReportController.class));
        actual.addAll(mappingsFor(MarketIntelligenceController.class));
        actual.addAll(mappingsFor(AuditComplianceController.class));
        actual.addAll(mappingsFor(ModelAgentConfigController.class));
        actual.addAll(mappingsFor(ResearchWorkbenchController.class));

        assertEquals(expected, actual);
    }

    private static Set<String> mappingsFor(Class<?> controllerClass) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        String basePath = firstPath(requestMapping.value(), requestMapping.path());
        String controllerName = controllerClass.getSimpleName();

        Set<String> mappings = new TreeSet<>();
        for (Method method : controllerClass.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                mappings.add("GET " + joinPath(basePath, firstPath(getMapping.value(), getMapping.path()))
                        + " -> " + controllerName);
            }
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                mappings.add("POST " + joinPath(basePath, firstPath(postMapping.value(), postMapping.path()))
                        + " -> " + controllerName);
            }
        }
        return mappings;
    }

    private static String firstPath(String[] values, String[] paths) {
        if (values.length > 0) {
            return values[0];
        }
        if (paths.length > 0) {
            return paths[0];
        }
        return "";
    }

    private static String joinPath(String basePath, String methodPath) {
        if (methodPath == null || methodPath.isBlank()) {
            return basePath;
        }
        return basePath + methodPath;
    }
}
