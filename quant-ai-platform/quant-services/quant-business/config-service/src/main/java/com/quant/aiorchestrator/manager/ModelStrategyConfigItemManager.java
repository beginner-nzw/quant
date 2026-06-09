package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.ModelStrategyUpdateDTO;
import com.quant.aiorchestrator.domain.vo.ModelStrategyItemVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ModelStrategyConfigItemManager {

    public void applyUpdate(Map<String, Object> item, ModelStrategyUpdateDTO dto) {
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

    public ModelStrategyItemVO toStrategyItem(Map<String, Object> item) {
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

    public List<String> diffFields(Map<String, Object> before, Map<String, Object> after) {
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

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
