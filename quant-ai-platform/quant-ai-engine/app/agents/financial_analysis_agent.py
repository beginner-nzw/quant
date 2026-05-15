import time
from typing import Any

from app.clients.model_client import ModelClient
from app.common.task_routing import (
    ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP,
    TASK_TYPE_AUDIT_REVIEW,
    TASK_TYPE_FOLLOW_UP_RESEARCH,
    TASK_TYPE_REPORT_REVIEW,
)
from app.services.langchain_financial_service import LangChainFinancialService
from app.services.market_data_service import MarketDataService
from app.services.prompt_builder_service import PromptBuilderService
from app.services.task_control_service import TaskControlService
from app.utils.logger import log_error


class FinancialAnalysisAgent:
    def __init__(self):
        self.langchain_financial_service = LangChainFinancialService()
        self.model_client = ModelClient()
        self.market_data_service = MarketDataService()
        self.prompt_builder_service = PromptBuilderService()
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        if state["target_code"] == "FAIL001":
            raise RuntimeError(
                "财务分析阶段触发预设失败分支，请检查测试标的配置"
            )
        if state["target_code"] == "TIMEOUT001":
            time.sleep(15)

        task_type = state.get("task_type", "")
        analysis_scope = state.get("analysis_scope", "")
        target_name = state.get("target_name") or "该标的"
        financial_data = state.get("market_context") or {}
        if not financial_data.get("dataSource"):
            financial_data = self.market_data_service.load_financial_data(
                target_code=state["target_code"],
                target_name=target_name,
                target_type=state.get("target_type", ""),
                trace_id=state.get("trace_id", ""),
            )

        state["current_stage"] = "FINANCIAL_ANALYSIS"
        state["current_node"] = "financial_analysis_agent"
        state["progress"] = 65
        state["market_context"] = financial_data
        fallback_result = {
            "revenueTrend": financial_data["revenueTrend"],
            "profitTrend": financial_data["profitTrend"],
            "cashflowSignal": financial_data["cashflowSignal"],
            "dataSource": financial_data.get("dataSource"),
            "latestInsightSummary": financial_data.get("latestInsightSummary"),
            "latestHighlights": financial_data.get("latestHighlights") or [],
            "latestRiskPoints": financial_data.get("latestRiskPoints") or [],
            "summary": self._build_summary(
                target_name=target_name,
                task_type=task_type,
                analysis_scope=analysis_scope,
                financial_data=financial_data,
                source_task_context=state.get("source_task_context") or {},
            ),
        }
        model_result, llm_framework, model_name = self._generate_model_result(state, fallback_result)
        state["financial_result"] = {
            "revenueTrend": self._resolve_enum(
                model_result.get("revenueTrend") if model_result else None,
                {"UP", "DOWN", "STABLE", "PRESSURED"},
                fallback_result["revenueTrend"],
            ),
            "profitTrend": self._resolve_enum(
                model_result.get("profitTrend") if model_result else None,
                {"UP", "DOWN", "STABLE"},
                fallback_result["profitTrend"],
            ),
            "cashflowSignal": self._resolve_enum(
                model_result.get("cashflowSignal") if model_result else None,
                {"NORMAL", "WATCH", "PRESSURED"},
                fallback_result["cashflowSignal"],
            ),
            "dataSource": financial_data.get("dataSource"),
            "latestInsightSummary": financial_data.get("latestInsightSummary"),
            "latestHighlights": financial_data.get("latestHighlights") or [],
            "latestRiskPoints": financial_data.get("latestRiskPoints") or [],
            "summary": self._normalize_text(
                model_result.get("summary") if model_result else None
            ) or fallback_result["summary"],
            "generationMode": "MODEL_ASSISTED" if model_result else "RULE_FALLBACK",
            "llmFramework": llm_framework,
            "modelName": model_name,
        }
        audit_confidence = 0.89 if model_result else 0.86
        state.setdefault("agent_audits", []).append(
            {
                "executionId": f"exec-{state['task_id']}-financial",
                "agentCode": "financial_analysis_agent",
                "agentName": "Financial Analysis Agent",
                "nodeCode": "financial_analysis_agent",
                "status": "SUCCESS",
                "confidenceScore": audit_confidence,
                "needHumanReview": task_type in {TASK_TYPE_AUDIT_REVIEW, TASK_TYPE_REPORT_REVIEW},
                "startTimestamp": now,
                "finishTimestamp": now,
                "durationMs": 0,
            }
        )
        return state

    def _build_summary(
        self,
        target_name: str,
        task_type: str,
        analysis_scope: str,
        financial_data: dict[str, Any],
        source_task_context: dict[str, Any],
    ) -> str:
        trend_summary = (
            f"收入趋势 {financial_data.get('revenueTrend', 'STABLE')}，"
            f"利润趋势 {financial_data.get('profitTrend', 'STABLE')}，"
            f"现金流信号 {financial_data.get('cashflowSignal', 'NORMAL')}。"
        )
        latest_insight_summary = self._normalize_text(financial_data.get("latestInsightSummary"))
        latest_strategy_signal_summary = self._normalize_text(
            financial_data.get("latestStrategySignalSummary")
        )
        latest_market_intelligence_summary = self._normalize_text(
            financial_data.get("latestMarketIntelligenceSummary")
        )
        source_report_summary = self._extract_source_report_summary(source_task_context)
        supporting_summary = (
            latest_insight_summary
            or latest_market_intelligence_summary
            or latest_strategy_signal_summary
        )

        if task_type == TASK_TYPE_REPORT_REVIEW:
            base = (
                f"{target_name} 的报告复核已补充平台最新投研上下文，"
                "可用于重新判断原报告结论是否充分。"
            )
            if source_report_summary:
                return f"{base}原报告摘要：{source_report_summary}{trend_summary}"
            return f"{base}{trend_summary}"

        if task_type == TASK_TYPE_AUDIT_REVIEW:
            return (
                f"{target_name} 的审计复核已结合平台近期投研记录，"
                "可用于核对合规留痕与异常点。"
                f"{trend_summary}"
            )

        if task_type == TASK_TYPE_FOLLOW_UP_RESEARCH and analysis_scope == ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP:
            base = (
                f"{target_name} 的信号跟踪已补充平台近期任务和报告结果，"
                "用于确认策略信号是否仍具持续性。"
            )
            if supporting_summary:
                return f"{base}最新洞察：{supporting_summary}{trend_summary}"
            return f"{base}{trend_summary}"

        if task_type == TASK_TYPE_FOLLOW_UP_RESEARCH:
            base = (
                f"{target_name} 的跟踪研究已更新当前平台聚合视图，"
                "可结合最近任务变化继续观察。"
            )
            if supporting_summary:
                return f"{base}最新洞察：{supporting_summary}{trend_summary}"
            return f"{base}{trend_summary}"

        base = f"{target_name} 当前财务走势分析已结合平台最新任务与报告快照。"
        if supporting_summary:
            return f"{base}最新洞察：{supporting_summary}{trend_summary}"
        return f"{base}{trend_summary}"

    def _extract_source_report_summary(self, source_task_context: dict[str, Any]) -> str:
        report = source_task_context.get("report") or {}
        for key in ("displaySummary", "summary", "originalSummary"):
            value = self._normalize_text(report.get(key))
            if value:
                return value
        return ""

    def _generate_model_result(
        self,
        state: dict[str, Any],
        fallback_result: dict[str, Any],
    ) -> tuple[dict[str, Any] | None, str | None, str | None]:
        if self.langchain_financial_service.is_enabled():
            langchain_result = self.langchain_financial_service.generate_financial_result(
                state=state,
                fallback_result=fallback_result,
            )
            if isinstance(langchain_result, dict):
                return (
                    langchain_result,
                    self.langchain_financial_service.framework_name(),
                    self.langchain_financial_service.model_name(),
                )

        if not self.model_client.is_enabled("financial"):
            return None, None, None

        system_prompt, user_prompt = self.prompt_builder_service.build_financial_prompts(
            state=state,
            fallback_result=fallback_result,
        )
        model_result = self.model_client.generate_json_object(
            scene="financial",
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            trace_id=state.get("trace_id", ""),
        )
        if not isinstance(model_result, dict):
            return None, None, None
        if not self._normalize_text(model_result.get("summary")):
            log_error(state.get("trace_id", ""), "[AI-ENGINE][MODEL] financial summary missing, fallback applied")
            return None, None, None
        return model_result, "custom-http", self.model_client.model_name("financial")

    def _resolve_enum(self, preferred: Any, allowed: set[str], fallback: str) -> str:
        normalized = self._normalize_text(preferred).upper() if preferred is not None else ""
        if normalized in allowed:
            return normalized
        return fallback

    def _normalize_text(self, value: Any) -> str:
        if value is None:
            return ""
        normalized = str(value).strip()
        return normalized
