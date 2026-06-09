package com.quant.subscription.repository;

import com.quant.subscription.domain.entity.UserSubscription;

import java.util.List;

public interface SubscriptionRepository {

    void save(UserSubscription subscription);

    List<UserSubscription> findByUserId(String userId);

    List<UserSubscription> findMatched(String targetType, String targetCode, String subscriptionType);
}
