package com.quant.subscription.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotificationDispatchConsumer {

    private static final String CONSUMER_GROUP = "subscription-service-notification-dispatch-group";

    private final ObjectMapper objectMapper;
    private final NotificationMediaAdapter mediaAdapter;

    public NotificationDispatchConsumer(ObjectMapper objectMapper,
                                        NotificationMediaAdapter mediaAdapter) {
        this.objectMapper = objectMapper;
        this.mediaAdapter = mediaAdapter;
    }

    @KafkaListener(topics = KafkaTopicConstants.NOTIFICATION_DISPATCH, groupId = CONSUMER_GROUP)
    public void consume(String message) {
        mediaAdapter.dispatch(parse(message));
    }

    NotificationDispatchMessage parse(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            NotificationDispatchMessage parsed = new NotificationDispatchMessage();
            parsed.setNotificationId(text(root, "notificationId"));
            parsed.setUserId(text(root, "userId"));
            parsed.setNotificationType(text(root, "notificationType"));
            parsed.setTitle(text(root, "title"));
            parsed.setContent(text(root, "content"));
            parsed.setCreatedAt(instant(text(root, "createdAt")));
            return parsed;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid notification dispatch message", ex);
        }
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode value = root == null ? null : root.get(fieldName);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private Instant instant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.EPOCH;
        }
        return Instant.parse(value);
    }
}
