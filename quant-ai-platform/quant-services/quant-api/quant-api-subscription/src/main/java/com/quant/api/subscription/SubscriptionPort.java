package com.quant.api.subscription;

import com.quant.api.subscription.dto.NotificationPublishDTO;
import com.quant.api.subscription.dto.SubscriptionCreateDTO;
import com.quant.api.subscription.vo.NotificationVO;
import com.quant.api.subscription.vo.SubscriptionVO;

import java.util.List;

public interface SubscriptionPort {

    SubscriptionVO createSubscription(SubscriptionCreateDTO dto);

    List<SubscriptionVO> listUserSubscriptions(String userId);

    List<NotificationVO> publishNotification(NotificationPublishDTO dto);

    List<NotificationVO> listUserNotifications(String userId);
}
