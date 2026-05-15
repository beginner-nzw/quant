package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("human_review_record")
public class HumanReviewRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reviewId;
    private String taskId;
    private String relatedObjectType;
    private String relatedObjectId;
    private String reviewerId;
    private String reviewerRole;
    private String reviewResult;
    private String reviewComment;
    private String beforeSnapshotRef;
    private String afterSnapshotRef;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String traceId;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
