package com.quant.strategy;

import com.quant.aiorchestrator.controller.StrategySignalController;
import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalPageVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalStatsVO;
import com.quant.common.core.model.Result;
import com.quant.config.api.RoleAccessPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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

class StrategySignalControllerMappingTests {

    private static final List<EndpointContract> CONTRACTS = List.of(
            endpoint("GET", "/api/tasks/strategy-signals",
                    resultOf(StrategySignalPageVO.class), List.of(query(StrategySignalPageQueryDTO.class)), null),
            endpoint("GET", "/api/tasks/strategy-signal-stats",
                    resultOf(StrategySignalStatsVO.class), List.of(), null),
            endpoint("POST", "/api/tasks/strategy-signals",
                    resultOf(String.class), List.of(body(StrategySignalCreateDTO.class)),
                    RoleAccessPermissions.REPORT_REVIEW),
            endpoint("GET", "/api/tasks/strategy-signals/{signalId}/factors",
                    resultOfList(StrategySignalFactorItemVO.class), List.of(path("signalId", String.class)), null),
            endpoint("POST", "/api/tasks/strategy-signals/{signalId}/status",
                    resultOf(String.class), List.of(path("signalId", String.class),
                            body(StrategySignalStatusUpdateDTO.class)),
                    RoleAccessPermissions.REPORT_REVIEW)
    );

    @Test
    void strategyLegacyTaskRoutesRemainStable() {
        Set<String> expected = new TreeSet<>();
        for (EndpointContract contract : CONTRACTS) {
            expected.add(contract.inventoryKey());
        }

        Set<String> actual = new TreeSet<>();
        for (Method method : StrategySignalController.class.getDeclaredMethods()) {
            for (ControllerMapping mapping : mappingsFor(method)) {
                actual.add(mapping.inventoryKey());
            }
        }

        assertEquals(expected, actual);
    }

    @Test
    void strategyLegacyTaskEndpointContractsRemainFrozen() throws Exception {
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
        for (Method method : StrategySignalController.class.getDeclaredMethods()) {
            for (ControllerMapping mapping : mappingsFor(method)) {
                if (mapping.httpMethod().equals(contract.httpMethod())
                        && mapping.path().equals(contract.path())) {
                    return method;
                }
            }
        }
        fail("Missing endpoint: " + contract.inventoryKey());
        return null;
    }

    private static List<ControllerMapping> mappingsFor(Method method) {
        RequestMapping requestMapping = StrategySignalController.class.getAnnotation(RequestMapping.class);
        List<String> basePaths = pathsOf(requestMapping.value(), requestMapping.path());
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

            RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                bindings.add(new Binding(BindingKind.REQUEST_BODY, "", parameter.getType()));
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
        } else {
            assertEquals(1, actualPermissionArguments.size(),
                    contract.inventoryKey() + " must keep exactly one explicit permission check");
            String expectedPermissionReference = "RoleAccessPermissions."
                    + permissionConstantName(contract.permission());
            assertTrue(actualPermissionArguments.get(0).contains(expectedPermissionReference),
                    contract.inventoryKey() + " must keep explicit permission behavior: "
                            + actualPermissionArguments);
        }
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
        String source = Files.readString(controllerSourceFile());
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(").matcher(source);

        if (!matcher.find()) {
            fail("Cannot locate method source: StrategySignalController#" + methodName);
        }

        int openBrace = source.indexOf('{', matcher.end());
        if (openBrace < 0) {
            fail("Cannot locate method body: StrategySignalController#" + methodName);
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

        fail("Cannot close method body: StrategySignalController#" + methodName);
        return "";
    }

    private static Path controllerSourceFile() {
        return Path.of("src/main/java/com/quant/aiorchestrator/controller/StrategySignalController.java");
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
            case RoleAccessPermissions.REPORT_REVIEW -> "REPORT_REVIEW";
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
        return new Binding(BindingKind.QUERY_OBJECT, "", type);
    }

    private static Binding path(String name, Class<?> type) {
        return new Binding(BindingKind.PATH_VARIABLE, name, type);
    }

    private static Binding body(Class<?> type) {
        return new Binding(BindingKind.REQUEST_BODY, "", type);
    }

    private record EndpointContract(String httpMethod,
                                    String path,
                                    String returnType,
                                    List<Binding> bindings,
                                    String permission) {

        private String inventoryKey() {
            return httpMethod + " " + path + " -> " + StrategySignalController.class.getSimpleName();
        }
    }

    private record ControllerMapping(String httpMethod, String path) {

        private String inventoryKey() {
            return httpMethod + " " + path + " -> " + StrategySignalController.class.getSimpleName();
        }
    }

    private record Binding(BindingKind kind, String name, Class<?> type) {
    }

    private enum BindingKind {
        QUERY_OBJECT,
        PATH_VARIABLE,
        REQUEST_BODY
    }
}
