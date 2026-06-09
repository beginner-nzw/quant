package com.quant.aiapi;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentedDatabaseResourceBoundaryTests {

    private static final Path SERVICES_ROOT = Path.of("..", "..").normalize().toAbsolutePath();

    @Test
    void newestDocumentedPersistenceModulesOwnSchemaResources() {
        assertModuleOwnsMigration("quant-business/user-service",
                "V1__create_user_permission_tables.sql",
                "sys_user",
                "sys_user_role");
        assertModuleOwnsMigration("quant-business/subscription-service",
                "V1__create_subscription_notification_tables.sql",
                "risk_subscription",
                "notification_dispatch");
    }

    @Test
    void newestDocumentedPersistenceModulesOwnRuntimeProfiles() {
        assertRuntimeProfiles("quant-business/user-service", "user-service");
        assertRuntimeProfiles("quant-business/subscription-service", "subscription-service");
    }

    @Test
    void newestDocumentedPersistenceModulesExposeHealthDependencies() {
        assertPomContains("quant-business/user-service", "spring-boot-starter-actuator");
        assertPomContains("quant-business/subscription-service", "spring-boot-starter-actuator");
    }

    @Test
    void userServiceOwnsSharedSecurityContextIntegration() {
        assertPomContains("quant-business/user-service", "quant-common-security");
        assertPomContains("quant-business/user-service", "quant-common-web");
        assertSourceContains("quant-business/user-service",
                "src/main/java/com/quant/user/config/CommonInfraConfig.java",
                "UserContextFilter",
                "ServiceActorContextFilter",
                "TraceIdFilter");
        assertSourceContains("quant-business/user-service",
                "src/main/java/com/quant/user/security/UserServiceProfileSource.java",
                "implements UserProfileSource");
        assertSourceContains("quant-business/user-service",
                "src/main/java/com/quant/user/controller/UserPermissionController.java",
                "@GetMapping(\"/me\")");
    }

    @Test
    void subscriptionServiceOwnsProductionNotificationDeliveryBoundary() {
        assertPomContains("quant-business/subscription-service", "quant-common-kafka");
        assertSourceContains("quant-business/subscription-service",
                "src/main/java/com/quant/subscription/delivery/KafkaNotificationDeliveryChannel.java",
                "KafkaTopicConstants.NOTIFICATION_DISPATCH",
                "KafkaTemplate<String, String>",
                "@ConditionalOnProperty");
        assertSourceContains("quant-business/subscription-service",
                "src/main/java/com/quant/subscription/delivery/NotificationDispatchConsumer.java",
                "@KafkaListener",
                "KafkaTopicConstants.NOTIFICATION_DISPATCH",
                "subscription-service-notification-dispatch-group",
                "NotificationMediaAdapter");
        assertSourceContains("quant-business/subscription-service",
                "src/main/java/com/quant/subscription/delivery/NotificationMediaAdapter.java",
                "void dispatch(NotificationDispatchMessage message)");
        assertSourceContains("quant-business/subscription-service",
                "src/main/java/com/quant/subscription/delivery/LocalNotificationDeliveryChannel.java",
                "@ConditionalOnProperty");
        assertRuntimeProfileContains("quant-business/subscription-service",
                "application-local.yml",
                "channel: local");
        assertRuntimeProfileContains("quant-business/subscription-service",
                "application-docker.yml",
                "channel: kafka",
                "bootstrap-servers: kafka:9092",
                "group-id: subscription-service-notification-dispatch-group");
    }

    @Test
    void newestDocumentedPersistenceModulesScanTheirOwnMappers() throws Exception {
        assertMapperScan("quant-business/user-service",
                "src/main/java/com/quant/user/UserServiceApplication.java",
                "com.quant.user.mapper");
        assertMapperScan("quant-business/subscription-service",
                "src/main/java/com/quant/subscription/SubscriptionServiceApplication.java",
                "com.quant.subscription.mapper");
    }

    private void assertModuleOwnsMigration(String modulePath, String migrationFileName, String... tables) {
        Path migration = SERVICES_ROOT.resolve(modulePath)
                .resolve("src/main/resources/db/migration")
                .resolve(migrationFileName);
        assertTrue(Files.exists(migration), () -> "Missing migration file: " + migration);
        String sql = read(migration);
        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table),
                    () -> migration + " should create table " + table);
        }
    }

    private void assertRuntimeProfiles(String modulePath, String applicationName) {
        assertRuntimeProfile(modulePath, "application-local.yml", applicationName, "127.0.0.1");
        assertRuntimeProfile(modulePath, "application-docker.yml", applicationName, "mysql:3306");
    }

    private void assertRuntimeProfile(String modulePath, String profileFileName, String applicationName, String datasourceHost) {
        Path profile = SERVICES_ROOT.resolve(modulePath).resolve("src/main/resources").resolve(profileFileName);
        assertTrue(Files.exists(profile), () -> "Missing runtime profile: " + profile);
        String yaml = read(profile);
        assertTrue(yaml.contains("name: " + applicationName),
                () -> profile + " should declare spring.application.name");
        assertTrue(yaml.contains(datasourceHost),
                () -> profile + " should configure datasource host " + datasourceHost);
        assertTrue(yaml.contains("map-underscore-to-camel-case: true"),
                () -> profile + " should configure MyBatis camel case mapping");
    }

    private void assertRuntimeProfileContains(String modulePath, String profileFileName, String... expectedValues) {
        Path profile = SERVICES_ROOT.resolve(modulePath).resolve("src/main/resources").resolve(profileFileName);
        assertTrue(Files.exists(profile), () -> "Missing runtime profile: " + profile);
        String yaml = read(profile);
        for (String expectedValue : expectedValues) {
            assertTrue(yaml.contains(expectedValue),
                    () -> profile + " should contain " + expectedValue);
        }
    }

    private void assertMapperScan(String modulePath, String applicationPath, String mapperPackage) throws Exception {
        Path application = SERVICES_ROOT.resolve(modulePath).resolve(applicationPath);
        assertTrue(Files.exists(application), () -> "Missing application file: " + application);
        String source = Files.readString(application);
        assertTrue(source.contains("@MapperScan(\"" + mapperPackage + "\")"),
                () -> application + " should scan " + mapperPackage);
    }

    private void assertPomContains(String modulePath, String artifactId) {
        Path pom = SERVICES_ROOT.resolve(modulePath).resolve("pom.xml");
        assertTrue(Files.exists(pom), () -> "Missing pom: " + pom);
        String source = read(pom);
        assertTrue(source.contains("<artifactId>" + artifactId + "</artifactId>"),
                () -> pom + " should depend on " + artifactId);
    }

    private void assertSourceContains(String modulePath, String sourcePath, String... expectedValues) {
        Path sourceFile = SERVICES_ROOT.resolve(modulePath).resolve(sourcePath);
        assertTrue(Files.exists(sourceFile), () -> "Missing source file: " + sourceFile);
        String source = read(sourceFile);
        for (String expectedValue : expectedValues) {
            assertTrue(source.contains(expectedValue),
                    () -> sourceFile + " should contain " + expectedValue);
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
