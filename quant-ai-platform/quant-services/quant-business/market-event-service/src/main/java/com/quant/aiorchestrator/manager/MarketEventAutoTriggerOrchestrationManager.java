package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.service.EventAutoTaskDispatchService;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.common.core.exception.BizException;
import com.quant.config.port.EventAutoTriggerConfigPort;
import com.quant.task.market.MarketEventTaskProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketEventAutoTriggerOrchestrationManager {

    private final MarketEventAutoTriggerAttemptManager marketEventAutoTriggerAttemptManager;
    private final EventAutoTriggerConfigPort eventAutoTriggerConfigPort;
    private final EventAutoTaskDispatchService eventAutoTaskDispatchService;

    public MarketEventDO prepareAutoTrigger(MarketEventDO event) {
        if (event == null || event.getId() == null) {
            return event;
        }

        EventAutoTriggerConfigPort.EventAutoTriggerConfig config = eventAutoTriggerConfigPort.loadConfig();
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return marketEventAutoTriggerAttemptManager.persistAutoTriggerResult(
                    event,
                    null,
                    MarketEventAutoTriggerService.AUTO_TRIGGER_DISABLED,
                    null,
                    "AUTO_TRIGGER_DISABLED",
                    "EVENT_AUTO_CONFIG",
                    null,
                    0,
                    "event auto trigger disabled"
            );
        }

        EventAutoTriggerConfigPort.EventAutoTriggerRule rule =
                eventAutoTriggerConfigPort.resolveMatchedRule(event.getEventType(), event.getImpactLevel());
        if (rule == null) {
            return marketEventAutoTriggerAttemptManager.persistAutoTriggerResult(
                    event,
                    null,
                    MarketEventAutoTriggerService.AUTO_TRIGGER_NO_MATCH,
                    null,
                    "NO_MATCHED_RULE",
                    "EVENT_AUTO_RULE",
                    null,
                    0,
                    "no enabled event auto trigger rule matched"
            );
        }

        return marketEventAutoTriggerAttemptManager.persistAutoTriggerResult(
                event,
                rule.getRuleCode(),
                MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER,
                null,
                "MATCHED_RULE:" + rule.getRuleCode(),
                "MARKET_EVENT",
                null,
                0,
                "queued for event auto trigger"
        );
    }

    public boolean isPendingAutoTrigger(MarketEventDO event) {
        if (event == null || StringUtils.hasText(event.getAutoTriggerTaskId())) {
            return false;
        }
        if (MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER.equalsIgnoreCase(event.getAutoTriggerStatus())) {
            return true;
        }
        return MarketEventAutoTriggerService.AUTO_TRIGGER_FAILED.equalsIgnoreCase(event.getAutoTriggerStatus())
                && marketEventAutoTriggerAttemptManager.canRetry(event);
    }

    public String executePendingAutoTrigger(MarketEventDO event) {
        if (event == null) {
            return null;
        }
        EventAutoTriggerConfigPort.EventAutoTriggerRule rule = resolveRuleForExecution(event);
        String ruleCode = rule == null ? event.getAutoTriggerRuleCode() : rule.getRuleCode();
        MarketEventTaskProjection existingTask = marketEventAutoTriggerAttemptManager.findExistingAutoTriggeredTask(event);
        if (existingTask != null) {
            marketEventAutoTriggerAttemptManager.persistAutoTriggerResult(
                    event,
                    ruleCode,
                    MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS,
                    existingTask.taskId(),
                    "IDEMPOTENT_EXISTING_TASK",
                    "RESEARCH_TASK",
                    null,
                    marketEventAutoTriggerAttemptManager.defaultRetryCount(event),
                    "existing event follow-up task reused"
            );
            return existingTask.taskId();
        }

        if (!marketEventAutoTriggerAttemptManager.claimAutoTrigger(event, ruleCode)) {
            MarketEventTaskProjection taskAfterClaimLost = marketEventAutoTriggerAttemptManager.findExistingAutoTriggeredTask(event);
            if (taskAfterClaimLost != null) {
                marketEventAutoTriggerAttemptManager.persistAutoTriggerResult(
                        event,
                        ruleCode,
                        MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS,
                        taskAfterClaimLost.taskId(),
                        "IDEMPOTENT_EXISTING_TASK_AFTER_CLAIM_LOST",
                        "RESEARCH_TASK",
                        null,
                        marketEventAutoTriggerAttemptManager.defaultRetryCount(event),
                        "existing event follow-up task reused after claim lost"
                );
                return taskAfterClaimLost.taskId();
            }
            marketEventAutoTriggerAttemptManager.appendAutoTriggerAttempt(
                    event,
                    ruleCode,
                    MarketEventAutoTriggerAttemptManager.AUTO_TRIGGER_SKIPPED_DUPLICATE,
                    null,
                    "CLAIM_ALREADY_HELD",
                    "MARKET_EVENT",
                    null,
                    marketEventAutoTriggerAttemptManager.defaultRetryCount(event),
                    "auto trigger claim already held"
            );
            return null;
        }

        try {
            marketEventAutoTriggerAttemptManager.assertWithinRateLimit(event);
            String taskId = eventAutoTaskDispatchService.createFollowUpTask(event, rule);
            marketEventAutoTriggerAttemptManager.persistAutoTriggerResult(
                    event,
                    ruleCode,
                    MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS,
                    taskId,
                    "DISPATCHED_BY_RULE:" + ruleCode,
                    "RESEARCH_TASK_SERVICE",
                    null,
                    marketEventAutoTriggerAttemptManager.defaultRetryCount(event),
                    "auto follow-up task created"
            );
            log.info("event-driven auto trigger follow-up task success, eventId={}, taskId={}, ruleCode={}",
                    event.getEventId(), taskId, ruleCode);
            return taskId;
        } catch (Exception e) {
            marketEventAutoTriggerAttemptManager.persistAutoTriggerResult(
                    event,
                    ruleCode,
                    MarketEventAutoTriggerService.AUTO_TRIGGER_FAILED,
                    null,
                    "DISPATCH_FAILED",
                    "RESEARCH_TASK_SERVICE",
                    marketEventAutoTriggerAttemptManager.resolveFailureCode(e),
                    marketEventAutoTriggerAttemptManager.defaultRetryCount(event) + 1,
                    marketEventAutoTriggerAttemptManager.trimMessage(e.getMessage(), 255)
            );
            log.warn("event-driven auto trigger follow-up task failed, eventId={}, ruleCode={}, err={}",
                    event.getEventId(), ruleCode, e.getMessage());
            throw e;
        }
    }

    public boolean shouldCountAsQueued(String autoTriggerStatus) {
        return MarketEventAutoTriggerService.AUTO_TRIGGER_SUCCESS.equalsIgnoreCase(autoTriggerStatus)
                || MarketEventAutoTriggerService.AUTO_TRIGGER_WILL_TRIGGER.equalsIgnoreCase(autoTriggerStatus);
    }

    private EventAutoTriggerConfigPort.EventAutoTriggerRule resolveRuleForExecution(MarketEventDO event) {
        EventAutoTriggerConfigPort.EventAutoTriggerRule rule =
                eventAutoTriggerConfigPort.findEnabledRuleByCode(event.getAutoTriggerRuleCode());
        if (rule != null) {
            return rule;
        }
        rule = eventAutoTriggerConfigPort.resolveMatchedRule(event.getEventType(), event.getImpactLevel());
        if (rule != null) {
            return rule;
        }
        throw new BizException("EVENT_AUTO_TRIGGER_RULE_NOT_FOUND", "鏈壘鍒板彲鎵ц鐨勪簨浠惰嚜鍔ㄨЕ鍙戣鍒?");
    }
}
