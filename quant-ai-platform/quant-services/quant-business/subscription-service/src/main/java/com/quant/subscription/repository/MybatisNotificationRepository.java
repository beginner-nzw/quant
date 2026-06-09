package com.quant.subscription.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.subscription.domain.entity.NotificationDispatchDO;
import com.quant.subscription.domain.entity.UserNotification;
import com.quant.subscription.mapper.NotificationDispatchMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class MybatisNotificationRepository implements NotificationRepository {

    private final NotificationDispatchMapper notificationDispatchMapper;

    public MybatisNotificationRepository(NotificationDispatchMapper notificationDispatchMapper) {
        this.notificationDispatchMapper = notificationDispatchMapper;
    }

    @Override
    public void save(UserNotification notification) {
        LocalDateTime createdAt = LocalDateTime.ofInstant(notification.getCreatedAt(), ZoneOffset.UTC);
        NotificationDispatchDO row = new NotificationDispatchDO();
        row.setNotificationId(notification.getNotificationId());
        row.setUserId(notification.getUserId());
        row.setNotificationType(notification.getNotificationType());
        row.setTitle(notification.getTitle());
        row.setContent(notification.getContent());
        row.setDispatchStatus("QUEUED");
        row.setCreatedAt(createdAt);
        row.setUpdatedAt(createdAt);
        row.setDeleted(0);
        notificationDispatchMapper.insert(row);
    }

    @Override
    public List<UserNotification> findByUserId(String userId) {
        return notificationDispatchMapper.selectList(new LambdaQueryWrapper<NotificationDispatchDO>()
                        .eq(NotificationDispatchDO::getUserId, userId)
                        .eq(NotificationDispatchDO::getDeleted, 0)
                        .orderByDesc(NotificationDispatchDO::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private UserNotification toDomain(NotificationDispatchDO row) {
        Instant createdAt = row.getCreatedAt() == null
                ? Instant.EPOCH
                : row.getCreatedAt().toInstant(ZoneOffset.UTC);
        return new UserNotification(
                row.getNotificationId(),
                row.getUserId(),
                row.getNotificationType(),
                row.getTitle(),
                row.getContent(),
                createdAt
        );
    }
}
