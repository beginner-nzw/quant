package com.quant.aiorchestrator.audit;

import java.util.List;

public interface HumanReviewQueueReportProvider {
    List<HumanReviewQueueReportProjection> listHumanReviewQueueReports();
}
