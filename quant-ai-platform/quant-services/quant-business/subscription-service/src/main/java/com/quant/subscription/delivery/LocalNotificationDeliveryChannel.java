package com.quant.subscription.delivery;

import com.quant.subscription.domain.entity.UserNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "quant.notification.delivery", name = "channel", havingValue = "local", matchIfMissing = true)
public class LocalNotificationDeliveryChannel implements NotificationDeliveryChannel {

    private static final Logger log = LoggerFactory.getLogger(LocalNotificationDeliveryChannel.class);

    @Override
    public void deliver(UserNotification notification) {
        if (notification == null) {
            return;
        }
        log.info("notification queued notificationId={} userId={} type={}",
                notification.getNotificationId(),
                notification.getUserId(),
                notification.getNotificationType());
    }
}
