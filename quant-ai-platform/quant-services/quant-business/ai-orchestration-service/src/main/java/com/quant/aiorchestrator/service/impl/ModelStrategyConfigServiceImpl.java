package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.vo.ModelStrategyItemVO;
import com.quant.common.core.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ModelStrategyConfigServiceImpl implements ModelStrategyConfigService {

    private final String modelStrategyConfigPath;
    private final ObjectMapper objectMapper;
    private final ConfigChangeAuditService configChangeAuditService;

    public ModelStrategyConfigServiceImpl(
            @Value("${quant.ai.model-strategy-config:../../../ai-config/model-strategies.json}") String modelStrategyConfigPath,
            ObjectMapper objectMapper,
            ConfigChangeAuditService configChangeAuditService
    ) {
        this.modelStrategyConfigPath = modelStrategyConfigPath;
        this.objectMapper = objectMapper;
        this.configChangeAuditService = configChangeAuditService;
    }

    public List<ModelStrategyItemVO> loadStrategies() {
        List<Map<String, Object>> strategies = readStrategies();
        List<ModelStrategyItemVO> result = new ArrayList<>();
        for (Map<String, Object> item : strategies) {
            result.add(toStrategyItem(item));
        }
        return result;
    }

    public void saveStrategy(String strategyCode, ModelStrategyUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("MODEL_STRATEGY_EMPTY", "模型策略更新内容不能为空");
        }
        if (!hasText(strategyCode)) {
            throw new BizException("MODEL_STRATEGY_CODE_EMPTY", "模型策略编码不能为空");
        }
        if (!hasText(dto.getProvider())) {
            throw new BizException("MODEL_STRATEGY_PROVIDER_EMPTY", "模型提供方不能为空");
        }
        if (!hasText(dto.getModelName())) {
            throw new BizException("MODEL_STRATEGY_NAME_EMPTY", "模型名称不能为空");
        }
        if (!hasText(dto.getBaseUrl())) {
            throw new BizException("MODEL_STRATEGY_BASE_URL_EMPTY", "模型地址不能为空");
        }
        if (!hasText(dto.getAccessMode())) {
            throw new BizException("MODEL_STRATEGY_ACCESS_MODE_EMPTY", "接入模式不能为空");
        }

        Path configPath = resolveConfigPath();
        List<Map<String, Object>> strategies = readStrategies();
        boolean updated = false;
        for (Map<String, Object> item : strategies) {
            if (Objects.equals(normalize(item.get("strategyCode")), strategyCode.trim())) {
                Map<String, Object> before = new LinkedHashMap<>(item);
                applyUpdate(item, dto);
                updated = true;
                configChangeAuditService.appendAudit(
                        "MODEL_STRATEGY",
                        strategyCode,
                        strategyCode,
                        "UPDATE",
                        configPath.toString(),
                        "更新模型策略配置",
                        diffFields(before, item)
                );
                break;
            }
        }

        if (!updated) {
            throw new BizException("MODEL_STRATEGY_NOT_FOUND", "未找到模型策略: " + strategyCode);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("strategies", strategies);

        try {
            Files.createDirectories(configPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(configPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("MODEL_STRATEGY_SAVE_FAILED", "保存模型策略失败: " + strategyCode);
        }
    }

    public String resolveConfigPathForDisplay() {
        return resolveConfigPath().toString();
    }

    private void applyUpdate(Map<String, Object> item, ModelStrategyUpdateDTO dto) {
        item.put("provider", dto.getProvider().trim());
        item.put("modelName", dto.getModelName().trim());
        item.put("baseUrl", dto.getBaseUrl().trim());
        item.put("accessMode", dto.getAccessMode().trim());
        item.put("enabled", Boolean.TRUE.equals(dto.getEnabled()));
        item.put("placeholder", Boolean.TRUE.equals(dto.getPlaceholder()));
        item.put("fallbackEnabled", dto.getFallbackEnabled() == null || Boolean.TRUE.equals(dto.getFallbackEnabled()));
        item.put("requestTimeoutSeconds", dto.getRequestTimeoutSeconds() == null ? 60 : dto.getRequestTimeoutSeconds());
        item.put("temperature", dto.getTemperature() == null ? 0.2D : dto.getTemperature());
        item.put("maxTokens", dto.getMaxTokens() == null ? 800 : dto.getMaxTokens());
        item.put("promptTemplateCode", normalize(dto.getPromptTemplateCode()));
        item.put("boundAgents", sanitizeList(dto.getBoundAgents()));
        item.put("remark", normalize(dto.getRemark()));
    }

    private ModelStrategyItemVO toStrategyItem(Map<String, Object> item) {
        ModelStrategyItemVO vo = new ModelStrategyItemVO();
        vo.setStrategyCode(normalize(item.get("strategyCode")));
        vo.setScenarioCode(normalize(item.get("scenarioCode")));
        vo.setProvider(normalize(item.get("provider")));
        vo.setModelName(normalize(item.get("modelName")));
        vo.setBaseUrl(normalize(item.get("baseUrl")));
        vo.setAccessMode(normalize(item.get("accessMode")));
        vo.setEnabled(readBoolean(item.get("enabled")));
        vo.setPlaceholder(readBoolean(item.get("placeholder")));
        vo.setFallbackEnabled(readBoolean(item.get("fallbackEnabled")));
        vo.setRequestTimeoutSeconds(readInteger(item.get("requestTimeoutSeconds")));
        vo.setTemperature(readDouble(item.get("temperature")));
        vo.setMaxTokens(readInteger(item.get("maxTokens")));
        vo.setPromptTemplateCode(normalize(item.get("promptTemplateCode")));
        vo.setBoundAgents(sanitizeList(castList(item.get("boundAgents"))));
        vo.setRemark(normalize(item.get("remark")));
        return vo;
    }

    private List<Map<String, Object>> readStrategies() {
        Path configPath = resolveConfigPath();
        if (!Files.exists(configPath)) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    Files.readString(configPath, StandardCharsets.UTF_8),
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
            Object strategies = root.get("strategies");
            if (!(strategies instanceof List<?> strategyList)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : strategyList) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(
                            rawItem,
                            new TypeReference<LinkedHashMap<String, Object>>() {}
                    )));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BizException("MODEL_STRATEGY_READ_FAILED", "读取模型策略配置失败");
        }
    }

    private Path resolveConfigPath() {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(modelStrategyConfigPath);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("ai-config").resolve("model-strategies.json").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("ai-config").resolve("model-strategies.json").normalize());

        Path current = userDir;
        while (current != null) {
            candidates.add(current.resolve("ai-config").resolve("model-strategies.json").normalize());
            candidates.add(current.resolve("quant-ai-platform").resolve("ai-config").resolve("model-strategies.json").normalize());
            current = current.getParent();
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return candidates.iterator().next();
    }

    private List<String> castList(Object value) {
        if (!(value instanceof List<?> items)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            String normalized = normalize(item);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> sanitizeList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String item : items) {
            String normalized = normalize(item);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> diffFields(Map<String, Object> before, Map<String, Object> after) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.addAll(before.keySet());
        fields.addAll(after.keySet());
        List<String> result = new ArrayList<>();
        for (String field : fields) {
            if (!Objects.equals(before.get(field), after.get(field))) {
                result.add(field);
            }
        }
        return result;
    }

    private Boolean readBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double readDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
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
}
