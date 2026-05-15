import time

from app.services.langchain_planner_service import LangChainPlannerService
from app.services.task_control_service import TaskControlService
from app.utils.logger import log_info


class PlannerAgent:
    def __init__(self):
        self.langchain_planner_service = LangChainPlannerService()
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)
        task_context = state.get("task_context") or {}
        source_task_context = state.get("source_task_context") or {}
        market_context = state.get("market_context") or {}
        analysis_scope = state.get("analysis_scope") or ""
        task_type = state.get("task_type") or ""

        source_report = source_task_context.get("report") or {}
        context_ready = bool(task_context.get("contextLoaded"))
        has_source_report = bool(source_report.get("reportId"))
        has_market_snapshot = bool(market_context.get("dataSource"))
        planning_mode = "CONTEXT_ENRICHED" if context_ready or has_market_snapshot else "DISPATCH_ONLY"

        execution_focus = ["TASK_DETAIL", "FINANCIAL_BASELINE", "RISK_SCREENING"]
        if has_source_report:
            execution_focus.append("SOURCE_REPORT_VALIDATION")
        if analysis_scope:
            execution_focus.append(f"SCOPE:{analysis_scope}")
        if task_type:
            execution_focus.append(f"TASK:{task_type}")

        fallback_plan_result = {
            "planningMode": planning_mode,
            "contextReady": context_ready,
            "sourceReportAvailable": has_source_report,
            "marketSnapshotReady": has_market_snapshot,
            "executionFocus": execution_focus,
        }
        model_result, llm_framework, model_name, fallback_reason = self._generate_plan_result(
            state=state,
            fallback_result=fallback_plan_result,
        )

        state["current_stage"] = "PLANNING"
        state["current_node"] = "planner_agent"
        state["progress"] = 10
        state["plan_result"] = {
            "planningMode": self._normalize_text(
                model_result.get("planningMode") if model_result else None
            ) or fallback_plan_result["planningMode"],
            "contextReady": self._resolve_bool(
                model_result.get("contextReady") if model_result else None,
                fallback_plan_result["contextReady"],
            ),
            "sourceReportAvailable": self._resolve_bool(
                model_result.get("sourceReportAvailable") if model_result else None,
                fallback_plan_result["sourceReportAvailable"],
            ),
            "marketSnapshotReady": self._resolve_bool(
                model_result.get("marketSnapshotReady") if model_result else None,
                fallback_plan_result["marketSnapshotReady"],
            ),
            "executionFocus": self._resolve_text_list(
                model_result.get("executionFocus") if model_result else None,
                fallback_plan_result["executionFocus"],
            ),
            "generationMode": "MODEL_ASSISTED" if model_result else "RULE_FALLBACK",
            "llmFramework": llm_framework,
            "modelName": model_name,
            "fallbackReason": fallback_reason,
        }
        state.setdefault("agent_audits", []).append({
            "executionId": f"exec-{state['task_id']}-planner",
            "agentCode": "planner_agent",
            "agentName": "Planner Agent",
            "nodeCode": "planner_agent",
            "status": "SUCCESS",
            "confidenceScore": 0.99,
            "needHumanReview": False,
            "startTimestamp": now,
            "finishTimestamp": now,
            "durationMs": 0
        })
        return state

    def _generate_plan_result(
        self,
        *,
        state: dict,
        fallback_result: dict,
    ) -> tuple[dict | None, str | None, str | None, str | None]:
        if not self.langchain_planner_service.is_enabled():
            reason = self.langchain_planner_service.availability_reason() or "LANGCHAIN_DISABLED"
            log_info(state.get("trace_id", ""), f"[AI-ENGINE][LANGCHAIN] planner fallback to rule reason={reason}")
            return None, None, None, reason

        result = self.langchain_planner_service.generate_plan_result(
            state=state,
            fallback_result=fallback_result,
        )
        if not isinstance(result, dict):
            reason = "LANGCHAIN_NO_RESULT"
            log_info(state.get("trace_id", ""), f"[AI-ENGINE][LANGCHAIN] planner fallback to rule reason={reason}")
            return None, None, None, reason
        return (
            result,
            self.langchain_planner_service.framework_name(),
            self.langchain_planner_service.model_name(),
            None,
        )

    def _normalize_text(self, value) -> str:
        if value is None:
            return ""
        return str(value).strip()

    def _resolve_bool(self, preferred, fallback: bool) -> bool:
        if isinstance(preferred, bool):
            return preferred
        if isinstance(preferred, str):
            normalized = preferred.strip().lower()
            if normalized in {"true", "1", "yes"}:
                return True
            if normalized in {"false", "0", "no"}:
                return False
        return fallback

    def _resolve_text_list(self, preferred, fallback: list[str]) -> list[str]:
        if not isinstance(preferred, list):
            return fallback
        normalized = [str(item).strip() for item in preferred if str(item).strip()]
        return normalized or fallback
