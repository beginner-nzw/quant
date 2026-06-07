package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.vo.ReportCenterPageVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportReviewStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.report.ReportDomainOwnership;
import com.quant.aiorchestrator.report.StableReportContract;
import com.quant.aiorchestrator.report.api.ReportDomainController;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.common.core.model.Result;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ReportDomainContractTests {

    @Test
    void reportAuthorityObjectsRemainExplicit() {
        assertEquals("ai-orchestration-service", ReportDomainOwnership.CURRENT_HOST);
        assertEquals(Set.of(
                "research_report",
                "research_report_version",
                "research_report_section",
                "report_evidence_ref",
                "research_report_review_log",
                "human_review_record"
        ), ReportDomainOwnership.AUTHORITY_OBJECTS);
    }

    @Test
    void stableReportApiContractRemainsFrozen() {
        assertEquals(new TreeSet<>(StableReportContract.STABLE_ENDPOINTS), mappingsFor(ReportDomainController.class));
        assertEndpoint("GET", "/api/reports/center", resultOf(ReportCenterPageVO.class),
                List.of(query(ReportCenterPageQueryDTO.class)), null);
        assertEndpoint("GET", "/api/reports/center/stats", resultOf(ReportCenterStatsVO.class), List.of(), null);
        assertEndpoint("GET", "/api/reports/review/stats", resultOf(ReportReviewStatsVO.class), List.of(), null);
        assertEndpoint("GET", "/api/reports/tasks/{taskId}", resultOf(TaskReportVO.class),
                List.of(path("taskId", String.class)), null);
        assertEndpoint("GET", "/api/reports/tasks/{taskId}/versions", resultOfList(ReportVersionVO.class),
                List.of(path("taskId", String.class)), null);
        assertEndpoint("GET", "/api/reports/tasks/{taskId}/versions/compare",
                resultOf(ReportVersionCompareVO.class),
                List.of(path("taskId", String.class),
                        requestParam("fromVersionNo", Integer.class),
                        requestParam("toVersionNo", Integer.class)),
                null);
        assertEndpoint("GET", "/api/reports/tasks/{taskId}/versions/{versionNo}", resultOf(ReportVersionVO.class),
                List.of(path("taskId", String.class), path("versionNo", Integer.class)), null);
        assertEndpoint("GET", "/api/reports/tasks/{taskId}/review-logs", resultOfList(TaskReportReviewLogVO.class),
                List.of(path("taskId", String.class)), null);
        assertEndpoint("POST", "/api/reports/tasks/{taskId}/review", resultOf(String.class),
                List.of(path("taskId", String.class), body(TaskReportReviewDTO.class)),
                RoleAccessConfigService.PERMISSION_REPORT_REVIEW);
    }

    @Test
    void reportGeneratedEventContractRemainsStable() {
        assertEquals(KafkaTopicConstants.REPORT_GENERATED, StableReportContract.REPORT_GENERATED_TOPIC);
        assertEquals("report.generated", StableReportContract.REPORT_GENERATED_TOPIC);
        assertEquals(MessageTypeConstants.REPORT_GENERATED, StableReportContract.REPORT_GENERATED_MESSAGE_TYPE);
        assertEquals("REPORT_GENERATED", StableReportContract.REPORT_GENERATED_MESSAGE_TYPE);
        assertEquals("1.0", StableReportContract.REPORT_GENERATED_VERSION);
        assertEquals("REPORT:", StableReportContract.REPORT_GENERATED_BIZ_KEY_PREFIX);
    }

    private static void assertEndpoint(String httpMethod,
                                       String path,
                                       String returnType,
                                       List<Binding> bindings,
                                       String permission) {
        Method method = findMappedMethod(httpMethod, path);
        assertEquals(returnType, method.getGenericReturnType().getTypeName(), path + " return type");
        assertEquals(bindings, bindingsFor(method), path + " bindings");
        assertPermissionCall(method, permission);
    }

    private static Method findMappedMethod(String httpMethod, String path) {
        for (Method method : ReportDomainController.class.getDeclaredMethods()) {
            for (ControllerMapping mapping : mappingsFor(ReportDomainController.class, method)) {
                if (mapping.httpMethod().equals(httpMethod) && mapping.path().equals(path)) {
                    return method;
                }
            }
        }
        fail("Missing stable report endpoint: " + httpMethod + " " + path);
        return null;
    }

    private static Set<String> mappingsFor(Class<?> controllerClass) {
        Set<String> mappings = new TreeSet<>();
        for (Method method : controllerClass.getDeclaredMethods()) {
            for (ControllerMapping mapping : mappingsFor(controllerClass, method)) {
                mappings.add(mapping.httpMethod() + " " + mapping.path());
            }
        }
        return mappings;
    }

    private static List<ControllerMapping> mappingsFor(Class<?> controllerClass, Method method) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        List<String> basePaths = requestMapping == null ? List.of("") : pathsOf(requestMapping.value(), requestMapping.path());
        List<ControllerMapping> mappings = new ArrayList<>();

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            addMappings(mappings, "GET", basePaths, getMapping.value(), getMapping.path());
        }

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            addMappings(mappings, "POST", basePaths, postMapping.value(), postMapping.path());
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
                bindings.add(requestParam(nameOf(requestParam.value(), requestParam.name()), parameter.getType()));
                continue;
            }
            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                bindings.add(body(parameter.getType()));
                continue;
            }
            bindings.add(query(parameter.getType()));
        }
        return bindings;
    }

    private static void assertPermissionCall(Method method, String permission) {
        String source = readSource("src/main/java/com/quant/aiorchestrator/report/api/ReportDomainController.java");
        String methodBody = methodBody(source, method.getName());
        List<String> permissionArguments = requirePermissionArguments(methodBody);
        if (permission == null) {
            assertTrue(permissionArguments.isEmpty(), method.getName() + " must keep no explicit permission check");
            return;
        }
        assertEquals(1, permissionArguments.size(), method.getName() + " must keep one permission check");
        assertTrue(permissionArguments.get(0).contains("RoleAccessConfigService.PERMISSION_REPORT_REVIEW"));
    }

    private static String methodBody(String source, String methodName) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(").matcher(source);
        if (!matcher.find()) {
            fail("Cannot locate method: " + methodName);
        }
        int openBrace = source.indexOf('{', matcher.end());
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
        fail("Cannot close method body: " + methodName);
        return "";
    }

    private static List<String> requirePermissionArguments(String methodBody) {
        Matcher matcher = Pattern.compile("\\brequirePermission\\s*\\((.*?)\\)", Pattern.DOTALL).matcher(methodBody);
        List<String> arguments = new ArrayList<>();
        while (matcher.find()) {
            arguments.add(matcher.group(1).replaceAll("\\s+", " ").trim());
        }
        return arguments;
    }

    private static String readSource(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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

    private static String nameOf(String value, String name) {
        return value.isBlank() ? name : value;
    }

    private static String resultOf(Class<?> payloadType) {
        return Result.class.getName() + "<" + payloadType.getName() + ">";
    }

    private static String resultOfList(Class<?> elementType) {
        return Result.class.getName() + "<java.util.List<" + elementType.getName() + ">>";
    }

    private static Binding query(Class<?> type) {
        return new Binding(BindingKind.QUERY_OBJECT, "", type);
    }

    private static Binding path(String name, Class<?> type) {
        return new Binding(BindingKind.PATH_VARIABLE, name, type);
    }

    private static Binding requestParam(String name, Class<?> type) {
        return new Binding(BindingKind.REQUEST_PARAM, name, type);
    }

    private static Binding body(Class<?> type) {
        return new Binding(BindingKind.REQUEST_BODY, "", type);
    }

    private record ControllerMapping(String httpMethod, String path) {
    }

    private record Binding(BindingKind kind, String name, Class<?> type) {
    }

    private enum BindingKind {
        QUERY_OBJECT,
        PATH_VARIABLE,
        REQUEST_PARAM,
        REQUEST_BODY
    }
}
