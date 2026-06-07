import time
from typing import Any

from app.services.task_control_service import TaskControlService


class StrategyReasoningAgent:
    def __init__(self):
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        evidence_items = self._normalize_evidence_items(state.get("evidence_items"))
        risk_result = state.get("risk_result") or {}
        financial_result = state.get("financial_result") or {}
        industry_result = state.get("industry_research_result") or {}
        event_result = state.get("event_extraction_result") or {}
        market_context = state.get("market_context") or {}

        risk_points = self._normalize_text_list(risk_result.get("riskPoints"))
        risk_warnings = self._normalize_text_list(risk_result.get("riskWarnings"))
        financial_summary = self._normalize_text(financial_result.get("summary"))
        industry_drivers = self._normalize_text_list(industry_result.get("industryDrivers"))
        event_themes = self._normalize_text_list(event_result.get("eventThemes"))

        confidence = self._resolve_confidence(
            financial_result.get("confidenceScore"),
            risk_result.get("generationMode"),
            bool(risk_result.get("needHumanReview")),
            risk_points,
            evidence_items,
        )
        direction = self._resolve_direction(confidence, risk_points, risk_warnings, bool(risk_result.get("needHumanReview")))
        factors = self._build_factors(
            confidence=confidence,
            financial_summary=financial_summary,
            industry_drivers=industry_drivers,
            event_themes=event_themes,
            risk_points=risk_points,
            evidence_items=evidence_items,
            market_context=market_context,
        )
        evidence = self._build_strategy_evidence(evidence_items, market_context)

        strategy_candidate = {
            "candidateType": "AI_STRATEGY_SIGNAL_CANDIDATE",
            "direction": direction,
            "summary": self._build_summary(
                target_name=state.get("target_name") or state.get("target_code") or "target",
                direction=direction,
                financial_summary=financial_summary,
                risk_points=risk_points,
                industry_drivers=industry_drivers,
            ),
            "confidence": confidence,
            "factors": factors,
            "evidence": evidence,
            "trace": {
                "taskId": state.get("task_id"),
                "traceId": state.get("trace_id"),
                "source": "strategy_reasoning_agent",
                "authority": "CANDIDATE_ONLY",
            },
        }

        state["current_stage"] = "STRATEGY_REASONING"
        state["current_node"] = "strategy_reasoning_agent"
        state["progress"] = 88
        state["strategy_result"] = strategy_candidate
        state.setdefault("agent_audits", []).append(
            {
                "executionId": f"exec-{state['task_id']}-strategy",
                "agentCode": "strategy_reasoning_agent",
                "agentName": "Strategy Reasoning Agent",
                "nodeCode": "strategy_reasoning_agent",
                "status": "SUCCESS",
                "confidenceScore": confidence,
                "needHumanReview": False,
                "startTimestamp": now,
                "finishTimestamp": now,
                "durationMs": 0,
            }
        )
        return state

    def _build_summary(
        self,
        *,
        target_name: str,
        direction: str,
        financial_summary: str,
        risk_points: list[str],
        industry_drivers: list[str],
    ) -> str:
        direction_text = {
            "POSITIVE": "positive candidate",
            "NEGATIVE": "defensive candidate",
            "NEUTRAL": "watchlist candidate",
        }.get(direction, "watchlist candidate")
        anchors = []
        if financial_summary:
            anchors.append(financial_summary)
        anchors.extend(industry_drivers[:1])
        anchors.extend(risk_points[:1])
        suffix = " / ".join(anchors[:3])
        if suffix:
            return f"{target_name} strategy signal is a {direction_text}; evidence anchor: {suffix}"
        return f"{target_name} strategy signal is a {direction_text} based on current workflow evidence."

    def _build_factors(
        self,
        *,
        confidence: float,
        financial_summary: str,
        industry_drivers: list[str],
        event_themes: list[str],
        risk_points: list[str],
        evidence_items: list[dict[str, Any]],
        market_context: dict[str, Any],
    ) -> list[dict[str, Any]]:
        factors: list[dict[str, Any]] = [
            {
                "factorCode": "MODEL_CONFIDENCE",
                "factorName": "Model confidence",
                "factorValue": f"{confidence:.2f}",
                "factorWeight": 0.35,
                "factorConclusion": "Candidate confidence derived from upstream analysis quality and review pressure.",
                "evidenceRefs": self._top_evidence_refs(evidence_items, 2),
            }
        ]
        if financial_summary:
            factors.append({
                "factorCode": "FINANCIAL_SIGNAL",
                "factorName": "Financial signal",
                "factorValue": financial_summary[:120],
                "factorWeight": 0.25,
                "factorConclusion": financial_summary,
                "evidenceRefs": self._evidence_refs_by_type(evidence_items, "LATEST_INSIGHT", "MARKET_INTELLIGENCE"),
            })
        if industry_drivers or event_themes:
            factors.append({
                "factorCode": "INDUSTRY_EVENT_CONTEXT",
                "factorName": "Industry/event context",
                "factorValue": " / ".join([*industry_drivers[:1], *event_themes[:1]])[:120],
                "factorWeight": 0.2,
                "factorConclusion": "Candidate reflects industry drivers and extracted event themes.",
                "evidenceRefs": self._evidence_refs_by_type(evidence_items, "SOURCE_EVENT", "MARKET_EVENT", "LIVE_MARKET_EVENT"),
            })
        if risk_points:
            factors.append({
                "factorCode": "RISK_ADJUSTMENT",
                "factorName": "Risk adjustment",
                "factorValue": str(len(risk_points)),
                "factorWeight": 0.2,
                "factorConclusion": risk_points[0],
                "evidenceRefs": self._evidence_refs_by_type(evidence_items, "RISK_WARNING", "REGULATORY_RISK_LIVE_EVENT"),
            })
        if market_context.get("latestInsightReportId"):
            factors.append({
                "factorCode": "WORKBENCH_CONTEXT",
                "factorName": "Workbench context",
                "factorValue": str(market_context.get("latestInsightReportId")),
                "factorWeight": 0.1,
                "factorConclusion": "Workbench context is display context only and does not become signal authority.",
                "evidenceRefs": [f"latestInsightReport:{market_context.get('latestInsightReportId')}"],
            })
        return factors[:5]

    def _build_strategy_evidence(
        self,
        evidence_items: list[dict[str, Any]],
        market_context: dict[str, Any],
    ) -> list[dict[str, Any]]:
        evidence = []
        for item in evidence_items[:6]:
            evidence.append({
                "evidenceId": self._normalize_text(item.get("evidenceId")),
                "evidenceType": self._normalize_text(item.get("evidenceType")),
                "title": self._normalize_text(item.get("title")),
                "summary": self._normalize_text(item.get("summary")),
                "referenceId": self._normalize_text(item.get("referenceId")),
                "relevance": self._normalize_text(item.get("relevance")),
            })
        if market_context.get("dataSource"):
            evidence.append({
                "evidenceId": f"marketData:{market_context.get('dataSource')}",
                "evidenceType": "MARKET_CONTEXT",
                "title": "Market context data source",
                "summary": str(market_context.get("dataSource")),
                "referenceId": str(market_context.get("dataSource")),
                "relevance": "MEDIUM",
            })
        return evidence[:8]

    def _resolve_direction(
        self,
        confidence: float,
        risk_points: list[str],
        risk_warnings: list[str],
        need_human_review: bool,
    ) -> str:
        if need_human_review or risk_warnings or len(risk_points) >= 3:
            return "NEGATIVE"
        if confidence >= 0.82 and not risk_points:
            return "POSITIVE"
        return "NEUTRAL"

    def _resolve_confidence(
        self,
        financial_score: Any,
        risk_generation_mode: Any,
        need_human_review: bool,
        risk_points: list[str],
        evidence_items: list[dict[str, Any]],
    ) -> float:
        if isinstance(financial_score, (int, float)):
            base = float(financial_score)
        else:
            base = 0.78
        if str(risk_generation_mode or "").upper() == "RULE_FALLBACK":
            base -= 0.04
        if need_human_review:
            base -= 0.1
        base -= min(0.15, len(risk_points) * 0.03)
        if evidence_items:
            base += min(0.08, len(evidence_items) * 0.01)
        return round(max(0.1, min(0.98, base)), 4)

    def _top_evidence_refs(self, evidence_items: list[dict[str, Any]], limit: int) -> list[str]:
        refs = []
        for item in evidence_items:
            ref = self._normalize_text(item.get("evidenceId")) or self._normalize_text(item.get("referenceId"))
            if ref and ref not in refs:
                refs.append(ref)
            if len(refs) >= limit:
                break
        return refs

    def _evidence_refs_by_type(self, evidence_items: list[dict[str, Any]], *types: str) -> list[str]:
        wanted = {item.upper() for item in types}
        refs = []
        for item in evidence_items:
            evidence_type = self._normalize_text(item.get("evidenceType")).upper()
            if evidence_type not in wanted:
                continue
            ref = self._normalize_text(item.get("evidenceId")) or self._normalize_text(item.get("referenceId"))
            if ref and ref not in refs:
                refs.append(ref)
        return refs[:3]

    def _normalize_evidence_items(self, value: Any) -> list[dict[str, Any]]:
        if not isinstance(value, list):
            return []
        return [item for item in value if isinstance(item, dict)]

    def _normalize_text_list(self, value: Any) -> list[str]:
        if not isinstance(value, list):
            return []
        result = []
        for item in value:
            text = self._normalize_text(item)
            if text:
                result.append(text)
        return result

    def _normalize_text(self, value: Any) -> str:
        if value is None:
            return ""
        return str(value).strip()
