package com.quant.subscription.manager;

import com.quant.api.subscription.dto.NotificationPublishDTO;
import com.quant.subscription.delivery.NotificationDeliveryChannel;
import com.quant.subscription.domain.entity.UserNotification;
import com.quant.subscription.domain.entity.UserSubscription;
import com.quant.subscription.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class NotificationQueueManager {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryChannel deliveryChannel;

    public NotificationQueueManager(NotificationRepository notificationRepository,
                                    NotificationDeliveryChannel deliveryChannel) {
        this.notificationRepository = notificationRepository;
        this.deliveryChannel = deliveryChannel;
    }

    public List<UserNotification> publish(NotificationPublishDTO dto, List<UserSubscription> subscriptions) {
        return subscriptions.stream()
                .map(subscription -> enqueue(dto, subscription.getUserId()))
                .toList();
    }

    public List<UserNotification> listUserNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    private UserNotification enqueue(NotificationPublishDTO dto, String userId) {
        UserNotification notification = new UserNotification(
                "notice-" + UUID.randomUUID(),
                userId,
                normalize(dto.getNotificationType(), "MARKET_EVENT"),
                defaultIfBlank(dto.getTitle(), "subscription update"),
                defaultIfBlank(dto.getContent(), ""),
                Instant.now()
        );
        notificationRepository.save(notification);
        deliveryChannel.deliver(notification);
        return notification;
    }

    private String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized.toUpperCase();
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
