package com.quant.subscription.service.impl;

import com.quant.api.subscription.SubscriptionPort;
import com.quant.api.subscription.dto.NotificationPublishDTO;
import com.quant.api.subscription.dto.SubscriptionCreateDTO;
import com.quant.api.subscription.vo.NotificationVO;
import com.quant.api.subscription.vo.SubscriptionVO;
import com.quant.subscription.domain.entity.UserNotification;
import com.quant.subscription.domain.entity.UserSubscription;
import com.quant.subscription.manager.NotificationQueueManager;
import com.quant.subscription.manager.SubscriptionRegistryManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionServiceImpl implements SubscriptionPort {

    private final SubscriptionRegistryManager subscriptionRegistryManager;
    private final NotificationQueueManager notificationQueueManager;

    public SubscriptionServiceImpl(SubscriptionRegistryManager subscriptionRegistryManager,
                                   NotificationQueueManager notificationQueueManager) {
        this.subscriptionRegistryManager = subscriptionRegistryManager;
        this.notificationQueueManager = notificationQueueManager;
    }

    @Override
    public SubscriptionVO createSubscription(SubscriptionCreateDTO dto) {
        return toSubscriptionVO(subscriptionRegistryManager.createSubscription(dto));
    }

    @Override
    public List<SubscriptionVO> listUserSubscriptions(String userId) {
        return subscriptionRegistryManager.listUserSubscriptions(userId).stream()
                .map(this::toSubscriptionVO)
                .toList();
    }

    @Override
    public List<NotificationVO> publishNotification(NotificationPublishDTO dto) {
        List<UserSubscription> matchedSubscriptions = subscriptionRegistryManager.findMatchedSubscriptions(
                dto == null ? null : dto.getTargetType(),
                dto == null ? null : dto.getTargetCode(),
                dto == null ? null : dto.getNotificationType()
        );
        return notificationQueueManager.publish(dto, matchedSubscriptions).stream()
                .map(this::toNotificationVO)
                .toList();
    }

    @Override
    public List<NotificationVO> listUserNotifications(String userId) {
        return notificationQueueManager.listUserNotifications(userId).stream()
                .map(this::toNotificationVO)
                .toList();
    }

    private SubscriptionVO toSubscriptionVO(UserSubscription subscription) {
        SubscriptionVO vo = new SubscriptionVO();
        vo.setSubscriptionId(subscription.getSubscriptionId());
        vo.setUserId(subscription.getUserId());
        vo.setTargetType(subscription.getTargetType());
        vo.setTargetCode(subscription.getTargetCode());
        vo.setSubscriptionType(subscription.getSubscriptionType());
        return vo;
    }

    private NotificationVO toNotificationVO(UserNotification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setNotificationId(notification.getNotificationId());
        vo.setUserId(notification.getUserId());
        vo.setNotificationType(notification.getNotificationType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setCreatedAt(notification.getCreatedAt());
        return vo;
    }
}
