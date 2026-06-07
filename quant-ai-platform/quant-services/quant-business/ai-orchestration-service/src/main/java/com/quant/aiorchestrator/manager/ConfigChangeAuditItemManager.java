package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.vo.ConfigChangeAuditItemVO;
import com.quant.common.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ConfigChangeAuditItemManager {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String, Object> buildAuditItem(String configType,
                                              String targetCode,
                                              String targetName,
                                              String operation,
                                              String configPath,
                                              String changeSummary,
                                              List<String> changedFields) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("auditId", UUID.randomUUID().toString());
        item.put("configType", normalize(configType));
        item.put("targetCode", normalize(targetCode));
        item.put("targetName", normalize(targetName));
        item.put("operation", normalize(operation));
        item.put("operatorId", normalize(SecurityUtils.currentUserId()) == null ? "unknown" : normalize(SecurityUtils.currentUserId()));
        item.put("operatorRole", normalize(SecurityUtils.currentUserRole()) == null ? "UNKNOWN" : normalize(SecurityUtils.currentUserRole()));
        item.put("configPath", normalize(configPath));
        item.put("changeSummary", normalize(changeSummary));
        item.put("changedFields", sanitizeList(changedFields));
        item.put("createdAt", DATE_TIME_FORMATTER.format(LocalDateTime.now()));
        return item;
    }

    public ConfigChangeAuditItemVO toAuditItemVO(Map<String, Object> item) {
        ConfigChangeAuditItemVO vo = new ConfigChangeAuditItemVO();
        vo.setAuditId(normalize(item.get("auditId")));
        vo.setConfigType(normalize(item.get("configType")));
        vo.setTargetCode(normalize(item.get("targetCode")));
        vo.setTargetName(normalize(item.get("targetName")));
        vo.setOperation(normalize(item.get("operation")));
        vo.setOperatorId(normalize(item.get("operatorId")));
        vo.setOperatorRole(normalize(item.get("operatorRole")));
        vo.setConfigPath(normalize(item.get("configPath")));
        vo.setChangeSummary(normalize(item.get("changeSummary")));
        vo.setChangedFields(sanitizeList(castList(item.get("changedFields"))));
        vo.setCreatedAt(normalize(item.get("createdAt")));
        return vo;
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
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
