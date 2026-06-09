package com.quant.aiorchestrator.report;

import java.util.List;
import java.util.Set;

public interface TaskReportReadPort {

    Set<String> findTaskIdsByReviewFilter(Boolean onlyPendingReview,
                                          String reportReviewStatus,
                                          String reportReviewedBy);

    List<TaskReportProjection> listReportsByTaskIds(List<String> taskIds);

    List<TaskReportProjection> listReportsByTaskIdSet(Set<String> taskIds);

    List<TaskReportProjection> listActiveReports();
}
