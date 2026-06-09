package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.configstore.ConfigStoreAuditAppender;
import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventSourceConfigCommandManager {

    private final ConfigStoreAuditAppender configStoreAuditAppender;
    private final EventSourceConfigStoreManager eventSourceConfigStoreManager;
    private final EventSourceConfigPolicyManager eventSourceConfigPolicyManager;

    public EventSourceConfigVO loadConfigView(String configPath) {
        EventSourceConfigVO vo = new EventSourceConfigVO();
        vo.setConfigPath(resolveConfigPathForDisplay(configPath));
        vo.setSources(loadSources(configPath));
        return vo;
    }

    public List<EventSourceConfigItemVO> loadSources(String configPath) {
        List<Map<String, Object>> sourceMaps = readSourceMaps(readRootConfig(configPath).get("sources"));
        return sourceMaps.stream()
                .map(eventSourceConfigPolicyManager::toSourceItem)
                .toList();
    }

    public EventSourceConfigItemVO findSource(String configPath, String sourceCode) {
        if (!StringUtils.hasText(sourceCode)) {
            return null;
        }
        return loadSources(configPath).stream()
                .filter(item -> sourceCode.trim().equalsIgnoreCase(item.getSourceCode()))
                .findFirst()
                .orElse(null);
    }

    public void saveSource(String configPath, String sourceCode, EventSourceConfigUpdateDTO dto) {
        eventSourceConfigPolicyManager.validateSaveSource(sourceCode, dto);

        Path path = resolveConfigPath(configPath);
        Map<String, Object> root = readRootConfig(configPath);
        List<Map<String, Object>> sources = readSourceMaps(root.get("sources"));
        boolean updated = false;

        for (Map<String, Object> item : sources) {
            if (sourceCode.trim().equalsIgnoreCase(eventSourceConfigPolicyManager.normalize(item.get("sourceCode")))) {
                Map<String, Object> before = eventSourceConfigPolicyManager.applyUpdate(item, dto);
                appendAudit(path, sourceCode, dto, before, item);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new BizException("EVENT_SOURCE_NOT_FOUND", "event source config not found: " + sourceCode);
        }

        root.put("sources", sources);
        writeRootConfig(path, root);
    }

    public String resolveConfigPathForDisplay(String configPath) {
        return resolveConfigPath(configPath).toString();
    }

    private void appendAudit(
            Path path,
            String sourceCode,
            EventSourceConfigUpdateDTO dto,
            Map<String, Object> before,
            Map<String, Object> after
    ) {
        configStoreAuditAppender.appendAudit(
                "EVENT_SOURCE_CONFIG",
                sourceCode.trim(),
                dto.getSourceName().trim(),
                "UPDATE",
                path.toString(),
                "update event source config",
                eventSourceConfigPolicyManager.diffFields(before, after)
        );
    }

    private Map<String, Object> readRootConfig(String configPath) {
        return eventSourceConfigStoreManager.readRootConfig(configPath);
    }

    private void writeRootConfig(Path path, Map<String, Object> root) {
        eventSourceConfigStoreManager.writeRootConfig(path, root);
    }

    private List<Map<String, Object>> readSourceMaps(Object value) {
        return eventSourceConfigStoreManager.readSourceMaps(value);
    }

    private Path resolveConfigPath(String configPath) {
        return eventSourceConfigStoreManager.resolveConfigPath(configPath);
    }
}
