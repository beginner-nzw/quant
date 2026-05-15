package com.quant.task.support;

public final class TaskRoutingSupport {

    public static final String TASK_TYPE_STOCK_RESEARCH = "STOCK_RESEARCH";
    public static final String TASK_TYPE_FOLLOW_UP_RESEARCH = "FOLLOW_UP_RESEARCH";
    public static final String TASK_TYPE_REPORT_REVIEW = "REPORT_REVIEW";
    public static final String TASK_TYPE_RISK_REVIEW = "RISK_REVIEW";
    public static final String TASK_TYPE_AUDIT_REVIEW = "AUDIT_REVIEW";

    public static final String ANALYSIS_SCOPE_DEEP_RESEARCH = "DEEP_RESEARCH";
    public static final String ANALYSIS_SCOPE_INTELLIGENCE_FOLLOW_UP = "INTELLIGENCE_FOLLOW_UP";
    public static final String ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP = "SIGNAL_FOLLOW_UP";
    public static final String ANALYSIS_SCOPE_REPORT_FOLLOW_UP = "REPORT_FOLLOW_UP";
    public static final String ANALYSIS_SCOPE_REPORT_REVIEW_RECHECK = "REPORT_REVIEW_RECHECK";
    public static final String ANALYSIS_SCOPE_RISK_RECHECK = "RISK_RECHECK";
    public static final String ANALYSIS_SCOPE_AUDIT_RECHECK = "AUDIT_RECHECK";

    private TaskRoutingSupport() {
    }

    public static String resolveTaskType(String taskType, String analysisScope) {
        if (hasText(taskType)) {
            return normalize(taskType);
        }

        return switch (normalize(analysisScope)) {
            case ANALYSIS_SCOPE_RISK_RECHECK -> TASK_TYPE_RISK_REVIEW;
            case ANALYSIS_SCOPE_AUDIT_RECHECK -> TASK_TYPE_AUDIT_REVIEW;
            case ANALYSIS_SCOPE_REPORT_REVIEW_RECHECK -> TASK_TYPE_REPORT_REVIEW;
            case ANALYSIS_SCOPE_INTELLIGENCE_FOLLOW_UP,
                    ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP,
                    ANALYSIS_SCOPE_REPORT_FOLLOW_UP -> TASK_TYPE_FOLLOW_UP_RESEARCH;
            default -> TASK_TYPE_STOCK_RESEARCH;
        };
    }

    public static String resolveAnalysisScope(String taskType, String analysisScope) {
        if (hasText(analysisScope)) {
            return normalize(analysisScope);
        }

        return switch (resolveTaskType(taskType, null)) {
            case TASK_TYPE_RISK_REVIEW -> ANALYSIS_SCOPE_RISK_RECHECK;
            case TASK_TYPE_AUDIT_REVIEW -> ANALYSIS_SCOPE_AUDIT_RECHECK;
            case TASK_TYPE_REPORT_REVIEW -> ANALYSIS_SCOPE_REPORT_REVIEW_RECHECK;
            case TASK_TYPE_FOLLOW_UP_RESEARCH -> ANALYSIS_SCOPE_REPORT_FOLLOW_UP;
            default -> ANALYSIS_SCOPE_DEEP_RESEARCH;
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
