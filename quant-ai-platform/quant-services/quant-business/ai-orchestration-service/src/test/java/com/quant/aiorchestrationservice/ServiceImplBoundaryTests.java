package com.quant.aiorchestrationservice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceImplBoundaryTests {

    @Test
    void serviceImplementationsDoNotOwnInfrastructureAccess() throws Exception {
        Path serviceImplRoot = resolveSourceRoot()
                .resolve("com/quant/aiorchestrator/service/impl");

        Map<String, String> forbiddenImports = Map.ofEntries(
                Map.entry("com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper", "move MyBatis queries into manager/store classes"),
                Map.entry("com.fasterxml.jackson.databind.ObjectMapper", "move JSON serialization into manager/projection classes"),
                Map.entry("org.springframework.data.redis.core.StringRedisTemplate", "move Redis access into runtime/cache managers"),
                Map.entry("org.springframework.kafka.core.KafkaTemplate", "move Kafka access into publisher managers"),
                Map.entry("org.springframework.web.client.RestTemplate", "move HTTP access into adapter/client managers"),
                Map.entry("com.quant.aiorchestrator.mapper.", "move mapper access into manager/store classes")
        );

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(serviceImplRoot)) {
            files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .forEach(path -> inspectServiceFile(serviceImplRoot, path, forbiddenImports, violations));
        }

        assertTrue(violations.isEmpty(),
                "service.impl classes should stay as transaction/API facades; move infrastructure access down: "
                        + violations);
    }

    private void inspectServiceFile(Path serviceImplRoot,
                                    Path path,
                                    Map<String, String> forbiddenImports,
                                    List<String> violations) {
        try {
            String source = Files.readString(path);
            for (Map.Entry<String, String> forbiddenImport : forbiddenImports.entrySet()) {
                if (source.contains("import " + forbiddenImport.getKey())) {
                    violations.add(serviceImplRoot.relativize(path) + " imports "
                            + forbiddenImport.getKey() + ": " + forbiddenImport.getValue());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Path resolveSourceRoot() {
        Path moduleSourceRoot = Path.of("src/main/java");
        if (Files.exists(moduleSourceRoot)) {
            return moduleSourceRoot;
        }
        return Path.of("quant-business/ai-orchestration-service/src/main/java");
    }
}
