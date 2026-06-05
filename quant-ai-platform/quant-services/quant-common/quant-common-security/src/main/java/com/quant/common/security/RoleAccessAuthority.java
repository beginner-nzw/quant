package com.quant.common.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RoleAccessAuthority {

    private static final Map<String, RoleAccessDefinition> BASELINE = baselineDefinitions();

    public RoleAccessAuthority(String roleAccessConfigPath) {
        // Kept for constructor compatibility. Governed role config is read by service owners.
    }

    public boolean hasPermission(UserProfile profile, String permissionKey) {
        String normalizedPermission = normalizeCode(permissionKey);
        if (profile == null || !profile.active() || normalizedPermission == null) {
            return false;
        }
        for (String role : profile.roles()) {
            RoleAccessDefinition definition = roleDefinition(role).orElse(null);
            if (definition != null && definition.permissionKeys().contains(normalizedPermission)) {
                return true;
            }
        }
        return false;
    }

    public List<String> permissionsForRole(String roleCode) {
        return roleDefinition(roleCode).map(RoleAccessDefinition::permissionKeys).orElse(List.of());
    }

    public List<String> menusForRole(String roleCode) {
        return roleDefinition(roleCode).map(RoleAccessDefinition::menuKeys).orElse(List.of());
    }

    public boolean roleExists(String roleCode) {
        return BASELINE.containsKey(normalizeCode(roleCode));
    }

    public boolean permissionAllowedForRole(String roleCode, String permissionKey) {
        return permissionsForRole(roleCode).contains(normalizeCode(permissionKey));
    }

    public boolean menuAllowedForRole(String roleCode, String menuKey) {
        return menusForRole(roleCode).contains(normalizeCode(menuKey));
    }

    private Optional<RoleAccessDefinition> roleDefinition(String roleCode) {
        String normalizedRole = normalizeCode(roleCode);
        if (normalizedRole == null) {
            return Optional.empty();
        }
        RoleAccessDefinition baseline = BASELINE.get(normalizedRole);
        if (baseline == null) {
            return Optional.empty();
        }
        return Optional.of(baseline);
    }

    private List<String> intersect(List<String> candidate, List<String> allowed) {
        Set<String> allowedSet = new LinkedHashSet<>(allowed);
        List<String> result = new ArrayList<>();
        for (String value : candidate) {
            String normalized = normalizeCode(value);
            if (normalized != null && allowedSet.contains(normalized) && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> normalizeCodes(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            String normalized = normalizeCode(item);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static String normalizeCode(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static Map<String, RoleAccessDefinition> baselineDefinitions() {
        LinkedHashMap<String, RoleAccessDefinition> result = new LinkedHashMap<>();
        result.put("RESEARCHER", new RoleAccessDefinition("RESEARCHER",
                List.of("TASK_LIST", "TASK_CREATE", "MARKET_EVENTS", "MARKET_INTELLIGENCE",
                        "RESEARCH_WORKBENCH", "STRATEGY_SIGNALS", "RISK_WARNINGS", "RESEARCH_REPORTS"),
                List.of("TASK_VIEW", "TASK_CREATE")));
        result.put("PM", new RoleAccessDefinition("PM",
                List.of("TASK_LIST", "TASK_CREATE", "MARKET_EVENTS", "MARKET_INTELLIGENCE",
                        "RESEARCH_WORKBENCH", "STRATEGY_SIGNALS", "RISK_WARNINGS", "RESEARCH_REPORTS"),
                List.of("TASK_VIEW", "TASK_CREATE")));
        result.put("RISK_MANAGER", new RoleAccessDefinition("RISK_MANAGER",
                List.of("TASK_LIST", "TASK_CREATE", "MARKET_EVENTS", "MARKET_INTELLIGENCE",
                        "RESEARCH_WORKBENCH", "STRATEGY_SIGNALS", "RISK_WARNINGS", "RESEARCH_REPORTS",
                        "AUDIT_COMPLIANCE"),
                List.of("TASK_VIEW", "TASK_CREATE", "AUDIT_COMPLIANCE_VIEW")));
        result.put("COMPLIANCE_AUDITOR", new RoleAccessDefinition("COMPLIANCE_AUDITOR",
                List.of("TASK_LIST", "TASK_CREATE", "MARKET_EVENTS", "MARKET_INTELLIGENCE",
                        "RESEARCH_WORKBENCH", "STRATEGY_SIGNALS", "RISK_WARNINGS", "RESEARCH_REPORTS",
                        "AUDIT_COMPLIANCE", "MODEL_AGENT_CONFIG", "REPORTS_PENDING",
                        "REPORTS_APPROVED", "REPORTS_REJECTED"),
                List.of("TASK_VIEW", "TASK_CREATE", "AUDIT_COMPLIANCE_VIEW",
                        "REPORT_REVIEW", "MODEL_AGENT_CONFIG_VIEW")));
        result.put("ADMIN", new RoleAccessDefinition("ADMIN",
                List.of("TASK_LIST", "TASK_CREATE", "MARKET_EVENTS", "MARKET_INTELLIGENCE",
                        "RESEARCH_WORKBENCH", "STRATEGY_SIGNALS", "RISK_WARNINGS", "RESEARCH_REPORTS",
                        "AUDIT_COMPLIANCE", "MODEL_AGENT_CONFIG", "REPORTS_PENDING",
                        "REPORTS_APPROVED", "REPORTS_REJECTED"),
                List.of("TASK_VIEW", "TASK_CREATE", "TASK_RETRY", "TASK_CANCEL",
                        "AUDIT_COMPLIANCE_VIEW", "REPORT_REVIEW", "MODEL_AGENT_CONFIG_VIEW",
                        "MODEL_AGENT_CONFIG_EDIT")));
        return Map.copyOf(result);
    }
}
