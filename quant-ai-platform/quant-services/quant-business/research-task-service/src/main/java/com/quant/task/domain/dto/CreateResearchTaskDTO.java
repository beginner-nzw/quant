package com.quant.task.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateResearchTaskDTO {

    @NotBlank
    private String taskType;

    @NotBlank
    private String taskTitle;

    @NotBlank
    private String targetType;

    @NotBlank
    private String targetCode;

    @NotBlank
    private String targetName;

    @NotBlank
    private String priority;

    private String sourceChannel;

    private String sourceTaskId;

    private String sourceReportId;

    private String sourceEventId;

    private String sourceDomain;

    private String sourceReviewStatus;

    private String analysisScope;
}
