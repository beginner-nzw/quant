package com.quant.common.security;

import java.util.Optional;

public interface UserProfileSource {

    Optional<UserProfile> findByUserId(String userId);
}
