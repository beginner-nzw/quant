package com.quant.market;

import com.quant.aiorchestrator.controller.MarketEventCoreController;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class MarketEventCoreControllerMappingTests {

    private static final List<EndpointContract> CONTRACTS = List.of(
            endpoint("GET", "/api/tasks/market-events",
                    resultOf(MarketEventPageVO.class), List.of(query(MarketEventPageQueryDTO.class)), null),
            endpoint("GET", "/api/tasks/market-event-stats",
                    resultOf(MarketEventStatsVO.class), List.of(), null),
            endpoint("GET", "/api/tasks/market-events/{eventId}",
                    resultOf(MarketEventListItemVO.class), List.of(path("eventId", String.class)), null),
            endpoint("POST", "/api/tasks/market-events",
                    resultOf(MarketEventCreateResultVO.class), List.of(body(MarketEventCreateDTO.class)),
                    RoleAccessPermissions.TASK_CREATE)
    );

    @Test
    void marketEventCoreLegacyTaskRoutesRemainStable() {
        Set<String> expected = new TreeSet<>();
        for (EndpointContract contract : CONTRACTS) {
            expected.add(contract.inventoryKey());
        }

        assertEquals(expected, mappingsFor(MarketEventCoreController.class));
    }

    @Test
    void marketEventCoreLegacyTaskEndpointContractsRemainFrozen() {
        for (EndpointContract contract : CONTRACTS) {
            Method method = findMappedMethod(contract);

            assertEquals(contract.returnType(), method.getGenericReturnType().getTypeName(),
                    contract.inventoryKey() + " must keep Result<T> response type");
            assertEquals(contract.bindings(), bindingsFor(method),
                    contract.inventoryKey() + " must keep request binding shape");
        }
    }

    private static Method findMappedMethod(EndpointContract contract) {
        for (Method method : MarketEventCoreController.class.getDeclaredMethods()) {
            for (ControllerMapping mapping : controllerMappingsFor(MarketEventCoreController.class, method)) {
                if (mapping.httpMethod().equals(contract.httpMethod())
                        && mapping.path().equals(contract.path())) {
                    return method;
                }
            }
        }
        fail("Missing endpoint: " + contract.inventoryKey());
        return null;
    }

    private static Set<String> mappingsFor(Class<?> controllerClass) {
        Set<String> mappings = new TreeSet<>();
        for (Method method : controllerClass.getDeclaredMethods()) {
            for (ControllerMapping mapping : controllerMappingsFor(controllerClass, method)) {
                mappings.add(mapping.inventoryKey(controllerClass));
            }
        }
        return mappings;
    }

    private static List<ControllerMapping> controllerMappingsFor(Class<?> controllerClass, Method method) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
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
            return httpMethod + " " + path + " -> " + MarketEventCoreController.class.getSimpleName();
        }
    }

    private record ControllerMapping(String httpMethod, String path) {

        private String inventoryKey(Class<?> controllerClass) {
            return httpMethod + " " + path + " -> " + controllerClass.getSimpleName();
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
