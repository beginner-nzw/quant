package com.quant.user.manager;

import com.quant.api.user.dto.UserCreateDTO;
import com.quant.user.domain.entity.UserAccount;
import com.quant.user.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserAccountManager {

    private final UserAccountRepository userAccountRepository;

    public UserAccountManager(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount createUser(UserCreateDTO dto) {
        String username = requireText(dto == null ? null : dto.getUsername(), "username is required");
        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username already exists");
        }
        String userId = "user-" + UUID.randomUUID();
        UserAccount account = new UserAccount(
                userId,
                username,
                defaultIfBlank(dto.getDisplayName(), username),
                dto.getRoles()
        );
        userAccountRepository.save(account);
        return account;
    }

    public Optional<UserAccount> findUser(String userId) {
        return userAccountRepository.findByUserId(userId);
    }

    public UserAccount requireUser(String userId) {
        return findUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    private String requireText(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
