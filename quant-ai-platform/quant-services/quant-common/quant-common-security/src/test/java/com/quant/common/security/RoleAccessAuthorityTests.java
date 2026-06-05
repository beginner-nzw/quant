package com.quant.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleAccessAuthorityTests {

    @TempDir
    Path tempDir;

    @Test
    void activeBackendProfileGrantsMappedPermissionAndDeniesUnmappedPermission() {
        RoleAccessAuthority authority = new RoleAccessAuthority("missing-role-access-configs.json");
        UserProfile researcher = active("researcher", "RESEARCHER");
        UserProfile admin = active("admin", "ADMIN");

        assertTrue(authority.hasPermission(researcher, "TASK_CREATE"));
        assertFalse(authority.hasPermission(researcher, "MODEL_AGENT_CONFIG_EDIT"));
        assertTrue(authority.hasPermission(admin, "MODEL_AGENT_CONFIG_EDIT"));
    }

    @Test
    void disabledAndUnknownUsersNeverReceivePermission() {
        RoleAccessAuthority authority = new RoleAccessAuthority("missing-role-access-configs.json");
        UserProfile disabled = new UserProfile(
                "disabled_user",
                "Disabled User",
                UserProfileStatus.DISABLED,
                List.of("ADMIN")
        );

        assertFalse(authority.hasPermission(disabled, "TASK_CREATE"));
        assertFalse(authority.hasPermission(UserProfile.unknown("missing"), "TASK_CREATE"));
    }

    @Test
    void compatibleJsonCannotWidenProductionRoleBaseline() throws Exception {
        Path config = tempDir.resolve("role-access-configs.json");
        Files.writeString(config, """
                {
                  "roles": [
                    {
                      "roleCode": "RESEARCHER",
                      "menuKeys": ["TASK_LIST", "MODEL_AGENT_CONFIG"],
                      "permissionKeys": ["TASK_CREATE", "MODEL_AGENT_CONFIG_EDIT"]
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        RoleAccessAuthority authority = new RoleAccessAuthority(config.toString());

        assertTrue(authority.permissionAllowedForRole("RESEARCHER", "TASK_CREATE"));
        assertFalse(authority.permissionAllowedForRole("RESEARCHER", "MODEL_AGENT_CONFIG_EDIT"));
        assertFalse(authority.menuAllowedForRole("RESEARCHER", "MODEL_AGENT_CONFIG"));
    }

    private UserProfile active(String userId, String role) {
        return new UserProfile(userId, userId, UserProfileStatus.ACTIVE, List.of(role));
    }
}
