TASK_TYPE_STOCK_RESEARCH = "STOCK_RESEARCH"
TASK_TYPE_FOLLOW_UP_RESEARCH = "FOLLOW_UP_RESEARCH"
TASK_TYPE_REPORT_REVIEW = "REPORT_REVIEW"
TASK_TYPE_RISK_REVIEW = "RISK_REVIEW"
TASK_TYPE_AUDIT_REVIEW = "AUDIT_REVIEW"

ANALYSIS_SCOPE_DEEP_RESEARCH = "DEEP_RESEARCH"
ANALYSIS_SCOPE_INTELLIGENCE_FOLLOW_UP = "INTELLIGENCE_FOLLOW_UP"
ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP = "SIGNAL_FOLLOW_UP"
ANALYSIS_SCOPE_REPORT_FOLLOW_UP = "REPORT_FOLLOW_UP"
ANALYSIS_SCOPE_REPORT_REVIEW_RECHECK = "REPORT_REVIEW_RECHECK"
ANALYSIS_SCOPE_RISK_RECHECK = "RISK_RECHECK"
ANALYSIS_SCOPE_AUDIT_RECHECK = "AUDIT_RECHECK"


def _normalize(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = str(value).strip()
    return normalized or None


def resolve_task_type(task_type: str | None, analysis_scope: str | None) -> str:
    normalized_task_type = _normalize(task_type)
    if normalized_task_type:
        return normalized_task_type

    normalized_scope = _normalize(analysis_scope)
    if normalized_scope == ANALYSIS_SCOPE_RISK_RECHECK:
        return TASK_TYPE_RISK_REVIEW
    if normalized_scope == ANALYSIS_SCOPE_AUDIT_RECHECK:
        return TASK_TYPE_AUDIT_REVIEW
    if normalized_scope == ANALYSIS_SCOPE_REPORT_REVIEW_RECHECK:
        return TASK_TYPE_REPORT_REVIEW
    if normalized_scope in {
        ANALYSIS_SCOPE_INTELLIGENCE_FOLLOW_UP,
        ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP,
        ANALYSIS_SCOPE_REPORT_FOLLOW_UP,
    }:
        return TASK_TYPE_FOLLOW_UP_RESEARCH
    return TASK_TYPE_STOCK_RESEARCH


def resolve_analysis_scope(task_type: str | None, analysis_scope: str | None) -> str:
    normalized_scope = _normalize(analysis_scope)
    if normalized_scope:
        return normalized_scope

    normalized_task_type = resolve_task_type(task_type, None)
    if normalized_task_type == TASK_TYPE_RISK_REVIEW:
        return ANALYSIS_SCOPE_RISK_RECHECK
    if normalized_task_type == TASK_TYPE_AUDIT_REVIEW:
        return ANALYSIS_SCOPE_AUDIT_RECHECK
    if normalized_task_type == TASK_TYPE_REPORT_REVIEW:
        return ANALYSIS_SCOPE_REPORT_REVIEW_RECHECK
    if normalized_task_type == TASK_TYPE_FOLLOW_UP_RESEARCH:
        return ANALYSIS_SCOPE_REPORT_FOLLOW_UP
    return ANALYSIS_SCOPE_DEEP_RESEARCH


def build_focus_dimensions(task_type: str, analysis_scope: str) -> list[str]:
    if task_type == TASK_TYPE_RISK_REVIEW:
        return ["RISK", "TRIGGER", "DISCLOSURE"]
    if task_type == TASK_TYPE_AUDIT_REVIEW:
        return ["COMPLIANCE", "TRACE", "RISK"]
    if task_type == TASK_TYPE_REPORT_REVIEW:
        return ["REPORT", "EVIDENCE", "RISK"]
    if analysis_scope == ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP:
        return ["SIGNAL", "MARKET", "RISK"]
    if analysis_scope == ANALYSIS_SCOPE_INTELLIGENCE_FOLLOW_UP:
        return ["INTELLIGENCE", "MARKET", "RISK"]
    if analysis_scope == ANALYSIS_SCOPE_REPORT_FOLLOW_UP:
        return ["REPORT", "MARKET", "RISK"]
    return ["FINANCIAL", "INDUSTRY", "RISK"]


def build_report_type(task_type: str, analysis_scope: str) -> str:
    if task_type == TASK_TYPE_RISK_REVIEW:
        return "RISK_REVIEW_REPORT"
    if task_type == TASK_TYPE_AUDIT_REVIEW:
        return "AUDIT_REVIEW_REPORT"
    if task_type == TASK_TYPE_REPORT_REVIEW:
        return "REPORT_REVIEW_REPORT"
    if analysis_scope == ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP:
        return "SIGNAL_FOLLOW_UP_REPORT"
    if analysis_scope == ANALYSIS_SCOPE_INTELLIGENCE_FOLLOW_UP:
        return "INTELLIGENCE_FOLLOW_UP_REPORT"
    if analysis_scope == ANALYSIS_SCOPE_REPORT_FOLLOW_UP:
        return "REPORT_FOLLOW_UP_REPORT"
    return "STOCK_DEEP_RESEARCH"


def build_review_suggestion(task_type: str, need_human_review: bool) -> str:
    if need_human_review:
        return "建议人工复核，并补充来源证据后再确认关键风险判断。"
    if task_type == TASK_TYPE_RISK_REVIEW:
        return "风险复核已完成，可继续跟踪风险暴露与处置计划。"
    if task_type == TASK_TYPE_AUDIT_REVIEW:
        return "审计复核已完成，可进入合规留痕与复盘流程。"
    if task_type == TASK_TYPE_REPORT_REVIEW:
        return "报告复核已完成，可根据结论决定是否重新提交审核。"
    if task_type == TASK_TYPE_FOLLOW_UP_RESEARCH:
        return "跟踪研究已完成，可继续观察后续市场变化。"
    return "深度研究已完成，可进入报告审核流程。"
