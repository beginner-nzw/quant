package com.quant.subscription.domain.entity;

import java.time.Instant;

public class UserNotification {

    private final String notificationId;
    private final String userId;
    private final String notificationType;
    private final String title;
    private final String content;
    private final Instant createdAt;

    public UserNotification(String notificationId,
                            String userId,
                            String notificationType,
                            String title,
                            String content,
                            Instant createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
