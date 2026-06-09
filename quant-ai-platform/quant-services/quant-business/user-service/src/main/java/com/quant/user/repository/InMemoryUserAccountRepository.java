package com.quant.user.repository;

import com.quant.user.domain.entity.UserAccount;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnMissingBean(UserAccountRepository.class)
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, UserAccount> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> userIdByUsername = new ConcurrentHashMap<>();

    @Override
    public boolean existsByUsername(String username) {
        return userIdByUsername.containsKey(normalizeUsername(username));
    }

    @Override
    public void save(UserAccount account) {
        usersById.put(account.getUserId(), account);
        userIdByUsername.put(normalizeUsername(account.getUsername()), account.getUserId());
    }

    @Override
    public Optional<UserAccount> findByUserId(String userId) {
        return Optional.ofNullable(usersById.get(userId));
    }

    @Override
    public void grantRoles(String userId, Set<String> roles) {
        findByUserId(userId).ifPresent(account -> account.grantRoles(roles));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
