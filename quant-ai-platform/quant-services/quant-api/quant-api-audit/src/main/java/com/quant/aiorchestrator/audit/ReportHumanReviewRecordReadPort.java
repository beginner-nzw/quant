package com.quant.aiorchestrator.audit;

import java.util.List;

public interface ReportHumanReviewRecordReadPort {
    List<ReportHumanReviewRecordProjection> listReportReviewRecords(String reportId);
}
