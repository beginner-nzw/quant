package com.quant.subscriptionservice;

import com.quant.api.subscription.dto.NotificationPublishDTO;
import com.quant.api.subscription.dto.SubscriptionCreateDTO;
import com.quant.api.subscription.vo.NotificationVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.subscription.delivery.KafkaNotificationDeliveryChannel;
import com.quant.subscription.delivery.NotificationDispatchConsumer;
import com.quant.subscription.delivery.NotificationDispatchMessage;
import com.quant.subscription.delivery.NotificationDeliveryChannel;
import com.quant.subscription.delivery.NotificationMediaAdapter;
import com.quant.subscription.domain.entity.UserNotification;
import com.quant.subscription.manager.NotificationQueueManager;
import com.quant.subscription.manager.SubscriptionRegistryManager;
import com.quant.subscription.repository.InMemoryNotificationRepository;
import com.quant.subscription.repository.InMemorySubscriptionRepository;
import com.quant.subscription.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SubscriptionServiceTests {

    @Test
    void publishesNotificationToMatchingSubscribers() {
        AtomicInteger deliveries = new AtomicInteger();
        NotificationDeliveryChannel deliveryChannel = notification -> deliveries.incrementAndGet();
        SubscriptionServiceImpl service = new SubscriptionServiceImpl(
                new SubscriptionRegistryManager(new InMemorySubscriptionRepository()),
                new NotificationQueueManager(new InMemoryNotificationRepository(), deliveryChannel)
        );
        SubscriptionCreateDTO subscription = new SubscriptionCreateDTO();
        subscription.setUserId("user-1");
        subscription.setTargetType("stock");
        subscription.setTargetCode("600519");
        subscription.setSubscriptionType("risk_alert");
        service.createSubscription(subscription);

        NotificationPublishDTO event = new NotificationPublishDTO();
        event.setTargetType("STOCK");
        event.setTargetCode("600519");
        event.setNotificationType("RISK_ALERT");
        event.setTitle("Risk warning");
        event.setContent("High impact event");

        List<NotificationVO> published = service.publishNotification(event);

        assertEquals(1, published.size());
        assertEquals(1, service.listUserNotifications("user-1").size());
        assertEquals(1, deliveries.get());
    }

    @Test
    void kafkaDeliveryPublishesToNotificationDispatchTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaNotificationDeliveryChannel deliveryChannel = new KafkaNotificationDeliveryChannel(
                kafkaTemplate,
                new ObjectMapper()
        );
        UserNotification notification = new UserNotification(
                "notice-1",
                "user-1",
                "RISK_ALERT",
                "Risk warning",
                "High impact event",
                Instant.parse("2026-06-09T00:00:00Z")
        );

        deliveryChannel.deliver(notification);

        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq(KafkaTopicConstants.NOTIFICATION_DISPATCH),
                org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.contains("\"notificationId\":\"notice-1\"")
        );
    }

    @Test
    void notificationDispatchConsumerHandsMessageToMediaAdapter() {
        List<NotificationDispatchMessage> dispatched = new ArrayList<>();
        NotificationMediaAdapter mediaAdapter = dispatched::add;
        NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(new ObjectMapper(), mediaAdapter);

        consumer.consume("""
                {
                  "notificationId": "notice-1",
                  "userId": "user-1",
                  "notificationType": "RISK_ALERT",
                  "title": "Risk warning",
                  "content": "High impact event",
                  "createdAt": "2026-06-09T00:00:00Z"
                }
                """);

        assertEquals(1, dispatched.size());
        assertEquals("notice-1", dispatched.get(0).getNotificationId());
        assertEquals("user-1", dispatched.get(0).getUserId());
        assertEquals(Instant.parse("2026-06-09T00:00:00Z"), dispatched.get(0).getCreatedAt());
    }
}
