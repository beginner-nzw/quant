package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.common.core.exception.BizException;
import com.quant.task.market.MarketEventTaskProjection;
import com.quant.task.market.MarketEventTaskReadPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class MarketEventAutoTriggerAttemptManager {

    public static final String AUTO_TRIGGER_DISPATCHING = "DISPATCHING";
    public static final String AUTO_TRIGGER_SKIPPED_DUPLICATE = "SKIPPED_DUPLICATE";
    private static final int MAX_AUTO_TRIGGER_RETRY = 3;
    private static final int EVENT_RATE_LIMIT_PER_MINUTE = 3;
    private static final int GLOBAL_RATE_LIMIT_PER_MINUTE = 60;

    private final MarketEventAutoTriggerPersistenceManager persistenceManager;
    private final MarketEventTaskReadPort marketEventTaskReadPort;
    private final StringRedisTemplate stringRedisTemplate;

    public MarketEventAutoTriggerAttemptManager(MarketEventAutoTriggerPersistenceManager persistenceManager,
                                                MarketEventTaskReadPort marketEventTaskReadPort,
                                                StringRedisTemplate stringRedisTemplate) {
        this.persistenceManager = persistenceManager;
        this.marketEventTaskReadPort = marketEventTaskReadPort;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public MarketEventDO persistAutoTriggerResult(MarketEventDO event,
                                                  String ruleCode,
                                                  String status,
                                                  String taskId,
                                                  String reason,
                                                  String source,
                                                  String failureCode,
                                                  Integer retryCount,
                                                  String message) {
        return persistenceManager.persistAutoTriggerResult(event, ruleCode, status, taskId, reason, source, failureCode, retryCount, message);
    }

    public boolean claimAutoTrigger(MarketEventDO event, String ruleCode) {
        return persistenceManager.claimAutoTrigger(event, ruleCode);
    }

    public void appendAutoTriggerAttempt(MarketEventDO event,
                                         String ruleCode,
                                         String status,
                                         String taskId,
                                         String reason,
                                         String source,
                                         String failureCode,
                                         Integer retryCount,
                                         String message) {
        persistenceManager.appendAutoTriggerAttempt(event, ruleCode, status, taskId, reason, source, failureCode, retryCount, message);
    }

    public MarketEventTaskProjection findExistingAutoTriggeredTask(MarketEventDO event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            return null;
        }
        return marketEventTaskReadPort.selectLatestTaskBySourceEvent("MARKET_EVENT", event.getEventId());
    }

    public void assertWithinRateLimit(MarketEventDO event) {
        incrementRateCounter("market:event:auto-trigger:global", GLOBAL_RATE_LIMIT_PER_MINUTE);
        incrementRateCounter("market:event:auto-trigger:event:" + event.getEventId(), EVENT_RATE_LIMIT_PER_MINUTE);
    }

    public int defaultRetryCount(MarketEventDO event) {
        return persistenceManager.defaultRetryCount(event);
    }

    public boolean canRetry(MarketEventDO event) {
        return defaultRetryCount(event) < MAX_AUTO_TRIGGER_RETRY;
    }

    public String resolveFailureCode(Exception e) {
        if (e instanceof BizException bizException && StringUtils.hasText(bizException.getCode())) {
            return bizException.getCode();
        }
        return "EVENT_AUTO_TRIGGER_FAILED";
    }

    public String trimMessage(String value, int maxLength) {
        return persistenceManager.trimMessage(value, maxLength);
    }

    private void incrementRateCounter(String key, int limit) {
        if (stringRedisTemplate == null || stringRedisTemplate.opsForValue() == null) {
            return;
        }
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > limit) {
            throw new BizException("EVENT_AUTO_TRIGGER_RATE_LIMITED", "event auto trigger rate limited");
        }
    }

}
