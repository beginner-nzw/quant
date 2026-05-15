package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.aiorchestrator.service.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventAutoTriggerServiceImpl implements MarketEventAutoTriggerService {

    public static final String AUTO_TRIGGER_DISABLED = "DISABLED";
    public static final String AUTO_TRIGGER_NO_MATCH = "NO_MATCH";
    public static final String AUTO_TRIGGER_SUCCESS = "SUCCESS";
    public static final String AUTO_TRIGGER_FAILED = "FAILED";
    public static final String AUTO_TRIGGER_WILL_TRIGGER = "WILL_TRIGGER";

    private final MarketEventMapper marketEventMapper;
    private final EventAutoTriggerConfigService eventAutoTriggerConfigService;
    private final EventAutoTaskDispatchService eventAutoTaskDispatchService;

    public MarketEventDO prepareAutoTrigger(MarketEventDO event) {
        if (event == null || event.getId() == null) {
            return event;
        }

        EventAutoTriggerConfigService.EventAutoTriggerConfig config = eventAutoTriggerConfigService.loadConfig();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return persistAutoTriggerResult(event, null, AUTO_TRIGGER_DISABLED, null, "事件自动触发已关闭");
        }

        EventAutoTriggerConfigService.EventAutoTriggerRule rule =
                eventAutoTriggerConfigService.resolveMatchedRule(event.getEventType(), event.getImpactLevel());
        if (rule == null) {
            return persistAutoTriggerResult(event, null, AUTO_TRIGGER_NO_MATCH, null, "未命中自动触发规则");
        }

        return persistAutoTriggerResult(event, rule.getRuleCode(), AUTO_TRIGGER_WILL_TRIGGER, null, "已进入自动触发队列");
    }

    public MarketEventDO loadEvent(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return null;
        }
        return marketEventMapper.selectOne(
                new LambdaQueryWrapper<MarketEventDO>()
                        .eq(MarketEventDO::getEventId, eventId)
                        .eq(MarketEventDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    public boolean isPendingAutoTrigger(MarketEventDO event) {
        return event != null && AUTO_TRIGGER_WILL_TRIGGER.equalsIgnoreCase(event.getAutoTriggerStatus());
    }

    public String executePendingAutoTrigger(MarketEventDO event) {
        if (event == null) {
            return null;
        }
        EventAutoTriggerConfigService.EventAutoTriggerRule rule = resolveRuleForExecution(event);
        String ruleCode = rule == null ? event.getAutoTriggerRuleCode() : rule.getRuleCode();
        try {
            String taskId = eventAutoTaskDispatchService.createFollowUpTask(event, rule);
            persistAutoTriggerResult(event, ruleCode, AUTO_TRIGGER_SUCCESS, taskId, "已自动创建跟踪研究任务");
            log.info("event-driven auto trigger follow-up task success, eventId={}, taskId={}, ruleCode={}",
                    event.getEventId(), taskId, ruleCode);
            return taskId;
        } catch (Exception e) {
            persistAutoTriggerResult(event, ruleCode, AUTO_TRIGGER_FAILED, null, trimMessage(e.getMessage(), 255));
            log.warn("event-driven auto trigger follow-up task failed, eventId={}, ruleCode={}, err={}",
                    event.getEventId(), ruleCode, e.getMessage());
            throw e;
        }
    }

    public boolean shouldCountAsQueued(String autoTriggerStatus) {
        return AUTO_TRIGGER_SUCCESS.equalsIgnoreCase(autoTriggerStatus)
                || AUTO_TRIGGER_WILL_TRIGGER.equalsIgnoreCase(autoTriggerStatus);
    }

    private EventAutoTriggerConfigService.EventAutoTriggerRule resolveRuleForExecution(MarketEventDO event) {
        EventAutoTriggerConfigService.EventAutoTriggerRule rule =
                eventAutoTriggerConfigService.findEnabledRuleByCode(event.getAutoTriggerRuleCode());
        if (rule != null) {
            return rule;
        }
        rule = eventAutoTriggerConfigService.resolveMatchedRule(event.getEventType(), event.getImpactLevel());
        if (rule != null) {
            return rule;
        }
        throw new BizException("EVENT_AUTO_TRIGGER_RULE_NOT_FOUND", "未找到可执行的事件自动触发规则");
    }

    private MarketEventDO persistAutoTriggerResult(MarketEventDO event,
                                                   String ruleCode,
                                                   String status,
                                                   String taskId,
                                                   String message) {
        MarketEventDO update = new MarketEventDO();
        update.setId(event.getId());
        update.setAutoTriggerRuleCode(trimToNull(ruleCode));
        update.setAutoTriggerStatus(trimToNull(status));
        update.setAutoTriggerTaskId(trimToNull(taskId));
        update.setAutoTriggerMessage(trimMessage(message, 255));
        update.setAutoTriggerAttemptedAt(LocalDateTime.now());
        marketEventMapper.updateById(update);

        event.setAutoTriggerRuleCode(update.getAutoTriggerRuleCode());
        event.setAutoTriggerStatus(update.getAutoTriggerStatus());
        event.setAutoTriggerTaskId(update.getAutoTriggerTaskId());
        event.setAutoTriggerMessage(update.getAutoTriggerMessage());
        event.setAutoTriggerAttemptedAt(update.getAutoTriggerAttemptedAt());
        return event;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimMessage(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
