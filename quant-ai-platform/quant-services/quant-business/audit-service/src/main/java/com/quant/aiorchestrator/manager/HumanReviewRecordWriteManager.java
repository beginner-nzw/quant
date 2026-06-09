package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.entity.HumanReviewRecordDO;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HumanReviewRecordWriteManager {

    private final HumanReviewRecordMapper humanReviewRecordMapper;

    public void insertReviewRecord(String taskId,
                                   String objectType,
                                   String objectId,
                                   String reviewerId,
                                   String reviewerRole,
                                   String decision,
                                   String comment,
                                   String beforeSnapshot,
                                   String afterSnapshot,
                                   String traceId,
                                   String tenantId) {
        HumanReviewRecordDO record = new HumanReviewRecordDO();
        record.setReviewId(UUID.randomUUID().toString());
        record.setTaskId(taskId);
        record.setRelatedObjectType(objectType);
        record.setRelatedObjectId(objectId);
        record.setReviewerId(reviewerId);
        record.setReviewerRole(reviewerRole);
        record.setReviewResult(decision);
        record.setReviewComment(comment);
        record.setBeforeSnapshot(beforeSnapshot);
        record.setAfterSnapshot(afterSnapshot);
        record.setTraceId(traceId);
        record.setTenantId(firstText(tenantId, "default"));
        record.setDeleted(0);
        humanReviewRecordMapper.insert(record);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
