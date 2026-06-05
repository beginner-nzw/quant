package com.quant.common.security;

import com.quant.common.core.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RoleChecker {

    public void requireAny(String... roles) {
        boolean matched = Arrays.stream(roles)
                .map(UserRoleEnum::from)
                .anyMatch(expectedRole -> UserContext.getRoles().stream().anyMatch(expectedRole::matches));
        if (!matched) {
            throw new BizException("FORBIDDEN", "当前用户无权限执行该操作");
        }
    }

    public void requireAny(UserRoleEnum... roles) {
        boolean matched = Arrays.stream(roles)
                .anyMatch(expectedRole -> UserContext.getRoles().stream().anyMatch(expectedRole::matches));
        if (!matched) {
            throw new BizException("FORBIDDEN", "当前用户无权限执行该操作");
        }
    }
}
