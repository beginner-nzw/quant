package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.configstore.ConfigStoreKey;
import com.quant.aiorchestrator.configstore.GovernedConfigStore;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.RoleAccessAuthority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoleAccessConfigStoreManager {

    private static final String ROLE_RESEARCHER = "RESEARCHER";
    private static final String ROLE_PM = "PM";
    private static final String ROLE_RISK_MANAGER = "RISK_MANAGER";
    private static final String ROLE_COMPLIANCE_AUDITOR = "COMPLIANCE_AUDITOR";
    private static final String ROLE_ADMIN = "ADMIN";

    private final ObjectMapper objectMapper;
    private final RoleAccessAuthority roleAccessAuthority;
    private final GovernedConfigStore governedConfigStore;

    public List<Map<String, Object>> readRoles() {
        try {
            Map<String, Object> emptyRoot = new LinkedHashMap<>();
            emptyRoot.put("roles", List.of());
            Map<String, Object> root = governedConfigStore.readRoot(ConfigStoreKey.ROLE_ACCESS, emptyRoot);
            Object roles = root.get("roles");
            if (!(roles instanceof List<?> roleList)) {
                return baselineRoles();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : roleList) {
                if (item instanceof Map<?, ?> rawItem) {
                    result.add(new LinkedHashMap<>(objectMapper.convertValue(
                            rawItem,
                            new TypeReference<LinkedHashMap<String, Object>>() {}
                    )));
                }
            }
            if (result.isEmpty()) {
                return baselineRoles();
            }
            return result;
        } catch (Exception e) {
            throw new BizException("ROLE_ACCESS_READ_FAILED", "Failed to read governed role access config");
        }
    }

    public void writeRoles(String roleCode,
                           String roleName,
                           List<String> changedFields,
                           List<Map<String, Object>> roles) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("roles", roles);
        governedConfigStore.writeRoot(
                ConfigStoreKey.ROLE_ACCESS,
                root,
                roleCode,
                roleName,
                "UPDATE",
                "Update role access config",
                changedFields
        );
    }

    public String displayPath() {
        return governedConfigStore.displayPath(ConfigStoreKey.ROLE_ACCESS);
    }

    private List<Map<String, Object>> baselineRoles() {
        return List.of(
                baselineRole(ROLE_RESEARCHER),
                baselineRole(ROLE_PM),
                baselineRole(ROLE_RISK_MANAGER),
                baselineRole(ROLE_COMPLIANCE_AUDITOR),
                baselineRole(ROLE_ADMIN)
        );
    }

    private Map<String, Object> baselineRole(String roleCode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("roleCode", roleCode);
        item.put("roleName", roleCode);
        item.put("roleDescription", "Baseline role access");
        item.put("menuKeys", roleAccessAuthority.menusForRole(roleCode));
        item.put("permissionKeys", roleAccessAuthority.permissionsForRole(roleCode));
        item.put("remark", "Generated from production baseline when governed role access config is empty.");
        return item;
    }
}
