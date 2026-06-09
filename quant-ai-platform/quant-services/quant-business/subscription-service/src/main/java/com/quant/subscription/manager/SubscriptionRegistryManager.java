package com.quant.subscription.manager;

import com.quant.api.subscription.dto.SubscriptionCreateDTO;
import com.quant.subscription.domain.entity.UserSubscription;
import com.quant.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SubscriptionRegistryManager {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionRegistryManager(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public UserSubscription createSubscription(SubscriptionCreateDTO dto) {
        UserSubscription subscription = new UserSubscription(
                "sub-" + UUID.randomUUID(),
                requireText(dto == null ? null : dto.getUserId(), "userId is required"),
                normalize(dto.getTargetType(), "STOCK"),
                requireText(dto.getTargetCode(), "targetCode is required").toUpperCase(),
                normalize(dto.getSubscriptionType(), "MARKET_EVENT")
        );
        subscriptionRepository.save(subscription);
        return subscription;
    }

    public List<UserSubscription> listUserSubscriptions(String userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public List<UserSubscription> findMatchedSubscriptions(String targetType, String targetCode, String subscriptionType) {
        String normalizedTargetType = normalize(targetType, "STOCK");
        String normalizedTargetCode = requireText(targetCode, "targetCode is required").toUpperCase();
        String normalizedSubscriptionType = normalize(subscriptionType, "MARKET_EVENT");
        return subscriptionRepository.findMatched(normalizedTargetType, normalizedTargetCode, normalizedSubscriptionType);
    }

    private String requireText(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized.toUpperCase();
    }
}
