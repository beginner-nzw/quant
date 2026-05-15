package com.quant.common.model.enums;

public enum ReportReviewStatusEnum {
    PENDING,
    APPROVED,
    REJECTED;

    public static ReportReviewStatusEnum from(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (ReportReviewStatusEnum value : values()) {
            if (value.name().equalsIgnoreCase(status)) {
                return value;
            }
        }
        return null;
    }
}
