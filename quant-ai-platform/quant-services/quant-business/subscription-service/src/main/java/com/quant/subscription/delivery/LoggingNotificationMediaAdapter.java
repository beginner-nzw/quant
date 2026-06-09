package com.quant.subscription.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationMediaAdapter implements NotificationMediaAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationMediaAdapter.class);

    @Override
    public void dispatch(NotificationDispatchMessage message) {
        if (message == null) {
            return;
        }
        log.info("notification media dispatch notificationId={} userId={} type={}",
                message.getNotificationId(),
                message.getUserId(),
                message.getNotificationType());
    }
}
