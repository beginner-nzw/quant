import time
from typing import Any

from app.clients.model_client import ModelClient
from app.services.prompt_builder_service import PromptBuilderService
from app.services.task_control_service import TaskControlService


class IndustryResearchAgent:
    def __init__(self):
        self.model_client = ModelClient()
        self.prompt_builder_service = PromptBuilderService()
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        state["current_stage"] = "INDUSTRY_RESEARCH"
        state["current_node"] = "industry_research_agent"
        state["progress"] = 62

        fallback_result = self._build_fallback_result(state)
        model_result, model_name, fallback_reason = self._generate_model_result(state, fallback_result)
        industry_result = {
            "industryName": self._normalize_text(model_result.get("industryName") if model_result else None)
            or fallback_result["industryName"],
            "industryDrivers": self._resolve_text_list(
                model_result.get("industryDrivers") if model_result else None,
                fallback_result["industryDrivers"],
            ),
            "industryRisks": self._resolve_text_list(
                model_result.get("industryRisks") if model_result else None,
                fallback_result["industryRisks"],
            ),
            "peerSignals": self._resolve_text_list(
                model_result.get("peerSignals") if model_result else None,
                fallback_result["peerSignals"],
            ),
            "evidence": fallback_result["evidence"],
            "provenance": fallback_result["provenance"],
            "generationMode": "MODEL_ASSISTED" if model_result else "RULE_FALLBACK",
            "fallbackReason": fallback_reason,
            "llmFramework": "custom-http" if model_result else None,
            "modelName": model_name,
        }
        state["industry_research_result"] = industry_result
        state.setdefault("agent_audits", []).append(
            {
                "executionId": f"exec-{state['task_id']}-industry-research",
                "agentCode": "industry_research_agent",
                "agentName": "Industry Research Agent",
                "nodeCode": "industry_research_agent",
                "status": "SUCCESS",
                "confidenceScore": 0.86 if model_result else 0.81,
                "needHumanReview": False,
                "startTimestamp": now,
                "finishTimestamp": now,
                "durationMs": 0,
            }
        )
        return state

    def _build_fallback_result(self, state: dict[str, Any]) -> dict[str, Any]:
        market_context = state.get("market_context") or {}
        event_result = state.get("event_extraction_result") or {}
        target_type = self._normalize_text(state.get("target_type")) or "UNKNOWN"
        industry_name = self._resolve_industry_name(state, market_context)

        drivers = []
        risks = []
        peer_signals = []
        evidence = []

        latest_insight = self._normalize_text(market_context.get("latestInsightSummary"))
        if latest_insight:
            drivers.append(latest_insight)
            evidence.append(self._build_evidence(
                evidence_id=f"latest-insight:{self._normalize_text(market_context.get('latestInsightReportId')) or 'summary'}",
                source=self._normalize_text(market_context.get("dataSource")) or "market-context",
                summary=latest_insight,
                reference_id=self._normalize_text(market_context.get("latestInsightReportId")),
            ))

        for item in (market_context.get("strategySignals") or [])[:3]:
            if not isinstance(item, dict):
                continue
            summary = self._first_text(item.get("strategySummary"), item.get("backtestSummary"))
            if summary:
                peer_signals.append(summary)

        for item in (market_context.get("riskWarnings") or [])[:3]:
            if not isinstance(item, dict):
                continue
            summary = self._first_text(item.get("summary"), self._join_text_list(item.get("riskReasons")))
            if summary:
                risks.append(summary)

        for event in (event_result.get("events") or [])[:3]:
            if not isinstance(event, dict):
                continue
            title = self._normalize_text(event.get("title"))
            impact_level = self._normalize_text(event.get("impactLevel"))
            if title and impact_level == "HIGH":
                drivers.append(f"High impact event: {title}")
            elif title:
                peer_signals.append(f"Related event: {title}")

        if not drivers:
            drivers.append(f"{target_type} target context available for industry comparison.")
        if not risks:
            risks.append("Industry risk requires validation against official filings and market data.")

        return {
            "industryName": industry_name,
            "industryDrivers": self._dedupe(drivers)[:5],
            "industryRisks": self._dedupe(risks)[:5],
            "peerSignals": self._dedupe(peer_signals)[:5],
            "evidence": evidence,
            "provenance": {
                "source": "MARKET_CONTEXT_AND_EVENT_EXTRACTION",
                "inputRefs": self._build_input_refs(state),
                "approvedPayloadFields": [
                    "industryName",
                    "industryDrivers",
                    "industryRisks",
                    "peerSignals",
                    "evidence",
                    "provenance",
                    "generationMode",
                    "fallbackReason",
                ],
            },
        }

    def _generate_model_result(
        self,
        state: dict[str, Any],
        fallback_result: dict[str, Any],
    ) -> tuple[dict[str, Any] | None, str | None, str | None]:
        if not self.model_client.is_enabled("industry_research"):
            return (
                None,
                None,
                self.model_client.availability_reason("industry_research") or "CUSTOM_HTTP_DISABLED",
            )
        system_prompt, user_prompt = self.prompt_builder_service.build_industry_research_prompts(
            state=state,
            fallback_result=fallback_result,
        )
        model_result = self.model_client.generate_json_object(
            scene="industry_research",
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            trace_id=state.get("trace_id", ""),
        )
        if not isinstance(model_result, dict):
            return None, None, "CUSTOM_HTTP_NO_RESULT"
        if not self._normalize_text(model_result.get("industryName")):
            return None, None, "CUSTOM_HTTP_EMPTY_INDUSTRY"
        return model_result, self.model_client.model_name("industry_research"), None

    def _resolve_industry_name(self, state: dict[str, Any], market_context: dict[str, Any]) -> str:
        for key in ("industryName", "industry", "sectorName", "sector"):
            value = self._normalize_text(market_context.get(key))
            if value:
                return value
        target_type = self._normalize_text(state.get("target_type")) or "UNKNOWN"
        target_name = self._normalize_text(state.get("target_name")) or self._normalize_text(state.get("target_code"))
        return f"{target_type} peer group for {target_name or 'target'}"

    def _build_evidence(self, *, evidence_id: str, source: str, summary: str, reference_id: str) -> dict[str, Any]:
        return {
            "evidenceId": evidence_id,
            "source": source,
            "summary": summary,
            "referenceId": reference_id,
        }

    def _build_input_refs(self, state: dict[str, Any]) -> list[str]:
        refs = [
            f"task:{state.get('task_id')}",
            f"target:{state.get('target_code')}",
        ]
        event_result = state.get("event_extraction_result") or {}
        for event in event_result.get("events") or []:
            if isinstance(event, dict) and self._normalize_text(event.get("eventId")):
                refs.append(f"event:{self._normalize_text(event.get('eventId'))}")
        return refs

    def _resolve_text_list(self, preferred: Any, fallback: list[str]) -> list[str]:
        if not isinstance(preferred, list):
            return fallback
        values = self._dedupe([self._normalize_text(item) for item in preferred])
        return values or fallback

    def _join_text_list(self, value: Any) -> str:
        if not isinstance(value, list):
            return ""
        return "; ".join(self._normalize_text(item) for item in value if self._normalize_text(item))

    def _dedupe(self, values: list[str]) -> list[str]:
        result = []
        for value in values:
            text = self._normalize_text(value)
            if text and text not in result:
                result.append(text)
        return result

    def _first_text(self, *values: Any) -> str:
        for value in values:
            text = self._normalize_text(value)
            if text:
                return text
        return ""

    def _normalize_text(self, value: Any) -> str:
        if value is None:
            return ""
        return str(value).strip()
