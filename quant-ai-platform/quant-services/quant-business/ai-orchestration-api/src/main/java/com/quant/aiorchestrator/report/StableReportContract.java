package com.quant.aiorchestrator.report;

import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;

import java.util.List;

public final class StableReportContract {

    public static final String API_BASE = "/api/reports";
    public static final String LEGACY_TASK_API_BASE = "/api/tasks";

    public static final String REPORT_GENERATED_TOPIC = KafkaTopicConstants.REPORT_GENERATED;
    public static final String REPORT_GENERATED_MESSAGE_TYPE = MessageTypeConstants.REPORT_GENERATED;
    public static final String REPORT_GENERATED_VERSION = "1.0";
    public static final String REPORT_GENERATED_SOURCE_SERVICE = "ai-orchestration-service";
    public static final String REPORT_GENERATED_TARGET_SERVICE = "domain-event-subscribers";
    public static final String REPORT_GENERATED_BIZ_KEY_PREFIX = "REPORT:";

    public static final List<String> STABLE_ENDPOINTS = List.of(
            "GET /api/reports/center",
            "GET /api/reports/center/stats",
            "GET /api/reports/review/stats",
            "GET /api/reports/tasks/{taskId}",
            "GET /api/reports/tasks/{taskId}/versions",
            "GET /api/reports/tasks/{taskId}/versions/compare",
            "GET /api/reports/tasks/{taskId}/versions/{versionNo}",
            "GET /api/reports/tasks/{taskId}/review-logs",
            "POST /api/reports/tasks/{taskId}/review"
    );

    public static final List<String> LEGACY_COMPAT_ENDPOINTS = List.of(
            "GET /api/tasks/report-center",
            "GET /api/tasks/report-center-stats",
            "GET /api/tasks/report-review-stats",
            "GET /api/tasks/{taskId}/report",
            "GET /api/tasks/{taskId}/report/versions",
            "GET /api/tasks/{taskId}/report/versions/compare",
            "GET /api/tasks/{taskId}/report/versions/{versionNo}",
            "GET /api/tasks/{taskId}/report/review-logs",
            "POST /api/tasks/{taskId}/report/review"
    );

    private StableReportContract() {
    }
}
