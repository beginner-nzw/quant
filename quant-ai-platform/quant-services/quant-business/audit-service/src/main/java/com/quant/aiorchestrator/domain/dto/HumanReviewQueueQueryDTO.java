package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class HumanReviewQueueQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String domain;
    private String reviewStatus;
    private String targetCode;
    private String targetName;
    private Boolean onlyPending = true;
}
