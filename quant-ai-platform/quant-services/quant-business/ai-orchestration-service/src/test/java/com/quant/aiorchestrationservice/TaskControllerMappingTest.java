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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
        List<String> basePaths = pathsOf(requestMapping.value(), requestMapping.path());
        String controllerName = controllerClass.getSimpleName();

        Set<String> mappings = new TreeSet<>();
        for (Method method : controllerClass.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                addMappings(mappings, "GET", basePaths, getMapping.value(), getMapping.path(), controllerName);
            }
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                addMappings(mappings, "POST", basePaths, postMapping.value(), postMapping.path(), controllerName);
            }
            PutMapping putMapping = method.getAnnotation(PutMapping.class);
            if (putMapping != null) {
                addMappings(mappings, "PUT", basePaths, putMapping.value(), putMapping.path(), controllerName);
            }
            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
            if (deleteMapping != null) {
                addMappings(mappings, "DELETE", basePaths, deleteMapping.value(), deleteMapping.path(), controllerName);
            }
            PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
            if (patchMapping != null) {
                addMappings(mappings, "PATCH", basePaths, patchMapping.value(), patchMapping.path(), controllerName);
            }
            RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
            if (methodRequestMapping != null) {
                RequestMethod[] requestMethods = methodRequestMapping.method();
                if (requestMethods.length == 0) {
                    addMappings(mappings, "ANY", basePaths,
                            methodRequestMapping.value(), methodRequestMapping.path(), controllerName);
                } else {
                    for (RequestMethod requestMethod : requestMethods) {
                        addMappings(mappings, requestMethod.name(), basePaths,
                                methodRequestMapping.value(), methodRequestMapping.path(), controllerName);
                    }
                }
            }
        }
        return mappings;
    }

    private static List<String> pathsOf(String[] values, String[] paths) {
        String[] source = values.length > 0 ? values : paths;
        if (source.length == 0) {
            return List.of("");
        }

        List<String> result = new ArrayList<>();
        for (String path : source) {
            result.add(path);
        }
        return result;
    }

    private static void addMappings(Set<String> mappings,
                                    String httpMethod,
                                    List<String> basePaths,
                                    String[] values,
                                    String[] paths,
                                    String controllerName) {
        for (String basePath : basePaths) {
            for (String methodPath : pathsOf(values, paths)) {
                mappings.add(httpMethod + " " + joinPath(basePath, methodPath) + " -> " + controllerName);
            }
        }
    }

    private static String joinPath(String basePath, String methodPath) {
        String base = normalizePathPart(basePath);
        String method = normalizePathPart(methodPath);
        if (base.isEmpty() || base.equals("/")) {
            return method;
        }
        if (method.isEmpty() || method.equals("/")) {
            return base;
        }
        return base + "/" + method.substring(1);
    }

    private static String normalizePathPart(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
