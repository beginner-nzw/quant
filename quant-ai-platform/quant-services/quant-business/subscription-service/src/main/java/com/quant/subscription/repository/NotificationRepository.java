package com.quant.subscription.repository;

import com.quant.subscription.domain.entity.UserNotification;

import java.util.List;

public interface NotificationRepository {

    void save(UserNotification notification);

    List<UserNotification> findByUserId(String userId);
}
