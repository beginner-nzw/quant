package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HumanReviewCommandManager {

    private final HumanReviewDecisionManager humanReviewDecisionManager;

    public String decide(String queueId, HumanReviewDecisionDTO dto) {
        QueueRef ref = parseQueueId(queueId);
        HumanReviewDecisionDTO safeDto = dto == null ? new HumanReviewDecisionDTO() : dto;
        ReportReviewStatusEnum decision = resolveDecision(safeDto.getDecision());
        if (HumanReviewQueueManager.DOMAIN_RISK.equals(ref.domain())) {
            humanReviewDecisionManager.decideRisk(ref.taskId(), safeDto, decision);
        } else {
            humanReviewDecisionManager.decideReport(ref.taskId(), safeDto, decision);
        }
        if (Boolean.TRUE.equals(safeDto.getRerunWorkflow())) {
            humanReviewDecisionManager.rerunWorkflow(ref.taskId(), safeDto);
        }
        return ref.taskId();
    }

    private QueueRef parseQueueId(String queueId) {
        if (!hasText(queueId) || !queueId.contains(":")) {
            throw new BizException("HUMAN_REVIEW_QUEUE_ID_INVALID", "human review queue id invalid");
        }
        String[] parts = queueId.split(":", 2);
        String domain = parts[0].trim().toUpperCase();
        String taskId = parts[1].trim();
        if (!List.of(HumanReviewQueueManager.DOMAIN_REPORT,
                HumanReviewQueueManager.DOMAIN_RISK,
                HumanReviewQueueManager.DOMAIN_COMPLIANCE).contains(domain) || !hasText(taskId)) {
            throw new BizException("HUMAN_REVIEW_QUEUE_ID_INVALID", "human review queue id invalid");
        }
        return new QueueRef(domain, taskId);
    }

    private ReportReviewStatusEnum resolveDecision(String decision) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(decision);
        if (resolved == null) {
            throw new BizException("HUMAN_REVIEW_DECISION_INVALID", "human review decision invalid");
        }
        return resolved;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record QueueRef(String domain, String taskId) {
    }
}
