package com.quant.aiorchestrator.configstore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GovernedConfigStore {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final ConfigChangeAuditService configChangeAuditService;
    private final String storeDirectory;
    private final String legacyConfigDirectory;
    private final String promptTemplateDirectory;
    private final Map<ConfigStoreKey, ReentrantLock> locks = new ConcurrentHashMap<>();

    public GovernedConfigStore(
            ObjectMapper objectMapper,
            ConfigChangeAuditService configChangeAuditService,
            @Value("${quant.ai.config-store-dir:../../../ai-config-store}") String storeDirectory,
            @Value("${quant.ai.config-store-legacy-dir:../../../ai-config}") String legacyConfigDirectory,
            @Value("${quant.ai.prompt-template-dir:../../../prompt-templates}") String promptTemplateDirectory
    ) {
        this.objectMapper = objectMapper;
        this.configChangeAuditService = configChangeAuditService;
        this.storeDirectory = storeDirectory;
        this.legacyConfigDirectory = legacyConfigDirectory;
        this.promptTemplateDirectory = promptTemplateDirectory;
    }

    public Map<String, Object> readRoot(ConfigStoreKey key, Map<String, Object> emptyRoot) {
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            ensureSeeded(key);
            Path currentPath = currentPath(key);
            if (!Files.exists(currentPath)) {
                return deepCopyMap(emptyRoot);
            }
            return objectMapper.readValue(Files.readString(currentPath, StandardCharsets.UTF_8), MAP_TYPE);
        } catch (Exception e) {
            throw new BizException("CONFIG_STORE_READ_FAILED", "read config store failed: " + key.storeCode());
        } finally {
            lock.unlock();
        }
    }

    public ConfigWriteResult writeRoot(ConfigStoreKey key,
                                       Map<String, Object> root,
                                       String targetCode,
                                       String targetName,
                                       String operation,
                                       String changeSummary,
                                       List<String> changedFields) {
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            ensureSeeded(key);
            ConfigMetadata metadata = readMetadata(key);
            int nextVersion = metadata.currentVersion() + 1;
            String versionId = buildVersionId(key, nextVersion);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

            Files.createDirectories(storePath(key).resolve("versions"));
            Files.writeString(versionPath(key, versionId), json + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.writeString(currentPath(key), json + System.lineSeparator(), StandardCharsets.UTF_8);

            ConfigMetadata nextMetadata = metadata.withVersion(nextVersion, versionId, false);
            writeMetadata(key, nextMetadata);
            appendStoreAudit(key, versionId, targetCode, operation, "WRITE", changedFields);
            configChangeAuditService.appendAudit(
                    key.auditType(),
                    targetCode,
                    targetName,
                    operation,
                    currentPath(key).toString(),
                    changeSummary,
                    changedFields
            );
            return new ConfigWriteResult(versionId, nextVersion, currentPath(key).toString());
        } catch (IOException e) {
            throw new BizException("CONFIG_STORE_WRITE_FAILED", "write config store failed: " + key.storeCode());
        } finally {
            lock.unlock();
        }
    }

    public ConfigWriteResult rollback(ConfigStoreKey key, String versionId, String reason) {
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            ensureSeeded(key);
            Path targetVersionPath = versionPath(key, normalizeVersionId(key, versionId));
            if (!Files.exists(targetVersionPath)) {
                throw new BizException("CONFIG_ROLLBACK_VERSION_NOT_FOUND", "config version not found: " + versionId);
            }
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(targetVersionPath, StandardCharsets.UTF_8),
                    MAP_TYPE
            );
            ConfigWriteResult result = writeRoot(
                    key,
                    root,
                    normalizeVersionId(key, versionId),
                    key.storeCode(),
                    "ROLLBACK",
                    hasText(reason) ? reason.trim() : "Rollback governed config",
                    List.of("version")
            );
            appendStoreAudit(key, result.versionId(), normalizeVersionId(key, versionId), "ROLLBACK", "ROLLBACK", List.of("version"));
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("CONFIG_ROLLBACK_FAILED", "rollback config failed: " + key.storeCode());
        } finally {
            lock.unlock();
        }
    }

    public List<Map<String, Object>> listAudit(ConfigStoreKey key) {
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            ensureSeeded(key);
            Path auditPath = auditPath(key);
            if (!Files.exists(auditPath)) {
                return new ArrayList<>();
            }
            Map<String, Object> root = objectMapper.readValue(Files.readString(auditPath, StandardCharsets.UTF_8), MAP_TYPE);
            Object audits = root.get("audits");
            if (!(audits instanceof List<?> rawAudits)) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : rawAudits) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(rawItem, MAP_TYPE)));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("CONFIG_STORE_AUDIT_READ_FAILED", "read config store audit failed: " + key.storeCode());
        } finally {
            lock.unlock();
        }
    }

    public String displayPath(ConfigStoreKey key) {
        return currentPath(key).toString();
    }

    private void ensureSeeded(ConfigStoreKey key) throws IOException {
        Path currentPath = currentPath(key);
        if (Files.exists(currentPath)) {
            return;
        }
        Map<String, Object> seedRoot = switch (key) {
            case PROMPT_TEMPLATE -> loadPromptTemplateSeed();
            default -> loadLegacyJsonSeed(key);
        };
        Files.createDirectories(storePath(key).resolve("versions"));
        String versionId = buildVersionId(key, 1);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(seedRoot);
        Files.writeString(versionPath(key, versionId), json + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.writeString(currentPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        writeMetadata(key, new ConfigMetadata(1, versionId, true));
        appendStoreAudit(key, versionId, "seed", "SEED", "MIGRATION_SEED", List.of("seed"));
    }

    private Map<String, Object> loadLegacyJsonSeed(ConfigStoreKey key) throws IOException {
        Path legacyPath = resolveLegacyPath(key.legacyFileName());
        if (!Files.exists(legacyPath)) {
            return new LinkedHashMap<>();
        }
        return objectMapper.readValue(Files.readString(legacyPath, StandardCharsets.UTF_8), MAP_TYPE);
    }

    private Map<String, Object> loadPromptTemplateSeed() throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> templates = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : promptTemplateFiles().entrySet()) {
            Path path = resolvePromptTemplatePath(entry.getValue());
            templates.put(entry.getKey(), Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8).trim() : "");
        }
        root.put("templates", templates);
        return root;
    }

    private void appendStoreAudit(ConfigStoreKey key,
                                  String versionId,
                                  String targetCode,
                                  String operation,
                                  String eventType,
                                  List<String> changedFields) throws IOException {
        Path auditPath = auditPath(key);
        List<Map<String, Object>> audits = new ArrayList<>();
        if (Files.exists(auditPath)) {
            Map<String, Object> root = objectMapper.readValue(Files.readString(auditPath, StandardCharsets.UTF_8), MAP_TYPE);
            Object rawAudits = root.get("audits");
            if (rawAudits instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> rawItem) {
                        audits.add(new LinkedHashMap<>(objectMapper.convertValue(rawItem, MAP_TYPE)));
                    }
                }
            }
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("auditId", UUID.randomUUID().toString());
        item.put("storeCode", key.storeCode());
        item.put("versionId", versionId);
        item.put("targetCode", targetCode);
        item.put("operation", operation);
        item.put("eventType", eventType);
        item.put("operatorId", defaultText(SecurityUtils.currentUserId(), "unknown"));
        item.put("operatorRole", defaultText(SecurityUtils.currentUserRole(), "UNKNOWN"));
        item.put("changedFields", changedFields == null ? List.of() : changedFields);
        item.put("createdAt", FORMATTER.format(LocalDateTime.now()));
        audits.add(0, item);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("audits", audits);
        Files.createDirectories(auditPath.getParent());
        Files.writeString(auditPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private ConfigMetadata readMetadata(ConfigStoreKey key) throws IOException {
        Path metadataPath = metadataPath(key);
        if (!Files.exists(metadataPath)) {
            return new ConfigMetadata(0, null, false);
        }
        Map<String, Object> root = objectMapper.readValue(Files.readString(metadataPath, StandardCharsets.UTF_8), MAP_TYPE);
        return new ConfigMetadata(readInteger(root.get("currentVersion")), normalize(root.get("currentVersionId")), readBoolean(root.get("seededFromLegacy")));
    }

    private void writeMetadata(ConfigStoreKey key, ConfigMetadata metadata) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("storeCode", key.storeCode());
        root.put("currentVersion", metadata.currentVersion());
        root.put("currentVersionId", metadata.currentVersionId());
        root.put("seededFromLegacy", metadata.seededFromLegacy());
        root.put("updatedAt", FORMATTER.format(LocalDateTime.now()));
        Files.createDirectories(metadataPath(key).getParent());
        Files.writeString(metadataPath(key), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private Path resolveLegacyPath(String fileName) {
        return resolveCandidates(legacyConfigDirectory, "ai-config", fileName);
    }

    private Path resolvePromptTemplatePath(String fileName) {
        return resolveCandidates(promptTemplateDirectory, "prompt-templates", fileName);
    }

    private Path resolveCandidates(String configured, String defaultDir, String fileName) {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        Path configuredPath = Paths.get(configured);
        candidates.add((configuredPath.isAbsolute() ? configuredPath : userDir.resolve(configuredPath)).resolve(fileName).normalize());
        candidates.add(userDir.resolve(defaultDir).resolve(fileName).normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve(defaultDir).resolve(fileName).normalize());
        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve(defaultDir).resolve(fileName).normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve(defaultDir).resolve(fileName).normalize());
            current = current.getParent();
        }
        return candidates.stream().filter(Files::exists).findFirst().orElse(candidates.iterator().next());
    }

    private Map<String, String> promptTemplateFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("planner_agent_template", "planner_agent_template.txt");
        files.put("intent_agent_template", "intent_agent_template.txt");
        files.put("financial_analysis_agent_template", "financial_analysis_agent_template.txt");
        files.put("risk_review_agent_template", "risk_review_agent_template.txt");
        files.put("report_generation_agent_template", "report_generation_agent_template.txt");
        return files;
    }

    private ReentrantLock lockFor(ConfigStoreKey key) {
        return locks.computeIfAbsent(key, ignored -> new ReentrantLock());
    }

    private Path storePath(ConfigStoreKey key) {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        Path configuredPath = Paths.get(storeDirectory);
        Path root = configuredPath.isAbsolute() ? configuredPath : userDir.resolve(configuredPath);
        return root.resolve(key.storeCode()).normalize();
    }

    private Path currentPath(ConfigStoreKey key) {
        return storePath(key).resolve("current.json");
    }

    private Path versionPath(ConfigStoreKey key, String versionId) {
        return storePath(key).resolve("versions").resolve(versionId + ".json");
    }

    private Path metadataPath(ConfigStoreKey key) {
        return storePath(key).resolve("metadata.json");
    }

    private Path auditPath(ConfigStoreKey key) {
        return storePath(key).resolve("audit.json");
    }

    private String buildVersionId(ConfigStoreKey key, int version) {
        return key.storeCode() + "-v" + version;
    }

    private String normalizeVersionId(ConfigStoreKey key, String versionId) {
        if (!hasText(versionId)) {
            throw new BizException("CONFIG_VERSION_EMPTY", "config rollback version cannot be empty");
        }
        String normalized = versionId.trim();
        if (!normalized.toLowerCase(Locale.ROOT).startsWith(key.storeCode().toLowerCase(Locale.ROOT) + "-v")) {
            throw new BizException("CONFIG_VERSION_STORE_MISMATCH", "config version does not belong to store: " + key.storeCode());
        }
        return normalized;
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> value) {
        return objectMapper.convertValue(value, MAP_TYPE);
    }

    private Integer readInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean readBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String defaultText(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? fallback : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record ConfigWriteResult(String versionId, int version, String configPath) {}

    private record ConfigMetadata(int currentVersion, String currentVersionId, boolean seededFromLegacy) {
        private ConfigMetadata withVersion(int version, String versionId, boolean seed) {
            return new ConfigMetadata(version, versionId, seededFromLegacy || seed);
        }
    }
}
