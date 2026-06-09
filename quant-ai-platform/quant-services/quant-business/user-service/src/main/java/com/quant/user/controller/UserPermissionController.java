package com.quant.user.controller;

import com.quant.api.user.UserPermissionPort;
import com.quant.api.user.dto.UserCreateDTO;
import com.quant.api.user.dto.UserRoleGrantDTO;
import com.quant.api.user.vo.UserProfileVO;
import com.quant.user.manager.CurrentUserManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserPermissionController {

    private final UserPermissionPort userPermissionService;
    private final CurrentUserManager currentUserManager;

    public UserPermissionController(UserPermissionPort userPermissionService,
                                    CurrentUserManager currentUserManager) {
        this.userPermissionService = userPermissionService;
        this.currentUserManager = currentUserManager;
    }

    @PostMapping
    public UserProfileVO createUser(@RequestBody UserCreateDTO dto) {
        return userPermissionService.createUser(dto);
    }

    @PostMapping("/{userId}/roles")
    public UserProfileVO grantRoles(@PathVariable String userId, @RequestBody UserRoleGrantDTO dto) {
        return userPermissionService.grantRoles(userId, dto);
    }

    @GetMapping("/{userId}")
    public UserProfileVO getUserProfile(@PathVariable String userId) {
        return userPermissionService.getUserProfile(userId);
    }

    @GetMapping("/me")
    public UserProfileVO currentUser() {
        return currentUserManager.currentUserProfile();
    }
}
