package com.quant.subscription.domain.entity;

public class UserSubscription {

    private final String subscriptionId;
    private final String userId;
    private final String targetType;
    private final String targetCode;
    private final String subscriptionType;

    public UserSubscription(String subscriptionId,
                            String userId,
                            String targetType,
                            String targetCode,
                            String subscriptionType) {
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.targetType = targetType;
        this.targetCode = targetCode;
        this.subscriptionType = subscriptionType;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }
}
