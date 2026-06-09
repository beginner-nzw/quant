package com.quant.subscription.repository;

import com.quant.subscription.domain.entity.UserNotification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnMissingBean(NotificationRepository.class)
public class InMemoryNotificationRepository implements NotificationRepository {

    private final List<UserNotification> notifications = new CopyOnWriteArrayList<>();

    @Override
    public void save(UserNotification notification) {
        notifications.add(notification);
    }

    @Override
    public List<UserNotification> findByUserId(String userId) {
        return notifications.stream()
                .filter(notification -> notification.getUserId().equals(userId))
                .toList();
    }
}
