package com.quant.subscription.delivery;

import com.quant.subscription.domain.entity.UserNotification;

public interface NotificationDeliveryChannel {

    void deliver(UserNotification notification);
}
