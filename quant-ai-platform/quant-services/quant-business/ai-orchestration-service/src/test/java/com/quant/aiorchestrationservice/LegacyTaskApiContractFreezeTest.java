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
import com.quant.aiorchestrator.domain.dto.AgentConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.dto.PromptTemplateUpdateDTO;
import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.RoleAccessConfigUpdateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.dto.WorkflowConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourcePreviewResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.aiorchestrator.domain.vo.ModelAgentConfigCenterVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterPageVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportReviewStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningPageVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningStatsVO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalPageVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalStatsVO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.common.core.model.Result;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LegacyTaskApiContractFreezeTest {

    private static final String NO_REQUEST_PARAM_DEFAULT = "<none>";

    private static final List<Class<?>> NON_TASK_LEGACY_CONTROLLERS = List.of(
            ReportController.class,
            RiskWarningController.class,
            StrategySignalController.class,
            MarketEventController.class,
            MarketIntelligenceController.class,
            AuditComplianceController.class,
            ModelAgentConfigController.class,
            ResearchWorkbenchController.class
    );

    private static final Set<String> APPROVED_API_TASKS_CONTROLLERS = Set.of(
            "TaskQueryController",
            "ReportController",
            "RiskWarningController",
            "StrategySignalController",
            "MarketEventController",
            "MarketIntelligenceController",
            "AuditComplianceController",
            "ModelAgentConfigController",
            "ResearchWorkbenchController"
    );

    private static final List<EndpointContract> NON_TASK_CONTRACTS = List.of(
            endpoint(ReportController.class, "GET", "/api/tasks/report-center",
                    resultOf(ReportCenterPageVO.class), List.of(query(ReportCenterPageQueryDTO.class)), null),
            endpoint(ReportController.class, "GET", "/api/tasks/report-center-stats",
                    resultOf(ReportCenterStatsVO.class), List.of(), null),
            endpoint(ReportController.class, "GET", "/api/tasks/{taskId}/report",
                    resultOf(TaskReportVO.class), List.of(path("taskId", String.class)), null),
            endpoint(ReportController.class, "GET", "/api/tasks/{taskId}/report/versions",
                    resultOfList(ReportVersionVO.class), List.of(path("taskId", String.class)), null),
            endpoint(ReportController.class, "GET", "/api/tasks/{taskId}/report/versions/compare",
                    resultOf(ReportVersionCompareVO.class), List.of(
                            path("taskId", String.class),
                            requestParam("fromVersionNo", Integer.class),
                            requestParam("toVersionNo", Integer.class)), null),
            endpoint(ReportController.class, "GET", "/api/tasks/{taskId}/report/versions/{versionNo}",
                    resultOf(ReportVersionVO.class), List.of(
                            path("taskId", String.class),
                            path("versionNo", Integer.class)), null),
            endpoint(ReportController.class, "GET", "/api/tasks/{taskId}/report/review-logs",
                    resultOfList(TaskReportReviewLogVO.class), List.of(path("taskId", String.class)), null),
            endpoint(ReportController.class, "POST", "/api/tasks/{taskId}/report/review",
                    resultOf(String.class), List.of(path("taskId", String.class), body(TaskReportReviewDTO.class)),
                    RoleAccessConfigService.PERMISSION_REPORT_REVIEW),
            endpoint(ReportController.class, "GET", "/api/tasks/report-review-stats",
                    resultOf(ReportReviewStatsVO.class), List.of(), null),

            endpoint(RiskWarningController.class, "GET", "/api/tasks/risk-warnings",
                    resultOf(RiskWarningPageVO.class), List.of(query(RiskWarningPageQueryDTO.class)), null),
            endpoint(RiskWarningController.class, "GET", "/api/tasks/risk-warning-stats",
                    resultOf(RiskWarningStatsVO.class), List.of(), null),

            endpoint(StrategySignalController.class, "GET", "/api/tasks/strategy-signals",
                    resultOf(StrategySignalPageVO.class), List.of(query(StrategySignalPageQueryDTO.class)), null),
            endpoint(StrategySignalController.class, "GET", "/api/tasks/strategy-signal-stats",
                    resultOf(StrategySignalStatsVO.class), List.of(), null),
            endpoint(StrategySignalController.class, "POST", "/api/tasks/strategy-signals",
                    resultOf(String.class), List.of(body(StrategySignalCreateDTO.class)),
                    RoleAccessConfigService.PERMISSION_REPORT_REVIEW),
            endpoint(StrategySignalController.class, "GET", "/api/tasks/strategy-signals/{signalId}/factors",
                    resultOfList(StrategySignalFactorItemVO.class), List.of(path("signalId", String.class)), null),
            endpoint(StrategySignalController.class, "POST", "/api/tasks/strategy-signals/{signalId}/status",
                    resultOf(String.class), List.of(path("signalId", String.class),
                            body(StrategySignalStatusUpdateDTO.class)),
                    RoleAccessConfigService.PERMISSION_REPORT_REVIEW),

            endpoint(MarketEventController.class, "GET", "/api/tasks/market-events",
                    resultOf(MarketEventPageVO.class), List.of(query(MarketEventPageQueryDTO.class)), null),
            endpoint(MarketEventController.class, "GET", "/api/tasks/market-event-stats",
                    resultOf(MarketEventStatsVO.class), List.of(), null),
            endpoint(MarketEventController.class, "GET", "/api/tasks/market-events/{eventId}",
                    resultOf(MarketEventListItemVO.class), List.of(path("eventId", String.class)), null),
            endpoint(MarketEventController.class, "GET", "/api/tasks/market-events/ingest-history",
                    resultOfList(MarketEventIngestHistoryItemVO.class), List.of(), null),
            endpoint(MarketEventController.class, "GET", "/api/tasks/market-event-source-configs",
                    resultOfList(EventSourceConfigItemVO.class), List.of(), null),
            endpoint(MarketEventController.class, "POST", "/api/tasks/market-events",
                    resultOf(MarketEventCreateResultVO.class), List.of(body(MarketEventCreateDTO.class)),
                    RoleAccessConfigService.PERMISSION_TASK_CREATE),
            endpoint(MarketEventController.class, "POST", "/api/tasks/market-events/batch-import/preview",
                    resultOf(MarketEventBatchPreviewResultVO.class), List.of(body(MarketEventBatchImportDTO.class)),
                    RoleAccessConfigService.PERMISSION_TASK_CREATE),
            endpoint(MarketEventController.class, "POST", "/api/tasks/market-events/batch-import",
                    resultOf(MarketEventBatchImportResultVO.class), List.of(body(MarketEventBatchImportDTO.class)),
                    RoleAccessConfigService.PERMISSION_TASK_CREATE),
            endpoint(MarketEventController.class, "POST", "/api/tasks/market-events/mock-ingest",
                    resultOf(MarketEventBatchImportResultVO.class), List.of(body(MarketEventMockIngestDTO.class)),
                    RoleAccessConfigService.PERMISSION_TASK_CREATE),
            endpoint(MarketEventController.class, "POST", "/api/tasks/market-events/source-sync/{sourceCode}",
                    resultOf(MarketEventBatchImportResultVO.class), List.of(path("sourceCode", String.class),
                            body(MarketEventSourceSyncDTO.class)),
                    RoleAccessConfigService.PERMISSION_TASK_CREATE),
            endpoint(MarketEventController.class, "POST", "/api/tasks/market-events/source-preview/{sourceCode}",
                    resultOf(EventSourcePreviewResultVO.class), List.of(path("sourceCode", String.class),
                            body(MarketEventSourceSyncDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW),
            endpoint(MarketEventController.class, "POST", "/api/tasks/market-events/source-diagnose/{sourceCode}",
                    resultOf(EventSourceRequestDiagnosticResultVO.class), List.of(path("sourceCode", String.class),
                            body(MarketEventSourceSyncDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW),
            endpoint(MarketEventController.class, "GET", "/api/tasks/market-events/cninfo-proxy",
                    resultOf(CninfoProxyAnnouncementResponseVO.class),
                    List.of(query(MarketEventSourceSyncDTO.class)), null),

            endpoint(MarketIntelligenceController.class, "GET", "/api/tasks/market-intelligence",
                    resultOf(MarketIntelligencePageVO.class),
                    List.of(query(MarketIntelligencePageQueryDTO.class)), null),
            endpoint(MarketIntelligenceController.class, "GET", "/api/tasks/market-intelligence-stats",
                    resultOf(MarketIntelligenceStatsVO.class), List.of(), null),

            endpoint(AuditComplianceController.class, "GET", "/api/tasks/audit-compliance",
                    resultOf(AuditCompliancePageVO.class), List.of(query(AuditCompliancePageQueryDTO.class)),
                    RoleAccessConfigService.PERMISSION_AUDIT_COMPLIANCE_VIEW),
            endpoint(AuditComplianceController.class, "GET", "/api/tasks/audit-compliance-stats",
                    resultOf(AuditComplianceStatsVO.class), List.of(),
                    RoleAccessConfigService.PERMISSION_AUDIT_COMPLIANCE_VIEW),

            endpoint(ModelAgentConfigController.class, "GET", "/api/tasks/model-agent-config",
                    resultOf(ModelAgentConfigCenterVO.class), List.of(),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW),
            endpoint(ModelAgentConfigController.class, "GET", "/api/tasks/role-access-configs",
                    resultOfList(RoleAccessConfigItemVO.class), List.of(), null),
            endpoint(ModelAgentConfigController.class, "POST",
                    "/api/tasks/model-agent-config/prompt-templates/{templateCode}",
                    resultOf(String.class), List.of(path("templateCode", String.class),
                            body(PromptTemplateUpdateDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT),
            endpoint(ModelAgentConfigController.class, "POST",
                    "/api/tasks/model-agent-config/model-strategies/{strategyCode}",
                    resultOf(String.class), List.of(path("strategyCode", String.class),
                            body(ModelStrategyUpdateDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT),
            endpoint(ModelAgentConfigController.class, "POST",
                    "/api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}",
                    resultOf(String.class), List.of(path("ruleCode", String.class),
                            body(EventAutoTriggerRuleUpdateDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT),
            endpoint(ModelAgentConfigController.class, "POST",
                    "/api/tasks/model-agent-config/event-sources/{sourceCode}",
                    resultOf(String.class), List.of(path("sourceCode", String.class),
                            body(EventSourceConfigUpdateDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT),
            endpoint(ModelAgentConfigController.class, "POST", "/api/tasks/model-agent-config/agents/{agentCode}",
                    resultOf(String.class), List.of(path("agentCode", String.class),
                            body(AgentConfigUpdateDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT),
            endpoint(ModelAgentConfigController.class, "POST",
                    "/api/tasks/model-agent-config/workflows/{workflowCode}",
                    resultOf(String.class), List.of(path("workflowCode", String.class),
                            body(WorkflowConfigUpdateDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT),
            endpoint(ModelAgentConfigController.class, "POST",
                    "/api/tasks/model-agent-config/role-access/{roleCode}",
                    resultOf(String.class), List.of(path("roleCode", String.class),
                            body(RoleAccessConfigUpdateDTO.class)),
                    RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT),

            endpoint(ResearchWorkbenchController.class, "GET", "/api/tasks/research-workbench",
                    resultOf(ResearchWorkbenchVO.class), List.of(query(ResearchWorkbenchQueryDTO.class)), null)
    );

    @Test
    void legacyNonTaskEndpointContractsRemainFrozen() throws Exception {
        for (EndpointContract contract : NON_TASK_CONTRACTS) {
            Method method = findMappedMethod(contract);

            assertEquals(contract.returnType(), method.getGenericReturnType().getTypeName(),
                    contract.inventoryKey() + " must keep Result<T> response type");
            assertEquals(contract.bindings(), bindingsFor(method),
                    contract.inventoryKey() + " must keep request binding shape");
            assertPermissionCall(contract, method);
        }
    }

    @Test
    void legacyNonTaskEndpointInventoryRejectsUnregisteredControllerMethods() {
        Set<String> expected = new TreeSet<>();
        for (EndpointContract contract : NON_TASK_CONTRACTS) {
            expected.add(contract.inventoryKey());
        }

        Set<String> actual = new TreeSet<>();
        for (Class<?> controllerClass : NON_TASK_LEGACY_CONTROLLERS) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                for (ControllerMapping mapping : mappingsFor(controllerClass, method)) {
                    actual.add(mapping.inventoryKey(controllerClass));
                }
            }
        }

        assertEquals(expected, actual,
                "legacy non-task /api/tasks/* endpoints must match the Phase 006 inventory");
    }

    @Test
    void allApiTasksEndpointMappingsRemainInApprovedInventory() throws Exception {
        Set<String> actual = new TreeSet<>();
        for (Class<?> controllerClass : controllerClassesFromSource()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                for (ControllerMapping mapping : mappingsFor(controllerClass, method)) {
                    if (isApiTasksPath(mapping.path())) {
                        actual.add(mapping.inventoryKey(controllerClass));
                    }
                }
            }
        }

        assertEquals(approvedApiTasksEndpointInventory(), actual,
                "new /api/tasks endpoint mappings must update the Phase 006 contract inventory");
    }

    @Test
    void apiTasksControllerClassesRemainApprovedSet() throws Exception {
        Set<String> actual = new TreeSet<>();
        Pattern apiTasksBaseMapping = Pattern.compile("@RequestMapping\\s*\\(\\s*"
                + "(?:value\\s*=\\s*|path\\s*=\\s*)?\"/api/tasks\"");

        try (Stream<Path> files = Files.walk(resolveControllerSourceRoot())) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith("Controller.java"))
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            if (apiTasksBaseMapping.matcher(source).find()) {
                                actual.add(path.getFileName().toString().replace(".java", ""));
                            }
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        assertEquals(new TreeSet<>(APPROVED_API_TASKS_CONTROLLERS), actual,
                "new /api/tasks controller owners must update the Phase 006 contract inventory");
    }

    @Test
    void controllersDoNotIntroduceDomainNamespaceAliases() throws Exception {
        Pattern domainNamespaceAlias = Pattern.compile("@(?:RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)"
                + "\\s*\\(\\s*(?:value\\s*=\\s*|path\\s*=\\s*)?\""
                + "/api/(reports|risk|risks|strategy|strategies|market|markets|audit|config|workbench)(?:/|\"|$)");
        List<String> aliases = new ArrayList<>();

        try (Stream<Path> files = Files.walk(resolveControllerSourceRoot())) {
            files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            Matcher matcher = domainNamespaceAlias.matcher(source);
                            while (matcher.find()) {
                                aliases.add(path.getFileName() + ": " + matcher.group());
                            }
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        aliases.sort(Comparator.naturalOrder());
        assertTrue(aliases.isEmpty(), "Phase 006 must not add domain namespace aliases: " + aliases);
    }

    private static Method findMappedMethod(EndpointContract contract) {
        for (Method method : contract.controller().getDeclaredMethods()) {
            for (ControllerMapping mapping : mappingsFor(contract.controller(), method)) {
                if (mapping.httpMethod().equals(contract.httpMethod())
                        && mapping.path().equals(contract.path())) {
                    return method;
                }
            }
        }
        fail("Missing endpoint: " + contract.inventoryKey());
        return null;
    }

    private static List<ControllerMapping> mappingsFor(Class<?> controllerClass, Method method) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        List<String> basePaths = requestMapping == null
                ? List.of("")
                : pathsOf(requestMapping.value(), requestMapping.path());
        List<ControllerMapping> mappings = new ArrayList<>();

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            addMappings(mappings, "GET", basePaths, getMapping.value(), getMapping.path());
        }

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            addMappings(mappings, "POST", basePaths, postMapping.value(), postMapping.path());
        }

        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            addMappings(mappings, "PUT", basePaths, putMapping.value(), putMapping.path());
        }

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            addMappings(mappings, "DELETE", basePaths, deleteMapping.value(), deleteMapping.path());
        }

        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            addMappings(mappings, "PATCH", basePaths, patchMapping.value(), patchMapping.path());
        }

        RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
        if (methodRequestMapping != null) {
            RequestMethod[] requestMethods = methodRequestMapping.method();
            if (requestMethods.length == 0) {
                addMappings(mappings, "ANY", basePaths, methodRequestMapping.value(), methodRequestMapping.path());
            } else {
                for (RequestMethod requestMethod : requestMethods) {
                    addMappings(mappings, requestMethod.name(), basePaths,
                            methodRequestMapping.value(), methodRequestMapping.path());
                }
            }
        }

        return mappings;
    }

    private static List<Binding> bindingsFor(Method method) {
        List<Binding> bindings = new ArrayList<>();

        for (Parameter parameter : method.getParameters()) {
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                bindings.add(path(nameOf(pathVariable.value(), pathVariable.name()), parameter.getType()));
                continue;
            }

            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                bindings.add(requestParam(nameOf(requestParam.value(), requestParam.name()), parameter.getType(),
                        requestParam.required(), normalizedDefaultValue(requestParam.defaultValue())));
                continue;
            }

            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                bindings.add(new Binding(BindingKind.REQUEST_BODY, "", parameter.getType(), requestBody.required(), ""));
                continue;
            }

            bindings.add(query(parameter.getType()));
        }

        return bindings;
    }

    private static void assertPermissionCall(EndpointContract contract, Method method) throws Exception {
        String methodBody = methodBody(contract.controller(), method.getName());
        Pattern permissionPattern = Pattern.compile("requirePermission\\s*\\(\\s*RoleAccessConfigService\\."
                + "(PERMISSION_[A-Z0-9_]+)\\s*\\)");
        Matcher matcher = permissionPattern.matcher(methodBody);
        List<String> actualPermissions = new ArrayList<>();
        while (matcher.find()) {
            actualPermissions.add(matcher.group(1));
        }

        if (contract.permission() == null) {
            assertTrue(actualPermissions.isEmpty(),
                    contract.inventoryKey() + " must keep absence of explicit permission checks");
        } else {
            assertEquals(List.of(permissionConstantName(contract.permission())), actualPermissions,
                    contract.inventoryKey() + " must keep explicit permission behavior");
        }
    }

    private static String methodBody(Class<?> controllerClass, String methodName) throws Exception {
        String source = Files.readString(controllerSourceFile(controllerClass));
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(").matcher(source);

        if (!matcher.find()) {
            fail("Cannot locate method source: " + controllerClass.getSimpleName() + "#" + methodName);
        }

        int openBrace = source.indexOf('{', matcher.end());
        if (openBrace < 0) {
            fail("Cannot locate method body: " + controllerClass.getSimpleName() + "#" + methodName);
        }

        int depth = 0;
        for (int i = openBrace; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(openBrace, i + 1);
                }
            }
        }

        fail("Cannot close method body: " + controllerClass.getSimpleName() + "#" + methodName);
        return "";
    }

    private static Set<String> approvedApiTasksEndpointInventory() {
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
                "POST /api/tasks/{taskId}/retry -> TaskQueryController"
        ));

        for (EndpointContract contract : NON_TASK_CONTRACTS) {
            expected.add(contract.inventoryKey());
        }
        return expected;
    }

    private static List<Class<?>> controllerClassesFromSource() throws Exception {
        List<Class<?>> controllerClasses = new ArrayList<>();

        try (Stream<Path> files = Files.walk(resolveControllerSourceRoot())) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            String simpleName = path.getFileName().toString().replace(".java", "");
                            controllerClasses.add(Class.forName("com.quant.aiorchestrator.controller." + simpleName));
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        return controllerClasses;
    }

    private static Path controllerSourceFile(Class<?> controllerClass) {
        return resolveControllerSourceRoot().resolve(controllerClass.getSimpleName() + ".java");
    }

    private static Path resolveControllerSourceRoot() {
        return resolveSourceRoot().resolve("com/quant/aiorchestrator/controller");
    }

    private static Path resolveSourceRoot() {
        Path moduleSourceRoot = Path.of("src/main/java");
        if (Files.exists(moduleSourceRoot)) {
            return moduleSourceRoot;
        }
        return Path.of("quant-business/ai-orchestration-service/src/main/java");
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

    private static void addMappings(List<ControllerMapping> mappings,
                                    String httpMethod,
                                    List<String> basePaths,
                                    String[] values,
                                    String[] paths) {
        for (String basePath : basePaths) {
            for (String methodPath : pathsOf(values, paths)) {
                mappings.add(new ControllerMapping(httpMethod, joinPath(basePath, methodPath)));
            }
        }
    }

    private static String joinPath(String basePath, String methodPath) {
        if (methodPath == null || methodPath.isBlank()) {
            return basePath;
        }
        return basePath + methodPath;
    }

    private static boolean isApiTasksPath(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedPath.equals("/api/tasks") || normalizedPath.startsWith("/api/tasks/");
    }

    private static String nameOf(String value, String name) {
        if (!value.isBlank()) {
            return value;
        }
        return name;
    }

    private static String resultOf(Class<?> payloadType) {
        return Result.class.getName() + "<" + payloadType.getName() + ">";
    }

    private static String resultOfList(Class<?> elementType) {
        return Result.class.getName() + "<java.util.List<" + elementType.getName() + ">>";
    }

    private static String permissionConstantName(String permissionValue) {
        return switch (permissionValue) {
            case RoleAccessConfigService.PERMISSION_REPORT_REVIEW -> "PERMISSION_REPORT_REVIEW";
            case RoleAccessConfigService.PERMISSION_TASK_CREATE -> "PERMISSION_TASK_CREATE";
            case RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_VIEW -> "PERMISSION_MODEL_AGENT_CONFIG_VIEW";
            case RoleAccessConfigService.PERMISSION_AUDIT_COMPLIANCE_VIEW -> "PERMISSION_AUDIT_COMPLIANCE_VIEW";
            case RoleAccessConfigService.PERMISSION_MODEL_AGENT_CONFIG_EDIT -> "PERMISSION_MODEL_AGENT_CONFIG_EDIT";
            default -> fail("Unexpected permission constant value: " + permissionValue);
        };
    }

    private static EndpointContract endpoint(Class<?> controller,
                                             String httpMethod,
                                             String path,
                                             String returnType,
                                             List<Binding> bindings,
                                             String permission) {
        return new EndpointContract(controller, httpMethod, path, returnType, bindings, permission);
    }

    private static Binding query(Class<?> type) {
        return new Binding(BindingKind.QUERY_OBJECT, "", type, true, "");
    }

    private static Binding path(String name, Class<?> type) {
        return new Binding(BindingKind.PATH_VARIABLE, name, type, true, "");
    }

    private static Binding requestParam(String name, Class<?> type) {
        return requestParam(name, type, true, NO_REQUEST_PARAM_DEFAULT);
    }

    private static Binding requestParam(String name, Class<?> type, boolean required, String defaultValue) {
        return new Binding(BindingKind.REQUEST_PARAM, name, type, required, defaultValue);
    }

    private static Binding body(Class<?> type) {
        return new Binding(BindingKind.REQUEST_BODY, "", type, true, "");
    }

    private static String normalizedDefaultValue(String defaultValue) {
        if (ValueConstants.DEFAULT_NONE.equals(defaultValue)) {
            return NO_REQUEST_PARAM_DEFAULT;
        }
        return defaultValue;
    }

    private record EndpointContract(Class<?> controller,
                                    String httpMethod,
                                    String path,
                                    String returnType,
                                    List<Binding> bindings,
                                    String permission) {

        private String inventoryKey() {
            return httpMethod + " " + path + " -> " + controller.getSimpleName();
        }
    }

    private record ControllerMapping(String httpMethod, String path) {

        private String inventoryKey(Class<?> controllerClass) {
            return httpMethod + " " + path + " -> " + controllerClass.getSimpleName();
        }
    }

    private record Binding(BindingKind kind, String name, Class<?> type, boolean required, String defaultValue) {
    }

    private enum BindingKind {
        QUERY_OBJECT,
        PATH_VARIABLE,
        REQUEST_PARAM,
        REQUEST_BODY
    }
}
