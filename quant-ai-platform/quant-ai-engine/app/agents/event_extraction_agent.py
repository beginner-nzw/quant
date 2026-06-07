import time
from typing import Any

from app.clients.model_client import ModelClient
from app.services.prompt_builder_service import PromptBuilderService
from app.services.task_control_service import TaskControlService


class EventExtractionAgent:
    def __init__(self):
        self.model_client = ModelClient()
        self.prompt_builder_service = PromptBuilderService()
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        state["current_stage"] = "EVENT_EXTRACTION"
        state["current_node"] = "event_extraction_agent"
        state["progress"] = 48

        fallback_result = self._build_fallback_result(state)
        model_result, model_name, fallback_reason = self._generate_model_result(state, fallback_result)
        event_result = {
            "events": self._resolve_event_list(
                model_result.get("events") if model_result else None,
                fallback_result["events"],
            ),
            "eventThemes": self._resolve_text_list(
                model_result.get("eventThemes") if model_result else None,
                fallback_result["eventThemes"],
            ),
            "evidence": fallback_result["evidence"],
            "provenance": fallback_result["provenance"],
            "generationMode": "MODEL_ASSISTED" if model_result else "RULE_FALLBACK",
            "fallbackReason": fallback_reason,
            "llmFramework": "custom-http" if model_result else None,
            "modelName": model_name,
        }
        state["event_extraction_result"] = event_result
        state.setdefault("agent_audits", []).append(
            {
                "executionId": f"exec-{state['task_id']}-event-extraction",
                "agentCode": "event_extraction_agent",
                "agentName": "Event Extraction Agent",
                "nodeCode": "event_extraction_agent",
                "status": "SUCCESS",
                "confidenceScore": 0.87 if model_result else 0.82,
                "needHumanReview": False,
                "startTimestamp": now,
                "finishTimestamp": now,
                "durationMs": 0,
            }
        )
        return state

    def _build_fallback_result(self, state: dict[str, Any]) -> dict[str, Any]:
        source_context = state.get("source_context") or {}
        task_context = state.get("task_context") or {}
        market_context = state.get("market_context") or {}
        events: list[dict[str, Any]] = []
        evidence: list[dict[str, Any]] = []

        source_event = task_context.get("sourceEvent") or {}
        source_event_id = self._first_text(source_event.get("eventId"), source_context.get("sourceEventId"))
        source_title = self._first_text(source_event.get("eventTitle"), source_context.get("sourceEventTitle"))
        source_summary = self._first_text(source_event.get("eventSummary"), source_context.get("sourceEventSummary"))
        if source_event_id or source_title or source_summary:
            evidence_id = f"source-event:{source_event_id or 'unknown'}"
            events.append(self._build_event(
                event_id=source_event_id or evidence_id,
                event_type=self._first_text(source_event.get("eventType"), source_context.get("sourceEventType"), "SOURCE_EVENT"),
                title=source_title or "Source event",
                summary=source_summary or source_title or "Source event attached to task.",
                occurred_at=self._first_text(source_event.get("occurredAt"), source_context.get("sourceEventOccurredAt")),
                impact_level=self._first_text(source_event.get("impactLevel"), source_context.get("sourceEventImpactLevel"), "MEDIUM"),
                evidence_ids=[evidence_id],
            ))
            evidence.append(self._build_evidence(
                evidence_id=evidence_id,
                source=self._first_text(source_event.get("sourceChannel"), source_context.get("sourceDomain"), "task-context"),
                title=source_title,
                summary=source_summary,
                reference_id=source_event_id,
                url=self._normalize_text(source_event.get("sourceUrl")),
            ))

        for index, item in enumerate((market_context.get("liveMarketEvents") or [])[:5], start=1):
            if not isinstance(item, dict):
                continue
            title = self._normalize_text(item.get("eventTitle") or item.get("title"))
            summary = self._normalize_text(item.get("eventSummary") or item.get("summary")) or title
            if not title and not summary:
                continue
            event_id = self._normalize_text(item.get("eventId")) or f"live-{index}"
            evidence_id = f"live-market-event:{event_id}"
            events.append(self._build_event(
                event_id=event_id,
                event_type=self._first_text(item.get("eventType"), item.get("sourceCategory"), "LIVE_MARKET_EVENT"),
                title=title or summary,
                summary=summary,
                occurred_at=self._normalize_text(item.get("occurredAt")),
                impact_level=self._first_text(item.get("impactLevel"), "MEDIUM"),
                evidence_ids=[evidence_id],
            ))
            evidence.append(self._build_evidence(
                evidence_id=evidence_id,
                source=self._first_text(item.get("sourceName"), item.get("sourceChannel"), item.get("sourceCode"), "live-market-event"),
                title=title,
                summary=summary,
                reference_id=event_id,
                url=self._normalize_text(item.get("sourceUrl")),
            ))

        return {
            "events": events,
            "eventThemes": self._build_event_themes(events),
            "evidence": evidence,
            "provenance": {
                "source": "TASK_AND_MARKET_CONTEXT",
                "inputRefs": self._build_input_refs(state, events),
                "approvedPayloadFields": [
                    "events",
                    "eventThemes",
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
        if not self.model_client.is_enabled("event_extraction"):
            return (
                None,
                None,
                self.model_client.availability_reason("event_extraction") or "CUSTOM_HTTP_DISABLED",
            )
        system_prompt, user_prompt = self.prompt_builder_service.build_event_extraction_prompts(
            state=state,
            fallback_result=fallback_result,
        )
        model_result = self.model_client.generate_json_object(
            scene="event_extraction",
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            trace_id=state.get("trace_id", ""),
        )
        if not isinstance(model_result, dict):
            return None, None, "CUSTOM_HTTP_NO_RESULT"
        if not isinstance(model_result.get("events"), list):
            return None, None, "CUSTOM_HTTP_INVALID_EVENTS"
        return model_result, self.model_client.model_name("event_extraction"), None

    def _build_event(self, *, event_id: str, event_type: str, title: str, summary: str, occurred_at: str, impact_level: str, evidence_ids: list[str]) -> dict[str, Any]:
        return {
            "eventId": event_id,
            "eventType": event_type,
            "title": title,
            "summary": summary,
            "occurredAt": occurred_at,
            "impactLevel": impact_level.upper() if impact_level else "MEDIUM",
            "evidenceIds": evidence_ids,
        }

    def _build_evidence(self, *, evidence_id: str, source: str, title: str, summary: str, reference_id: str, url: str) -> dict[str, Any]:
        return {
            "evidenceId": evidence_id,
            "source": source,
            "title": title,
            "summary": summary,
            "referenceId": reference_id,
            "url": url,
        }

    def _resolve_event_list(self, preferred: Any, fallback: list[dict[str, Any]]) -> list[dict[str, Any]]:
        if not isinstance(preferred, list):
            return fallback
        result = []
        for item in preferred:
            if not isinstance(item, dict):
                continue
            title = self._normalize_text(item.get("title"))
            summary = self._normalize_text(item.get("summary"))
            if not title and not summary:
                continue
            result.append(self._build_event(
                event_id=self._first_text(item.get("eventId"), f"model-event-{len(result) + 1}"),
                event_type=self._first_text(item.get("eventType"), "EXTRACTED_EVENT"),
                title=title or summary,
                summary=summary or title,
                occurred_at=self._normalize_text(item.get("occurredAt")),
                impact_level=self._first_text(item.get("impactLevel"), "MEDIUM"),
                evidence_ids=self._resolve_text_list(item.get("evidenceIds"), []),
            ))
        return result or fallback

    def _resolve_text_list(self, preferred: Any, fallback: list[str]) -> list[str]:
        if not isinstance(preferred, list):
            return fallback
        values = []
        for item in preferred:
            text = self._normalize_text(item)
            if text and text not in values:
                values.append(text)
        return values or fallback

    def _build_event_themes(self, events: list[dict[str, Any]]) -> list[str]:
        themes = []
        for item in events:
            event_type = self._normalize_text(item.get("eventType")).upper()
            impact_level = self._normalize_text(item.get("impactLevel")).upper()
            if event_type and event_type not in themes:
                themes.append(event_type)
            if impact_level == "HIGH" and "HIGH_IMPACT" not in themes:
                themes.append("HIGH_IMPACT")
        return themes[:6]

    def _build_input_refs(self, state: dict[str, Any], events: list[dict[str, Any]]) -> list[str]:
        refs = [
            f"task:{state.get('task_id')}",
            f"target:{state.get('target_code')}",
        ]
        for item in events:
            event_id = self._normalize_text(item.get("eventId"))
            if event_id:
                refs.append(f"event:{event_id}")
        return [ref for ref in refs if ref]

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
