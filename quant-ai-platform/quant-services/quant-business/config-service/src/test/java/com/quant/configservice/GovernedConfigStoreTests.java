package com.quant.configservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.configstore.ConfigStoreAuditAppender;
import com.quant.aiorchestrator.configstore.ConfigStoreKey;
import com.quant.aiorchestrator.configstore.GovernedConfigStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GovernedConfigStoreTests {

    @TempDir
    Path tempDir;

    @Test
    void readShouldSeedLegacyJsonAndExposeVersionedStore() throws Exception {
        Path legacyDir = tempDir.resolve("ai-config");
        Files.createDirectories(legacyDir);
        Files.writeString(
                legacyDir.resolve("agent-configs.json"),
                "{\"agents\":[{\"agentCode\":\"planner_agent\",\"enabled\":true}]}",
                StandardCharsets.UTF_8
        );

        GovernedConfigStore store = newStore(tempDir.resolve("store"), legacyDir);

        Map<String, Object> root = store.readRoot(ConfigStoreKey.AGENT, Map.of("agents", List.of()));

        assertEquals(1, ((List<?>) root.get("agents")).size());
        assertTrue(Files.exists(tempDir.resolve("store/agent-configs/current.json")));
        assertTrue(Files.exists(tempDir.resolve("store/agent-configs/versions/agent-configs-v1.json")));
        assertFalse(store.listAudit(ConfigStoreKey.AGENT).isEmpty());
    }

    @Test
    void writeAndRollbackShouldCreateAuditedVersions() throws Exception {
        Path legacyDir = tempDir.resolve("ai-config");
        Files.createDirectories(legacyDir);
        Files.writeString(
                legacyDir.resolve("agent-configs.json"),
                "{\"agents\":[{\"agentCode\":\"planner_agent\",\"enabled\":true}]}",
                StandardCharsets.UTF_8
        );
        ConfigStoreAuditAppender auditService = mock(ConfigStoreAuditAppender.class);
        GovernedConfigStore store = newStore(tempDir.resolve("store"), legacyDir, auditService);

        Map<String, Object> nextRoot = new LinkedHashMap<>();
        nextRoot.put("agents", List.of(Map.of("agentCode", "planner_agent", "enabled", false)));

        GovernedConfigStore.ConfigWriteResult writeResult = store.writeRoot(
                ConfigStoreKey.AGENT,
                nextRoot,
                "planner_agent",
                "planner_agent",
                "UPDATE",
                "update agent",
                List.of("enabled")
        );
        GovernedConfigStore.ConfigWriteResult rollbackResult = store.rollback(
                ConfigStoreKey.AGENT,
                "agent-configs-v1",
                "rollback test"
        );

        Map<String, Object> current = store.readRoot(ConfigStoreKey.AGENT, Map.of("agents", List.of()));
        Map<?, ?> agent = (Map<?, ?>) ((List<?>) current.get("agents")).get(0);
        assertEquals(Boolean.TRUE, agent.get("enabled"));
        assertEquals("agent-configs-v2", writeResult.versionId());
        assertEquals("agent-configs-v3", rollbackResult.versionId());
        assertTrue(store.listAudit(ConfigStoreKey.AGENT).size() >= 3);
        verify(auditService, atLeast(2)).appendAudit(any(), any(), any(), any(), any(), any(), any());
    }

    private GovernedConfigStore newStore(Path storeDir, Path legacyDir) {
        return newStore(storeDir, legacyDir, mock(ConfigStoreAuditAppender.class));
    }

    private GovernedConfigStore newStore(Path storeDir, Path legacyDir, ConfigStoreAuditAppender auditService) {
        return new GovernedConfigStore(
                new ObjectMapper(),
                auditService,
                storeDir.toString(),
                legacyDir.toString(),
                tempDir.resolve("prompt-templates").toString()
        );
    }
}
