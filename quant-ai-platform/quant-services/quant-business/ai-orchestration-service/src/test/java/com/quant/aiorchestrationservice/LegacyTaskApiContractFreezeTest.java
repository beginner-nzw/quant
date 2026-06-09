package com.quant.aiorchestrationservice;

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

    private static final List<Class<?>> NON_TASK_LEGACY_CONTROLLERS = List.of();

    private static final Set<String> APPROVED_API_TASKS_CONTROLLERS = Set.of();

    private static final List<EndpointContract> NON_TASK_CONTRACTS = List.of();

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
                + "(?:value\\s*=\\s*|path\\s*=\\s*)?\"/api/tasks(?:/|\"|$)");

        try (Stream<Path> files = Files.walk(resolveControllerSourceRoot())) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith("Controller.java"))
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            if (apiTasksBaseMapping.matcher(source).find()
                                    || source.contains("@RequestMapping(MarketDataIngestStableContract.LEGACY_TASK_API_BASE)")) {
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
                + "/api/(risk|risks|strategy|strategies|market|markets|audit|config|workbench)(?:/|\"|$)");
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
        assertTrue(aliases.isEmpty(), "Unapproved domain namespace aliases must stay out of legacy route cutover: " + aliases);
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
        List<String> actualPermissionArguments = requirePermissionArguments(methodBody);

        if (contract.permission() == null) {
            assertTrue(actualPermissionArguments.isEmpty(),
                    contract.inventoryKey() + " must keep absence of explicit permission checks");
        } else {
            assertEquals(1, actualPermissionArguments.size(),
                    contract.inventoryKey() + " must keep exactly one explicit permission check");
            String expectedPermissionReference = "RoleAccessConfigService."
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
        Set<String> expected = new TreeSet<>();

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

    private static boolean isApiTasksPath(String path) {
        String normalizedPath = normalizePathPart(path);
        return normalizedPath.equals("/api/tasks") || normalizedPath.startsWith("/api/tasks/");
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
