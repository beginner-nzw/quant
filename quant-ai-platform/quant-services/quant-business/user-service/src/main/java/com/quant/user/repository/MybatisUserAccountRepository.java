package com.quant.user.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.user.domain.entity.SysUserDO;
import com.quant.user.domain.entity.SysUserRoleDO;
import com.quant.user.domain.entity.UserAccount;
import com.quant.user.mapper.SysUserMapper;
import com.quant.user.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class MybatisUserAccountRepository implements UserAccountRepository {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public MybatisUserAccountRepository(SysUserMapper sysUserMapper, SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    public boolean existsByUsername(String username) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, normalizeUsername(username))
                .eq(SysUserDO::getDeleted, 0)) > 0;
    }

    @Override
    public void save(UserAccount account) {
        LocalDateTime now = LocalDateTime.now();
        SysUserDO user = new SysUserDO();
        user.setUserId(account.getUserId());
        user.setUsername(normalizeUsername(account.getUsername()));
        user.setDisplayName(account.getDisplayName());
        user.setStatus("ENABLED");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        sysUserMapper.insert(user);
        grantRoles(account.getUserId(), account.getRoles());
    }

    @Override
    public Optional<UserAccount> findByUserId(String userId) {
        SysUserDO user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUserId, userId)
                .eq(SysUserDO::getDeleted, 0)
                .last("limit 1"));
        if (user == null) {
            return Optional.empty();
        }
        Set<String> roles = new LinkedHashSet<>();
        for (SysUserRoleDO role : findRoleRows(userId)) {
            roles.add(role.getRoleCode());
        }
        return Optional.of(new UserAccount(user.getUserId(), user.getUsername(), user.getDisplayName(), roles));
    }

    @Override
    public void grantRoles(String userId, Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        Set<String> existingRoles = new LinkedHashSet<>();
        for (SysUserRoleDO role : findRoleRows(userId)) {
            existingRoles.add(role.getRoleCode());
        }
        LocalDateTime now = LocalDateTime.now();
        for (String role : roles) {
            String normalizedRole = normalizeRole(role);
            if (normalizedRole.isEmpty() || existingRoles.contains(normalizedRole)) {
                continue;
            }
            SysUserRoleDO userRole = new SysUserRoleDO();
            userRole.setUserId(userId);
            userRole.setRoleCode(normalizedRole);
            userRole.setCreatedAt(now);
            userRole.setDeleted(0);
            sysUserRoleMapper.insert(userRole);
        }
    }

    private List<SysUserRoleDO> findRoleRows(String userId) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleDO>()
                .eq(SysUserRoleDO::getUserId, userId)
                .eq(SysUserRoleDO::getDeleted, 0));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase();
    }
}
