package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerRuleItemVO;
import com.quant.common.core.exception.BizException;
import com.quant.config.port.EventAutoTriggerConfigPort.EventAutoTriggerRule;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class EventAutoTriggerConfigPolicyManager {

    public void validateRuleUpdate(String ruleCode, EventAutoTriggerRuleUpdateDTO dto) {
        if (dto == null) {
            throw new BizException("EVENT_AUTO_TRIGGER_RULE_EMPTY", "event auto trigger rule cannot be empty");
        }
        if (!StringUtils.hasText(ruleCode)) {
            throw new BizException("EVENT_AUTO_TRIGGER_RULE_CODE_EMPTY", "event auto trigger rule code cannot be empty");
        }
        if (!StringUtils.hasText(dto.getRuleName())) {
            throw new BizException("EVENT_AUTO_TRIGGER_RULE_NAME_EMPTY", "event auto trigger rule name cannot be empty");
        }
        if (sanitizeList(dto.getEventTypes()).isEmpty()) {
            throw new BizException("EVENT_AUTO_TRIGGER_EVENT_TYPES_EMPTY", "event types cannot be empty");
        }
        if (sanitizeList(dto.getImpactLevels()).isEmpty()) {
            throw new BizException("EVENT_AUTO_TRIGGER_IMPACT_LEVELS_EMPTY", "impact levels cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTaskType())) {
            throw new BizException("EVENT_AUTO_TRIGGER_TASK_TYPE_EMPTY", "task type cannot be empty");
        }
        if (!StringUtils.hasText(dto.getAnalysisScope())) {
            throw new BizException("EVENT_AUTO_TRIGGER_ANALYSIS_SCOPE_EMPTY", "analysis scope cannot be empty");
        }
        if (!StringUtils.hasText(dto.getPriority())) {
            throw new BizException("EVENT_AUTO_TRIGGER_PRIORITY_EMPTY", "priority cannot be empty");
        }
        if (!StringUtils.hasText(dto.getSourceChannel())) {
            throw new BizException("EVENT_AUTO_TRIGGER_SOURCE_CHANNEL_EMPTY", "source channel cannot be empty");
        }
        if (!StringUtils.hasText(dto.getTitleTemplate())) {
            throw new BizException("EVENT_AUTO_TRIGGER_TITLE_TEMPLATE_EMPTY", "title template cannot be empty");
        }
    }

    public Map<String, Object> applyRuleUpdate(Map<String, Object> item, EventAutoTriggerRuleUpdateDTO dto) {
        Map<String, Object> before = new LinkedHashMap<>(item);
        item.put("ruleName", dto.getRuleName().trim());
        item.put("enabled", dto.getEnabled() == null || Boolean.TRUE.equals(dto.getEnabled()));
        item.put("eventTypes", sanitizeList(dto.getEventTypes()));
        item.put("impactLevels", sanitizeList(dto.getImpactLevels()));
        item.put("taskType", dto.getTaskType().trim());
        item.put("analysisScope", dto.getAnalysisScope().trim());
        item.put("priority", dto.getPriority().trim());
        item.put("sourceChannel", dto.getSourceChannel().trim());
        item.put("titleTemplate", dto.getTitleTemplate().trim());
        item.put("remark", normalize(dto.getRemark()));
        return before;
    }

    public EventAutoTriggerRule toRule(Map<String, Object> item) {
        EventAutoTriggerRule rule = new EventAutoTriggerRule();
        rule.setRuleCode(normalize(item.get("ruleCode")));
        rule.setRuleName(normalize(item.get("ruleName")));
        rule.setEnabled(readBoolean(item.get("enabled"), true));
        rule.setEventTypes(sanitizeList(castList(item.get("eventTypes"))));
        rule.setImpactLevels(sanitizeList(castList(item.get("impactLevels"))));
        rule.setTaskType(normalize(item.get("taskType")));
        rule.setAnalysisScope(normalize(item.get("analysisScope")));
        rule.setPriority(normalize(item.get("priority")));
        rule.setSourceChannel(normalize(item.get("sourceChannel")));
        rule.setTitleTemplate(normalize(item.get("titleTemplate")));
        rule.setRemark(normalize(item.get("remark")));
        return rule;
    }

    public EventAutoTriggerRuleItemVO toRuleItem(EventAutoTriggerRule rule) {
        EventAutoTriggerRuleItemVO vo = new EventAutoTriggerRuleItemVO();
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setEnabled(rule.getEnabled());
        vo.setEventTypes(rule.getEventTypes());
        vo.setImpactLevels(rule.getImpactLevels());
        vo.setTaskType(rule.getTaskType());
        vo.setAnalysisScope(rule.getAnalysisScope());
        vo.setPriority(rule.getPriority());
        vo.setSourceChannel(rule.getSourceChannel());
        vo.setTitleTemplate(rule.getTitleTemplate());
        vo.setRemark(rule.getRemark());
        return vo;
    }

    public boolean matches(List<String> expectedValues, String actualValue) {
        if (expectedValues == null || expectedValues.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(actualValue)) {
            return false;
        }
        return expectedValues.stream().anyMatch(item -> actualValue.equalsIgnoreCase(item));
    }

    public boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim());
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

    public String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
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

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return new ArrayList<>(result);
    }
}
