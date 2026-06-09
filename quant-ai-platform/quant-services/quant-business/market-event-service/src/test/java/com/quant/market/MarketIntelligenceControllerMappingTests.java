package com.quant.market;

import com.quant.aiorchestrator.controller.MarketIntelligenceController;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.common.core.model.Result;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class MarketIntelligenceControllerMappingTests {

    private static final List<EndpointContract> CONTRACTS = List.of(
            endpoint("GET", "/api/tasks/market-intelligence",
                    resultOf(MarketIntelligencePageVO.class),
                    List.of(query(MarketIntelligencePageQueryDTO.class))),
            endpoint("GET", "/api/tasks/market-intelligence-stats",
                    resultOf(MarketIntelligenceStatsVO.class),
                    List.of())
    );

    @Test
    void marketIntelligenceLegacyTaskRoutesRemainStable() {
        Set<String> expected = new TreeSet<>();
        for (EndpointContract contract : CONTRACTS) {
            expected.add(contract.inventoryKey());
        }

        assertEquals(expected, mappingsFor(MarketIntelligenceController.class));
    }

    @Test
    void marketIntelligenceLegacyTaskEndpointContractsRemainFrozen() {
        for (EndpointContract contract : CONTRACTS) {
            Method method = findMappedMethod(contract);

            assertEquals(contract.returnType(), method.getGenericReturnType().getTypeName(),
                    contract.inventoryKey() + " must keep Result<T> response type");
            assertEquals(contract.bindings(), bindingsFor(method),
                    contract.inventoryKey() + " must keep request binding shape");
        }
    }

    private static Method findMappedMethod(EndpointContract contract) {
        for (Method method : MarketIntelligenceController.class.getDeclaredMethods()) {
            for (ControllerMapping mapping : controllerMappingsFor(MarketIntelligenceController.class, method)) {
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

        return mappings;
    }

    private static List<Binding> bindingsFor(Method method) {
        List<Binding> bindings = new ArrayList<>();

        for (Parameter parameter : method.getParameters()) {
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

    private static String resultOf(Class<?> payloadType) {
        return Result.class.getName() + "<" + payloadType.getName() + ">";
    }

    private static EndpointContract endpoint(String httpMethod,
                                             String path,
                                             String returnType,
                                             List<Binding> bindings) {
        return new EndpointContract(httpMethod, path, returnType, bindings);
    }

    private static Binding query(Class<?> type) {
        return new Binding(BindingKind.QUERY_OBJECT, "", type);
    }

    private record EndpointContract(String httpMethod,
                                    String path,
                                    String returnType,
                                    List<Binding> bindings) {

        private String inventoryKey() {
            return httpMethod + " " + path + " -> " + MarketIntelligenceController.class.getSimpleName();
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
        QUERY_OBJECT
    }
}
