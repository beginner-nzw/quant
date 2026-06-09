package com.quant.aiapi;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentedDeploymentBoundaryTests {

    private static final Path PLATFORM_ROOT = Path.of("..", "..", "..").normalize().toAbsolutePath();
    private static final Path SERVICES_ROOT = Path.of("..", "..").normalize().toAbsolutePath();

    @Test
    void userAndSubscriptionServicesHaveDockerImages() {
        assertFileContains(SERVICES_ROOT.resolve("quant-business/user-service/Dockerfile"),
                "mvn -pl quant-business/user-service -am clean package -DskipTests",
                "target/user-service.jar");
        assertFileContains(SERVICES_ROOT.resolve("quant-business/subscription-service/Dockerfile"),
                "mvn -pl quant-business/subscription-service -am clean package -DskipTests",
                "target/subscription-service.jar");
    }

    @Test
    void composeIncludesUserAndSubscriptionRuntimeServices() {
        Path compose = PLATFORM_ROOT.resolve("docker/compose/docker-compose.yml");
        assertFileContains(compose,
                "user-service:",
                "subscription-service:",
                "quant-business/user-service/Dockerfile",
                "quant-business/subscription-service/Dockerfile",
                "\"8080:8080\"",
                "\"8088:8088\"");
    }

    @Test
    void gatewayAndObservabilityIncludeUserAndSubscriptionServices() {
        assertFileContains(PLATFORM_ROOT.resolve("docker/gateway/nginx.conf"),
                "upstream user_service_upstream",
                "upstream subscription_service_upstream",
                "location /api/users",
                "location /api/subscriptions");
        assertFileContains(PLATFORM_ROOT.resolve("docker/observability/prometheus.yml"),
                "job_name: user-service",
                "job_name: subscription-service",
                "user-service:8080",
                "subscription-service:8088");
        assertFileContains(PLATFORM_ROOT.resolve("docker/scripts/compose-verification.ps1"),
                "\"user-service\"",
                "\"subscription-service\"",
                "http://127.0.0.1:8080/actuator/health/readiness",
                "http://127.0.0.1:8088/actuator/health/readiness");
    }

    @Test
    void dockerMysqlInitIncludesUserAndSubscriptionTables() {
        assertFileContains(PLATFORM_ROOT.resolve("docker/mysql/init/020_user_subscription_services.sql"),
                "CREATE TABLE IF NOT EXISTS sys_user",
                "CREATE TABLE IF NOT EXISTS sys_user_role",
                "CREATE TABLE IF NOT EXISTS risk_subscription",
                "CREATE TABLE IF NOT EXISTS notification_dispatch");
    }

    private void assertFileContains(Path path, String... expectedValues) {
        assertTrue(Files.exists(path), () -> "Missing file: " + path);
        String content = read(path);
        for (String expectedValue : expectedValues) {
            assertTrue(content.contains(expectedValue),
                    () -> path + " should contain " + expectedValue);
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }
}
