import time

from app.common.task_routing import build_focus_dimensions
from app.services.langchain_intent_service import LangChainIntentService
from app.services.task_control_service import TaskControlService
from app.utils.logger import log_info


class IntentAgent:
    def __init__(self):
        self.langchain_intent_service = LangChainIntentService()
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        task_type = state.get("task_type", "")
        analysis_scope = state.get("analysis_scope", "")
        source_context = state.get("source_context", {})
        task_context = state.get("task_context") or {}
        market_context = state.get("market_context") or {}
        source_task_context = state.get("source_task_context") or {}
        pending_review_count = market_context.get("pendingReviewCount") or 0
        review_pressure = "HIGH" if pending_review_count >= 3 else "MEDIUM" if pending_review_count >= 1 else "LOW"
        source_report = source_task_context.get("report") or {}
        focus_dimensions = build_focus_dimensions(task_type, analysis_scope)
        if source_report.get("reportId") and "SOURCE_REPORT" not in focus_dimensions:
            focus_dimensions.append("SOURCE_REPORT")
        if market_context.get("latestInsightSummary") and "PLATFORM_INSIGHT" not in focus_dimensions:
            focus_dimensions.append("PLATFORM_INSIGHT")

        fallback_intent_result = {
            "analysisMode": analysis_scope,
            "focusDimensions": focus_dimensions,
            "sourceDomain": source_context.get("sourceDomain") or "UNKNOWN",
            "contextSource": task_context.get("contextSource"),
            "sourceReportId": source_context.get("sourceReportId") or source_report.get("reportId"),
            "latestInsightReportId": market_context.get("latestInsightReportId"),
            "pendingReviewCount": pending_review_count,
            "reviewPressure": review_pressure,
        }
        model_result, llm_framework, model_name, fallback_reason = self._generate_intent_result(
            state=state,
            fallback_result=fallback_intent_result,
        )

        state["current_stage"] = "INTENT_UNDERSTANDING"
        state["current_node"] = "intent_agent"
        state["progress"] = 40
        state["intent_result"] = {
            "analysisMode": self._normalize_text(
                model_result.get("analysisMode") if model_result else None
            ) or fallback_intent_result["analysisMode"],
            "focusDimensions": self._resolve_text_list(
                model_result.get("focusDimensions") if model_result else None,
                fallback_intent_result["focusDimensions"],
            ),
            "sourceDomain": self._normalize_text(
                model_result.get("sourceDomain") if model_result else None
            ) or fallback_intent_result["sourceDomain"],
            "contextSource": self._normalize_optional_text(
                model_result.get("contextSource") if model_result else None
            ) or fallback_intent_result["contextSource"],
            "sourceReportId": self._normalize_optional_text(
                model_result.get("sourceReportId") if model_result else None
            ) or fallback_intent_result["sourceReportId"],
            "latestInsightReportId": self._normalize_optional_text(
                model_result.get("latestInsightReportId") if model_result else None
            ) or fallback_intent_result["latestInsightReportId"],
            "pendingReviewCount": self._resolve_int(
                model_result.get("pendingReviewCount") if model_result else None,
                fallback_intent_result["pendingReviewCount"],
            ),
            "reviewPressure": self._normalize_text(
                model_result.get("reviewPressure") if model_result else None
            ) or fallback_intent_result["reviewPressure"],
            "generationMode": "MODEL_ASSISTED" if model_result else "RULE_FALLBACK",
            "llmFramework": llm_framework,
            "modelName": model_name,
            "fallbackReason": fallback_reason,
        }
        state.setdefault("agent_audits", []).append({
            "executionId": f"exec-{state['task_id']}-intent",
            "agentCode": "intent_agent",
            "agentName": "Intent Agent",
            "nodeCode": "intent_agent",
            "status": "SUCCESS",
            "confidenceScore": 0.95,
            "needHumanReview": False,
            "startTimestamp": now,
            "finishTimestamp": now,
            "durationMs": 0
        })
        return state

    def _generate_intent_result(
        self,
        *,
        state: dict,
        fallback_result: dict,
    ) -> tuple[dict | None, str | None, str | None, str | None]:
        if not self.langchain_intent_service.is_enabled():
            reason = self.langchain_intent_service.availability_reason() or "LANGCHAIN_DISABLED"
            log_info(state.get("trace_id", ""), f"[AI-ENGINE][LANGCHAIN] intent fallback to rule reason={reason}")
            return None, None, None, reason

        result = self.langchain_intent_service.generate_intent_result(
            state=state,
            fallback_result=fallback_result,
        )
        if not isinstance(result, dict):
            reason = "LANGCHAIN_NO_RESULT"
            log_info(state.get("trace_id", ""), f"[AI-ENGINE][LANGCHAIN] intent fallback to rule reason={reason}")
            return None, None, None, reason
        return (
            result,
            self.langchain_intent_service.framework_name(),
            self.langchain_intent_service.model_name(),
            None,
        )

    def _normalize_text(self, value) -> str:
        if value is None:
            return ""
        return str(value).strip()

    def _normalize_optional_text(self, value) -> str | None:
        normalized = self._normalize_text(value)
        return normalized or None

    def _resolve_text_list(self, preferred, fallback: list[str]) -> list[str]:
        if not isinstance(preferred, list):
            return fallback
        normalized = [str(item).strip() for item in preferred if str(item).strip()]
        return normalized or fallback

    def _resolve_int(self, preferred, fallback: int) -> int:
        if isinstance(preferred, bool):
            return fallback
        if isinstance(preferred, int):
            return preferred
        if isinstance(preferred, float):
            return int(preferred)
        if isinstance(preferred, str):
            try:
                return int(preferred.strip())
            except ValueError:
                return fallback
        return fallback
