package com.quant.aiorchestrator.report;

import java.util.Set;

public final class ReportDomainOwnership {

    public static final String DOMAIN = "report";
    public static final String CURRENT_HOST = "ai-orchestration-service";

    public static final String REPORT_SOT = "research_report";
    public static final String VERSION_SOT = "research_report_version";
    public static final String SECTION_SOT = "research_report_section";
    public static final String EVIDENCE_SOT = "report_evidence_ref";
    public static final String REVIEW_LOG_SOT = "research_report_review_log";
    public static final String HUMAN_REVIEW_AUDIT_SOT = "human_review_record";

    public static final Set<String> AUTHORITY_OBJECTS = Set.of(
            REPORT_SOT,
            VERSION_SOT,
            SECTION_SOT,
            EVIDENCE_SOT,
            REVIEW_LOG_SOT,
            HUMAN_REVIEW_AUDIT_SOT
    );

    private ReportDomainOwnership() {
    }
}
