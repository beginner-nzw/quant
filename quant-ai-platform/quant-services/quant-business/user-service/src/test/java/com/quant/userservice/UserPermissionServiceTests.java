package com.quant.userservice;

import com.quant.api.user.dto.UserCreateDTO;
import com.quant.api.user.dto.UserRoleGrantDTO;
import com.quant.api.user.vo.UserProfileVO;
import com.quant.common.security.UserContext;
import com.quant.common.security.UserProfile;
import com.quant.common.security.UserProfileStatus;
import com.quant.user.manager.CurrentUserManager;
import com.quant.user.manager.UserAccountManager;
import com.quant.user.manager.UserPermissionManager;
import com.quant.user.repository.InMemoryUserAccountRepository;
import com.quant.user.security.UserServiceProfileSource;
import com.quant.user.service.impl.UserPermissionServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPermissionServiceTests {

    @Test
    void createsUserAndResolvesRolePermissions() {
        UserPermissionServiceImpl service = new UserPermissionServiceImpl(
                new UserAccountManager(new InMemoryUserAccountRepository()),
                new UserPermissionManager()
        );
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("analyst01");
        dto.setDisplayName("Analyst");
        dto.setRoles(Set.of("researcher"));

        UserProfileVO created = service.createUser(dto);

        assertTrue(created.getRoles().contains("RESEARCHER"));
        assertTrue(created.getPermissions().contains("TASK_CREATE"));

        UserRoleGrantDTO grant = new UserRoleGrantDTO();
        grant.setRoles(Set.of("risk_manager"));
        UserProfileVO updated = service.grantRoles(created.getUserId(), grant);

        assertTrue(updated.getPermissions().contains("RISK_REVIEW"));
    }

    @Test
    void exposesCurrentUserFromSharedSecurityContext() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        UserAccountManager accountManager = new UserAccountManager(repository);
        UserPermissionManager permissionManager = new UserPermissionManager();
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("admin01");
        dto.setDisplayName("Admin");
        dto.setRoles(Set.of("admin"));
        UserProfileVO created = new UserPermissionServiceImpl(accountManager, permissionManager).createUser(dto);
        UserContext.set(created.getUserId(), "ADMIN",
                new UserProfile(created.getUserId(), created.getDisplayName(), UserProfileStatus.ACTIVE, List.of("ADMIN")));
        try {
            UserProfileVO current = new CurrentUserManager(accountManager, permissionManager).currentUserProfile();

            assertEquals(created.getUserId(), current.getUserId());
            assertTrue(current.getPermissions().contains("USER_MANAGE"));
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void userServiceProfileSourceBacksSharedSecurityFilter() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        UserAccountManager accountManager = new UserAccountManager(repository);
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("reviewer01");
        dto.setDisplayName("Reviewer");
        dto.setRoles(Set.of("compliance"));
        UserProfileVO created = new UserPermissionServiceImpl(accountManager, new UserPermissionManager()).createUser(dto);

        UserProfile profile = new UserServiceProfileSource(accountManager).findByUserId(created.getUserId()).orElseThrow();

        assertEquals(created.getUserId(), profile.userId());
        assertEquals(UserProfileStatus.ACTIVE, profile.status());
        assertTrue(profile.roles().contains("COMPLIANCE"));
    }
}
