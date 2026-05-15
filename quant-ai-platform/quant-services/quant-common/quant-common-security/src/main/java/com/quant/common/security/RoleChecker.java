package com.quant.common.security;

import com.quant.common.core.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RoleChecker {

    public void requireAny(String... roles) {
        String currentRole = UserContext.getUserRole();
        boolean matched = Arrays.stream(roles)
                .map(UserRoleEnum::from)
                .anyMatch(role -> role.matches(currentRole));
        if (!matched) {
            throw new BizException("FORBIDDEN", "当前用户无权限执行该操作");
        }
    }

    public void requireAny(UserRoleEnum... roles) {
        String currentRole = UserContext.getUserRole();
        boolean matched = Arrays.stream(roles)
                .anyMatch(role -> role.matches(currentRole));
        if (!matched) {
            throw new BizException("FORBIDDEN", "当前用户无权限执行该操作");
        }
    }
}
