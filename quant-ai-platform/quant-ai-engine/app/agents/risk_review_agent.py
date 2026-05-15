import time
from typing import Any

from app.clients.model_client import ModelClient
from app.common.task_routing import (
    ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP,
    TASK_TYPE_AUDIT_REVIEW,
    TASK_TYPE_FOLLOW_UP_RESEARCH,
    TASK_TYPE_REPORT_REVIEW,
    TASK_TYPE_RISK_REVIEW,
)
from app.services.langchain_risk_service import LangChainRiskService
from app.services.prompt_builder_service import PromptBuilderService
from app.services.task_control_service import TaskControlService
from app.utils.logger import log_error


class RiskReviewAgent:
    def __init__(self):
        self.langchain_risk_service = LangChainRiskService()
        self.model_client = ModelClient()
        self.prompt_builder_service = PromptBuilderService()
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        task_type = state.get("task_type", "")
        analysis_scope = state.get("analysis_scope", "")
        source_domain = state.get("source_context", {}).get("sourceDomain") or ""
        market_context = state.get("market_context") or {}
        source_task_context = state.get("source_task_context") or {}

        fallback_risk_result = self._build_risk_result(
            task_type=task_type,
            analysis_scope=analysis_scope,
            source_domain=source_domain,
            market_context=market_context,
            source_task_context=source_task_context,
        )
        model_risk_result, llm_framework, model_name = self._generate_model_result(state, fallback_risk_result)
        risk_result = {
            "riskLevel": self._resolve_enum(
                model_risk_result.get("riskLevel") if model_risk_result else None,
                {"HIGH", "MEDIUM", "LOW"},
                fallback_risk_result["riskLevel"],
            ),
            "riskPoints": self._resolve_text_list(
                model_risk_result.get("riskPoints") if model_risk_result else None,
                fallback_risk_result["riskPoints"],
            ),
            "riskWarnings": self._resolve_text_list(
                model_risk_result.get("riskWarnings") if model_risk_result else None,
                fallback_risk_result["riskWarnings"],
            ),
            "needHumanReview": self._resolve_bool(
                model_risk_result.get("needHumanReview") if model_risk_result else None,
                bool(fallback_risk_result.get("needHumanReview")),
            ),
            "generationMode": "MODEL_ASSISTED" if model_risk_result else "RULE_FALLBACK",
            "llmFramework": llm_framework,
            "modelName": model_name,
        }
        risk_result = self._ensure_priority_external_risk_context(
            risk_result=risk_result,
            market_context=market_context,
        )

        state["current_stage"] = "RISK_REVIEW"
        state["current_node"] = "risk_review_agent"
        state["progress"] = 80
        state["risk_result"] = risk_result
        state["need_human_review"] = bool(risk_result.get("needHumanReview"))
        audit_confidence = 0.87 if model_risk_result else 0.84
        state.setdefault("agent_audits", []).append(
            {
                "executionId": f"exec-{state['task_id']}-risk",
                "agentCode": "risk_review_agent",
                "agentName": "Risk Review Agent",
                "nodeCode": "risk_review_agent",
                "status": "SUCCESS",
                "confidenceScore": audit_confidence,
                "needHumanReview": bool(risk_result.get("needHumanReview")),
                "startTimestamp": now,
                "finishTimestamp": now,
                "durationMs": 0,
            }
        )
        return state

    def _build_risk_result(
        self,
        *,
        task_type: str,
        analysis_scope: str,
        source_domain: str,
        market_context: dict[str, Any],
        source_task_context: dict[str, Any],
    ) -> dict[str, Any]:
        pending_review_count = int(market_context.get("pendingReviewCount") or 0)
        failed_task_count = int(market_context.get("failedTaskCount") or 0)
        source_report = source_task_context.get("report") or {}
        source_review_status = str(source_report.get("reviewStatus") or "").upper()
        latest_risk_points = self._normalize_text_list(market_context.get("latestRiskPoints"))
        latest_risk_warning_summary = self._extract_first_item_text(
            market_context.get("riskWarnings"),
            "summary",
        )
        latest_strategy_signal_summary = self._extract_first_item_text(
            market_context.get("strategySignals"),
            "strategySummary",
            "backtestSummary",
        )
        latest_market_intelligence_summary = self._extract_first_item_text(
            market_context.get("marketIntelligence"),
            "summary",
        )
        latest_regulatory_risk_live_event = self._extract_priority_live_event_text(
            market_context.get("regulatoryRiskLiveEvents"),
            market_context.get("regulatoryRiskLiveEventHighlights"),
        )
        latest_policy_live_event = self._extract_priority_live_event_text(
            market_context.get("policyLiveEvents"),
            market_context.get("policyLiveEventHighlights"),
        )

        if task_type == TASK_TYPE_RISK_REVIEW:
            risk_points = [
                "来源风险预警触发后仍存在高暴露项，需要确认事件是否已经解除。",
                "需要复核关键风险触发条件与最新市场反馈是否一致。",
            ]
            risk_points.extend(latest_risk_points[:1])
            if latest_risk_warning_summary:
                risk_points.append(f"平台最新风险预警提示：{latest_risk_warning_summary}")
            if latest_regulatory_risk_live_event:
                risk_points.append(f"监管风险事件提示：{latest_regulatory_risk_live_event}")
            return {
                "riskLevel": "HIGH",
                "riskPoints": risk_points,
                "riskWarnings": [
                    "高风险预警待复核",
                    "建议补充处置计划和关键时间点",
                ],
                "needHumanReview": True,
            }

        if task_type == TASK_TYPE_AUDIT_REVIEW:
            risk_points = [
                "审计留痕与报告内容仍有待核对字段，需要确认版本与口径一致。",
                "需要补查来源链路和关键操作说明，避免合规判断出现缺口。",
            ]
            if pending_review_count > 0:
                risk_points.append(f"当前同标的仍有 {pending_review_count} 份报告待审核，需要确认结论是否一致。")
            return {
                "riskLevel": "MEDIUM",
                "riskPoints": risk_points,
                "riskWarnings": [
                    "合规留痕待补充",
                ],
                "needHumanReview": True,
            }

        if task_type == TASK_TYPE_REPORT_REVIEW:
            risk_points = [
                "原报告结论与最新证据之间仍存在待解释差异。",
                "需要确认复核后的摘要是否完整覆盖关键风险点。",
            ]
            if source_review_status == "REJECTED":
                risk_points.append("来源报告曾被驳回，需要重点复核结论修正是否充分。")
            return {
                "riskLevel": "MEDIUM",
                "riskPoints": risk_points,
                "riskWarnings": [
                    "报告复核后建议再次进入审核流程",
                ],
                "needHumanReview": True,
            }

        if task_type == TASK_TYPE_FOLLOW_UP_RESEARCH and analysis_scope == ANALYSIS_SCOPE_SIGNAL_FOLLOW_UP:
            risk_points = [
                "策略信号可能受到短期市场噪声影响，需要继续验证持续性。",
                "需要观察信号与基本面变化是否保持同步。",
            ]
            if latest_strategy_signal_summary:
                risk_points.append(f"最新策略信号摘要：{latest_strategy_signal_summary}")
            if failed_task_count > 0:
                risk_points.append(f"相同标的最近有 {failed_task_count} 条失败任务，需要注意结论稳定性。")
            if latest_regulatory_risk_live_event:
                risk_points.append(f"监管风险事件提示：{latest_regulatory_risk_live_event}")
            return {
                "riskLevel": "MEDIUM",
                "riskPoints": risk_points,
                "riskWarnings": [
                    "信号持续性待观察",
                ],
                "needHumanReview": False,
            }

        if task_type == TASK_TYPE_FOLLOW_UP_RESEARCH:
            risk_points = [
                "跟踪研究已发现新的观察点，需要结合后续披露继续验证。",
            ]
            if source_domain == "MARKET_INTELLIGENCE":
                risk_points.append("情报来源变化较快，建议持续关注后续事件更新。")
            if latest_market_intelligence_summary:
                risk_points.append(f"近期市场情报提示：{latest_market_intelligence_summary}")
            if latest_policy_live_event:
                risk_points.append(f"最新政策事件提示：{latest_policy_live_event}")
            if pending_review_count > 0:
                risk_points.append(f"同标的仍有 {pending_review_count} 份待审核报告，建议合并审阅后再固化跟踪结论。")
            return {
                "riskLevel": "LOW",
                "riskPoints": risk_points,
                "riskWarnings": [],
                "needHumanReview": False,
            }

        risk_points = [
            "深度研究阶段已识别行业波动和经营不确定性。",
            "需要持续跟踪市场情绪与经营数据变化。",
        ]
        if latest_risk_points:
            risk_points.extend(latest_risk_points[:1])
        if latest_risk_warning_summary:
            risk_points.append(f"平台风险预警提示：{latest_risk_warning_summary}")
        elif latest_market_intelligence_summary:
            risk_points.append(f"平台市场情报提示：{latest_market_intelligence_summary}")
        if latest_regulatory_risk_live_event:
            risk_points.append(f"监管风险事件提示：{latest_regulatory_risk_live_event}")
        if latest_policy_live_event:
            risk_points.append(f"政策事件提示：{latest_policy_live_event}")
        return {
            "riskLevel": "MEDIUM",
            "riskPoints": risk_points,
            "riskWarnings": [],
            "needHumanReview": False,
        }

    def _generate_model_result(
        self,
        state: dict[str, Any],
        fallback_result: dict[str, Any],
    ) -> tuple[dict[str, Any] | None, str | None, str | None]:
        if self.langchain_risk_service.is_enabled():
            langchain_result = self.langchain_risk_service.generate_risk_result(
                state=state,
                fallback_result=fallback_result,
            )
            if isinstance(langchain_result, dict):
                return (
                    langchain_result,
                    self.langchain_risk_service.framework_name(),
                    self.langchain_risk_service.model_name(),
                )

        if not self.model_client.is_enabled("risk"):
            return None, None, None

        system_prompt, user_prompt = self.prompt_builder_service.build_risk_prompts(
            state=state,
            fallback_result=fallback_result,
        )
        model_result = self.model_client.generate_json_object(
            scene="risk",
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            trace_id=state.get("trace_id", ""),
        )
        if not isinstance(model_result, dict):
            return None, None, None
        if not self._normalize_text_list(model_result.get("riskPoints")):
            log_error(state.get("trace_id", ""), "[AI-ENGINE][MODEL] risk points missing, fallback applied")
            return None, None, None
        return model_result, "custom-http", self.model_client.model_name("risk")

    def _normalize_text_list(self, value: Any) -> list[str]:
        if not isinstance(value, list):
            return []
        normalized: list[str] = []
        for item in value:
            if item is None:
                continue
            text = str(item).strip()
            if text:
                normalized.append(text)
        return normalized

    def _resolve_text_list(self, preferred: Any, fallback: list[str]) -> list[str]:
        normalized = self._normalize_text_list(preferred)
        return normalized or fallback

    def _resolve_enum(self, preferred: Any, allowed: set[str], fallback: str) -> str:
        if preferred is None:
            return fallback
        normalized = str(preferred).strip().upper()
        if normalized in allowed:
            return normalized
        return fallback

    def _resolve_bool(self, preferred: Any, fallback: bool) -> bool:
        if isinstance(preferred, bool):
            return preferred
        if isinstance(preferred, str):
            normalized = preferred.strip().lower()
            if normalized in {"true", "1", "yes"}:
                return True
            if normalized in {"false", "0", "no"}:
                return False
        return fallback

    def _extract_first_item_text(self, value: Any, *keys: str) -> str:
        if not isinstance(value, list):
            return ""
        for item in value:
            if not isinstance(item, dict):
                continue
            for key in keys:
                text = str(item.get(key) or "").strip()
                if text:
                    return text
        return ""

    def _extract_priority_live_event_text(self, events: Any, highlights: Any) -> str:
        if isinstance(events, list):
            for item in events:
                if not isinstance(item, dict):
                    continue
                occurred_at = str(item.get("occurredAt") or "").strip()
                title = str(item.get("eventTitle") or item.get("title") or "").strip()
                summary = str(item.get("eventSummary") or item.get("summary") or "").strip()
                impact_level = str(item.get("impactLevel") or "").strip()
                parts = [part for part in (occurred_at, title, summary, impact_level) if part]
                if parts:
                    return " / ".join(parts)
        if isinstance(highlights, list):
            for item in highlights:
                text = str(item or "").strip()
                if text:
                    return text
        return ""

    def _ensure_priority_external_risk_context(
        self,
        *,
        risk_result: dict[str, Any],
        market_context: dict[str, Any],
    ) -> dict[str, Any]:
        result = dict(risk_result or {})
        risk_points = self._normalize_text_list(result.get("riskPoints"))
        risk_warnings = self._normalize_text_list(result.get("riskWarnings"))
        risk_level = str(result.get("riskLevel") or "MEDIUM").upper()
        need_human_review = bool(result.get("needHumanReview"))

        regulatory_risk_summary = self._extract_priority_live_event_text(
            market_context.get("regulatoryRiskLiveEvents"),
            market_context.get("regulatoryRiskLiveEventHighlights"),
        )
        policy_summary = self._extract_priority_live_event_text(
            market_context.get("policyLiveEvents"),
            market_context.get("policyLiveEventHighlights"),
        )
        if regulatory_risk_summary and all(regulatory_risk_summary not in item for item in risk_points):
            risk_points.insert(0, f"监管风险事件提示：{regulatory_risk_summary}")
            if "未命中该标的精确监管记录" not in regulatory_risk_summary:
                risk_level = "HIGH"
                need_human_review = True
                if not any("监管风险待核实" in item for item in risk_warnings):
                    risk_warnings.insert(0, "监管风险待核实")
        if policy_summary and all(policy_summary not in item for item in risk_points):
            insert_index = 1 if regulatory_risk_summary else 0
            risk_points.insert(insert_index, f"政策事件提示：{policy_summary}")

        result["riskLevel"] = risk_level
        result["riskPoints"] = risk_points[:5]
        result["riskWarnings"] = risk_warnings[:3]
        result["needHumanReview"] = need_human_review
        return result
