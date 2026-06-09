package com.quant.subscription.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.subscription.domain.entity.UserNotification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "quant.notification.delivery", name = "channel", havingValue = "kafka")
public class KafkaNotificationDeliveryChannel implements NotificationDeliveryChannel {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaNotificationDeliveryChannel(KafkaTemplate<String, String> kafkaTemplate,
                                            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void deliver(UserNotification notification) {
        if (notification == null) {
            return;
        }
        kafkaTemplate.send(
                KafkaTopicConstants.NOTIFICATION_DISPATCH,
                notification.getUserId(),
                toJson(notification)
        );
    }

    private String toJson(UserNotification notification) {
        try {
            return objectMapper.writeValueAsString(toPayload(notification));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("notification dispatch serialization failed", ex);
        }
    }

    private Map<String, String> toPayload(UserNotification notification) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("notificationId", notification.getNotificationId());
        payload.put("userId", notification.getUserId());
        payload.put("notificationType", notification.getNotificationType());
        payload.put("title", notification.getTitle());
        payload.put("content", notification.getContent());
        payload.put("createdAt", notification.getCreatedAt() == null ? "" : notification.getCreatedAt().toString());
        return payload;
    }
}
