package com.quant.riskservice;

import com.quant.aiorchestrator.controller.RiskWarningController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskWarningControllerMappingTests {

    @Test
    void riskLegacyTaskRoutesRemainStable() {
        Set<String> expected = new TreeSet<>(Set.of(
                "GET /api/tasks/risk-warning-stats -> RiskWarningController",
                "GET /api/tasks/risk-warnings -> RiskWarningController"
        ));

        assertEquals(expected, mappingsFor(RiskWarningController.class));
    }

    private static Set<String> mappingsFor(Class<?> controllerClass) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        List<String> basePaths = pathsOf(requestMapping.value(), requestMapping.path());
        Set<String> mappings = new TreeSet<>();
        for (Method method : controllerClass.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                addMappings(mappings, "GET", basePaths, getMapping.value(), getMapping.path(), controllerClass);
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
                                    Class<?> controllerClass) {
        for (String basePath : basePaths) {
            for (String methodPath : pathsOf(values, paths)) {
                mappings.add(httpMethod + " " + joinPath(basePath, methodPath)
                        + " -> " + controllerClass.getSimpleName());
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
