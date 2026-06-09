package com.quant.user.repository;

import com.quant.user.domain.entity.UserAccount;

import java.util.Optional;
import java.util.Set;

public interface UserAccountRepository {

    boolean existsByUsername(String username);

    void save(UserAccount account);

    Optional<UserAccount> findByUserId(String userId);

    void grantRoles(String userId, Set<String> roles);
}
