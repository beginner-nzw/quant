package com.quant.subscription.repository;

import com.quant.subscription.domain.entity.UserSubscription;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnMissingBean(SubscriptionRepository.class)
public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final List<UserSubscription> subscriptions = new CopyOnWriteArrayList<>();

    @Override
    public void save(UserSubscription subscription) {
        subscriptions.add(subscription);
    }

    @Override
    public List<UserSubscription> findByUserId(String userId) {
        return subscriptions.stream()
                .filter(subscription -> subscription.getUserId().equals(userId))
                .toList();
    }

    @Override
    public List<UserSubscription> findMatched(String targetType, String targetCode, String subscriptionType) {
        return subscriptions.stream()
                .filter(subscription -> subscription.getTargetType().equals(targetType))
                .filter(subscription -> subscription.getTargetCode().equals(targetCode))
                .filter(subscription -> subscription.getSubscriptionType().equals(subscriptionType))
                .toList();
    }
}
