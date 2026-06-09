package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventAutoTriggerAttemptDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.mapper.MarketEventAutoTriggerAttemptMapper;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.common.web.TraceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MarketEventAutoTriggerPersistenceManager {

    public static final String AUTO_TRIGGER_DISPATCHING = "DISPATCHING";
    private static final int MAX_AUTO_TRIGGER_RETRY = 3;

    private final MarketEventMapper marketEventMapper;
    private final MarketEventAutoTriggerAttemptMapper marketEventAutoTriggerAttemptMapper;

    public MarketEventDO persistAutoTriggerResult(MarketEventDO event,
                                                  String ruleCode,
                                                  String status,
                                                  String taskId,
                                                  String reason,
                                                  String source,
                                                  String failureCode,
                                                  Integer retryCount,
                                                  String message) {
        MarketEventDO update = new MarketEventDO();
        update.setId(event.getId());
        update.setAutoTriggerRuleCode(trimToNull(ruleCode));
        update.setAutoTriggerStatus(trimToNull(status));
        update.setAutoTriggerTaskId(trimToNull(taskId));
        update.setAutoTriggerMessage(trimMessage(message, 255));
        update.setAutoTriggerReason(trimMessage(reason, 255));
        update.setAutoTriggerSource(trimToNull(source));
        update.setAutoTriggerFailureCode(trimToNull(failureCode));
        update.setAutoTriggerRetryCount(retryCount == null ? 0 : retryCount);
        update.setAutoTriggerAttemptedAt(LocalDateTime.now());
        marketEventMapper.updateById(update);

        event.setAutoTriggerRuleCode(update.getAutoTriggerRuleCode());
        event.setAutoTriggerStatus(update.getAutoTriggerStatus());
        event.setAutoTriggerTaskId(update.getAutoTriggerTaskId());
        event.setAutoTriggerMessage(update.getAutoTriggerMessage());
        event.setAutoTriggerReason(update.getAutoTriggerReason());
        event.setAutoTriggerSource(update.getAutoTriggerSource());
        event.setAutoTriggerFailureCode(update.getAutoTriggerFailureCode());
        event.setAutoTriggerRetryCount(update.getAutoTriggerRetryCount());
        event.setAutoTriggerAttemptedAt(update.getAutoTriggerAttemptedAt());
        appendAutoTriggerAttempt(event, update.getAutoTriggerRuleCode(), update.getAutoTriggerStatus(), update.getAutoTriggerTaskId(),
                update.getAutoTriggerReason(), update.getAutoTriggerSource(), update.getAutoTriggerFailureCode(),
                update.getAutoTriggerRetryCount(), update.getAutoTriggerMessage());
        return event;
    }

    public boolean claimAutoTrigger(MarketEventDO event, String ruleCode) {
        if (event == null || event.getId() == null) {
            return false;
        }
        int retryCount = defaultRetryCount(event);
        MarketEventDO update = new MarketEventDO();
        update.setAutoTriggerRuleCode(trimToNull(ruleCode));
        update.setAutoTriggerStatus(AUTO_TRIGGER_DISPATCHING);
        update.setAutoTriggerTaskId(null);
        update.setAutoTriggerReason("CLAIMED_FOR_DISPATCH:" + trimToNull(ruleCode));
        update.setAutoTriggerSource("MARKET_EVENT");
        update.setAutoTriggerFailureCode(null);
        update.setAutoTriggerRetryCount(retryCount);
        update.setAutoTriggerMessage("auto trigger dispatch claim acquired");
        update.setAutoTriggerAttemptedAt(LocalDateTime.now());
        int updated = marketEventMapper.update(
                update,
                new LambdaQueryWrapper<MarketEventDO>()
                        .eq(MarketEventDO::getId, event.getId())
                        .eq(MarketEventDO::getDeleted, 0)
                        .isNull(MarketEventDO::getAutoTriggerTaskId)
                        .and(wrapper -> wrapper
                                .lt(MarketEventDO::getAutoTriggerRetryCount, MAX_AUTO_TRIGGER_RETRY)
                                .or()
                                .isNull(MarketEventDO::getAutoTriggerRetryCount))
                        .and(wrapper -> wrapper
                                .eq(MarketEventDO::getAutoTriggerStatus, MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER)
                                .or()
                                .eq(MarketEventDO::getAutoTriggerStatus, MarketEventAutoTriggerService.AUTO_TRIGGER_FAILED))
        );
        if (updated <= 0) {
            return false;
        }
        event.setAutoTriggerRuleCode(update.getAutoTriggerRuleCode());
        event.setAutoTriggerStatus(update.getAutoTriggerStatus());
        event.setAutoTriggerReason(update.getAutoTriggerReason());
        event.setAutoTriggerSource(update.getAutoTriggerSource());
        event.setAutoTriggerFailureCode(update.getAutoTriggerFailureCode());
        event.setAutoTriggerRetryCount(update.getAutoTriggerRetryCount());
        event.setAutoTriggerMessage(update.getAutoTriggerMessage());
        event.setAutoTriggerAttemptedAt(update.getAutoTriggerAttemptedAt());
        appendAutoTriggerAttempt(event, ruleCode, AUTO_TRIGGER_DISPATCHING, null,
                update.getAutoTriggerReason(), update.getAutoTriggerSource(), null, retryCount, update.getAutoTriggerMessage());
        return true;
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
        if (marketEventAutoTriggerAttemptMapper == null || event == null || !StringUtils.hasText(event.getEventId())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        MarketEventAutoTriggerAttemptDO attempt = new MarketEventAutoTriggerAttemptDO();
        attempt.setAttemptId(UUID.randomUUID().toString());
        attempt.setEventId(event.getEventId());
        attempt.setRuleCode(trimToNull(ruleCode));
        attempt.setStatus(trimToNull(status));
        attempt.setTaskId(trimToNull(taskId));
        attempt.setReason(trimMessage(reason, 255));
        attempt.setSource(trimToNull(source));
        attempt.setFailureCode(trimToNull(failureCode));
        attempt.setRetryCount(retryCount == null ? 0 : retryCount);
        attempt.setMessage(trimMessage(message, 1000));
        attempt.setTraceId(TraceContext.currentTraceId());
        attempt.setTenantId("default");
        attempt.setAttemptedAt(now);
        attempt.setCreatedAt(now);
        attempt.setDeleted(0);
        marketEventAutoTriggerAttemptMapper.insert(attempt);
    }

    public int defaultRetryCount(MarketEventDO event) {
        return event == null || event.getAutoTriggerRetryCount() == null ? 0 : event.getAutoTriggerRetryCount();
    }

    public String trimMessage(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
