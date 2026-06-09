package com.quant.aiorchestrator.audit;

public interface ReportHumanReviewRecordWritePort {
    void insertReportReviewRecord(ReportHumanReviewRecordWriteCommand command);
}
