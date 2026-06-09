package com.quant.subscription.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.subscription.domain.entity.RiskSubscriptionDO;
import com.quant.subscription.domain.entity.UserSubscription;
import com.quant.subscription.mapper.RiskSubscriptionMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MybatisSubscriptionRepository implements SubscriptionRepository {

    private final RiskSubscriptionMapper riskSubscriptionMapper;

    public MybatisSubscriptionRepository(RiskSubscriptionMapper riskSubscriptionMapper) {
        this.riskSubscriptionMapper = riskSubscriptionMapper;
    }

    @Override
    public void save(UserSubscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        RiskSubscriptionDO row = new RiskSubscriptionDO();
        row.setSubscriptionId(subscription.getSubscriptionId());
        row.setUserId(subscription.getUserId());
        row.setTargetType(subscription.getTargetType());
        row.setTargetCode(subscription.getTargetCode());
        row.setSubscriptionType(subscription.getSubscriptionType());
        row.setStatus("ENABLED");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        row.setDeleted(0);
        riskSubscriptionMapper.insert(row);
    }

    @Override
    public List<UserSubscription> findByUserId(String userId) {
        return riskSubscriptionMapper.selectList(new LambdaQueryWrapper<RiskSubscriptionDO>()
                        .eq(RiskSubscriptionDO::getUserId, userId)
                        .eq(RiskSubscriptionDO::getStatus, "ENABLED")
                        .eq(RiskSubscriptionDO::getDeleted, 0))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<UserSubscription> findMatched(String targetType, String targetCode, String subscriptionType) {
        return riskSubscriptionMapper.selectList(new LambdaQueryWrapper<RiskSubscriptionDO>()
                        .eq(RiskSubscriptionDO::getTargetType, targetType)
                        .eq(RiskSubscriptionDO::getTargetCode, targetCode)
                        .eq(RiskSubscriptionDO::getSubscriptionType, subscriptionType)
                        .eq(RiskSubscriptionDO::getStatus, "ENABLED")
                        .eq(RiskSubscriptionDO::getDeleted, 0))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private UserSubscription toDomain(RiskSubscriptionDO row) {
        return new UserSubscription(
                row.getSubscriptionId(),
                row.getUserId(),
                row.getTargetType(),
                row.getTargetCode(),
                row.getSubscriptionType()
        );
    }
}
