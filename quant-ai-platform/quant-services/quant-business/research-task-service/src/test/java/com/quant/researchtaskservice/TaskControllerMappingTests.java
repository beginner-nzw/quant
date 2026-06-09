package com.quant.researchtaskservice;

import com.quant.aiorchestrator.controller.TaskQueryController;
import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.domain.vo.AgentExecutionVO;
import com.quant.aiorchestrator.domain.vo.AuditRecordVO;
import com.quant.aiorchestrator.domain.vo.TaskDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskFullDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskPageVO;
import com.quant.aiorchestrator.domain.vo.TaskRetryLogVO;
import com.quant.aiorchestrator.domain.vo.TaskStateVO;
import com.quant.aiorchestrator.domain.vo.TaskStatsVO;
import com.quant.aiorchestrator.domain.vo.TaskStepVO;
import com.quant.aiorchestrator.domain.vo.WorkflowInstanceVO;
import com.quant.common.core.model.Result;
import com.quant.task.service.TaskRoleAccessService;
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

class TaskControllerMappingTests {

    private static final List<EndpointContract> CONTRACTS = List.of(
            endpoint("GET", "/api/tasks/{taskId}", resultOf(TaskDetailVO.class), List.of(path("taskId", String.class)), null),
            endpoint("GET", "/api/tasks/{taskId}/state", resultOf(TaskStateVO.class), List.of(path("taskId", String.class)), null),
            endpoint("GET", "/api/tasks/{taskId}/steps", resultOfList(TaskStepVO.class), List.of(path("taskId", String.class)), null),
            endpoint("GET", "/api/tasks/{taskId}/workflow", resultOf(WorkflowInstanceVO.class), List.of(path("taskId", String.class)), null),
            endpoint("GET", "/api/tasks/{taskId}/agents", resultOfList(AgentExecutionVO.class), List.of(path("taskId", String.class)), null),
            endpoint("GET", "/api/tasks/{taskId}/audits", resultOfList(AuditRecordVO.class), List.of(path("taskId", String.class)), null),
            endpoint("GET", "/api/tasks", resultOf(TaskPageVO.class), List.of(query(TaskPageQueryDTO.class)), null),
            endpoint("POST", "/api/tasks/{taskId}/retry", resultOf(String.class), List.of(path("taskId", String.class), body(TaskRetryDTO.class, false)), TaskRoleAccessService.PERMISSION_TASK_RETRY),
            endpoint("GET", "/api/tasks/{taskId}/retries", resultOfList(TaskRetryLogVO.class), List.of(path("taskId", String.class)), null),
            endpoint("GET", "/api/tasks/{taskId}/full", resultOf(TaskFullDetailVO.class), List.of(path("taskId", String.class)), null),
            endpoint("GET", "/api/tasks/stats", resultOf(TaskStatsVO.class), List.of(), null),
            endpoint("GET", "/api/tasks/failed", resultOf(TaskPageVO.class), List.of(query(TaskPageQueryDTO.class)), null),
            endpoint("POST", "/api/tasks/{taskId}/cancel", resultOf(String.class), List.of(path("taskId", String.class), body(TaskCancelDTO.class, false)), TaskRoleAccessService.PERMISSION_TASK_CANCEL),
            endpoint("POST", "/api/tasks/{taskId}/resume", resultOf(String.class), List.of(path("taskId", String.class), body(TaskWorkflowControlDTO.class, false)), TaskRoleAccessService.PERMISSION_TASK_RETRY),
            endpoint("POST", "/api/tasks/{taskId}/rerun", resultOf(String.class), List.of(path("taskId", String.class), body(TaskWorkflowControlDTO.class, false)), TaskRoleAccessService.PERMISSION_TASK_RETRY)
    );

    @Test
    void legacyTaskApiMappingsRemainInResearchTaskService() {
        Set<String> expected = new TreeSet<>();
        CONTRACTS.forEach(contract -> expected.add(contract.inventoryKey()));

        Set<String> actual = new TreeSet<>();
        for (Method method : TaskQueryController.class.getDeclaredMethods()) {
            for (ControllerMapping mapping : mappingsFor(TaskQueryController.class, method)) {
                actual.add(mapping.inventoryKey(TaskQueryController.class));
            }
        }

        assertEquals(expected, actual);
    }

    @Test
    void legacyTaskEndpointContractsRemainFrozen() throws Exception {
        for (EndpointContract contract : CONTRACTS) {
            Method method = findMappedMethod(contract);

            assertEquals(contract.returnType(), method.getGenericReturnType().getTypeName(),
                    contract.inventoryKey() + " must keep Result<T> response type");
            assertEquals(contract.bindings(), bindingsFor(method),
                    contract.inventoryKey() + " must keep request binding shape");
            assertPermissionCall(contract, method);
        }
    }

    private static Method findMappedMethod(EndpointContract contract) {
        for (Method method : TaskQueryController.class.getDeclaredMethods()) {
            for (ControllerMapping mapping : mappingsFor(TaskQueryController.class, method)) {
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
            for (RequestMethod requestMethod : methodRequestMapping.method()) {
                addMappings(mappings, requestMethod.name(), basePaths,
                        methodRequestMapping.value(), methodRequestMapping.path());
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
            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                bindings.add(body(parameter.getType(), requestBody.required()));
                continue;
            }
            bindings.add(query(parameter.getType()));
        }
        return bindings;
    }

    private static void assertPermissionCall(EndpointContract contract, Method method) throws Exception {
        String methodBody = methodBody(method.getName());
        List<String> actualPermissionArguments = requirePermissionArguments(methodBody);

        if (contract.permission() == null) {
            assertTrue(actualPermissionArguments.isEmpty(),
                    contract.inventoryKey() + " must keep absence of explicit permission checks");
            return;
        }

        assertEquals(1, actualPermissionArguments.size(),
                contract.inventoryKey() + " must keep exactly one explicit permission check");
        assertTrue(actualPermissionArguments.get(0).contains(permissionConstantName(contract.permission())),
                contract.inventoryKey() + " must keep explicit permission behavior: " + actualPermissionArguments);
    }

    private static List<String> requirePermissionArguments(String methodBody) {
        Pattern permissionPattern = Pattern.compile("\\brequirePermission\\s*\\((.*?)\\)", Pattern.DOTALL);
        Matcher matcher = permissionPattern.matcher(methodBody);
        List<String> arguments = new ArrayList<>();
        while (matcher.find()) {
            arguments.add(matcher.group(1).replaceAll("\\s+", " ").trim());
        }
        return arguments;
    }

    private static String methodBody(String methodName) throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java"));
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(").matcher(source);
        if (!matcher.find()) {
            fail("Cannot locate method source: TaskQueryController#" + methodName);
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
        fail("Cannot close method body: TaskQueryController#" + methodName);
        return "";
    }

    private static List<String> pathsOf(String[] values, String[] paths) {
        String[] source = values.length > 0 ? values : paths;
        if (source.length == 0) {
            return List.of("");
        }
        return List.of(source);
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

    private static String permissionConstantName(String permissionValue) {
        return switch (permissionValue) {
            case TaskRoleAccessService.PERMISSION_TASK_RETRY -> "TaskRoleAccessService.PERMISSION_TASK_RETRY";
            case TaskRoleAccessService.PERMISSION_TASK_CANCEL -> "TaskRoleAccessService.PERMISSION_TASK_CANCEL";
            default -> fail("Unexpected permission constant value: " + permissionValue);
        };
    }

    private static EndpointContract endpoint(String httpMethod,
                                             String path,
                                             String returnType,
                                             List<Binding> bindings,
                                             String permission) {
        return new EndpointContract(httpMethod, path, returnType, bindings, permission);
    }

    private static Binding query(Class<?> type) {
        return new Binding(BindingKind.QUERY_OBJECT, "", type, true);
    }

    private static Binding path(String name, Class<?> type) {
        return new Binding(BindingKind.PATH_VARIABLE, name, type, true);
    }

    private static Binding body(Class<?> type, boolean required) {
        return new Binding(BindingKind.REQUEST_BODY, "", type, required);
    }

    private record EndpointContract(String httpMethod,
                                    String path,
                                    String returnType,
                                    List<Binding> bindings,
                                    String permission) {

        private String inventoryKey() {
            return httpMethod + " " + path + " -> " + TaskQueryController.class.getSimpleName();
        }
    }

    private record ControllerMapping(String httpMethod, String path) {

        private String inventoryKey(Class<?> controllerClass) {
            return httpMethod + " " + path + " -> " + controllerClass.getSimpleName();
        }
    }

    private record Binding(BindingKind kind, String name, Class<?> type, boolean required) {
    }

    private enum BindingKind {
        QUERY_OBJECT,
        PATH_VARIABLE,
        REQUEST_BODY
    }
}
