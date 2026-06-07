import unittest

from app.agents.event_extraction_agent import EventExtractionAgent
from app.agents.industry_research_agent import IndustryResearchAgent
from app.common.task_routing import TASK_TYPE_STOCK_RESEARCH
from app.graph.workflow_builder import resolve_configured_sequence


class _NoopTaskControlService:
    def check_cancelled(self, task_id: str) -> None:
        return None


class _DisabledModelClient:
    def is_enabled(self, scene: str | None = None) -> bool:
        return False

    def availability_reason(self, scene: str | None = None) -> str:
        return "MODEL_CONFIG_DISABLED_FOR_TEST"

    def model_name(self, scene: str | None = None) -> str | None:
        return None


class Phase007ResearchAgentsTests(unittest.TestCase):
    def test_event_extraction_agent_outputs_fallback_provenance(self) -> None:
        agent = EventExtractionAgent.__new__(EventExtractionAgent)
        agent.model_client = _DisabledModelClient()
        agent.task_control_service = _NoopTaskControlService()

        state = agent.invoke({
            "task_id": "task-event",
            "trace_id": "trace-event",
            "target_code": "600000",
            "source_context": {
                "sourceEventId": "evt-1",
                "sourceEventTitle": "Major contract signed",
            },
            "task_context": {},
            "market_context": {},
            "agent_audits": [],
        })

        result = state["event_extraction_result"]
        self.assertEqual("RULE_FALLBACK", result["generationMode"])
        self.assertEqual("MODEL_CONFIG_DISABLED_FOR_TEST", result["fallbackReason"])
        self.assertTrue(result["evidence"])
        self.assertIn("approvedPayloadFields", result["provenance"])
        self.assertEqual("event_extraction_agent", state["agent_audits"][-1]["agentCode"])

    def test_industry_research_agent_outputs_fallback_provenance(self) -> None:
        agent = IndustryResearchAgent.__new__(IndustryResearchAgent)
        agent.model_client = _DisabledModelClient()
        agent.task_control_service = _NoopTaskControlService()

        state = agent.invoke({
            "task_id": "task-industry",
            "trace_id": "trace-industry",
            "target_type": "STOCK",
            "target_code": "600000",
            "target_name": "Test Bank",
            "market_context": {
                "latestInsightSummary": "Banking margin pressure is easing.",
                "latestInsightReportId": "report-1",
            },
            "event_extraction_result": {
                "events": [
                    {
                        "eventId": "evt-1",
                        "title": "Policy support",
                        "impactLevel": "HIGH",
                    }
                ]
            },
            "agent_audits": [],
        })

        result = state["industry_research_result"]
        self.assertEqual("RULE_FALLBACK", result["generationMode"])
        self.assertEqual("MODEL_CONFIG_DISABLED_FOR_TEST", result["fallbackReason"])
        self.assertTrue(result["industryDrivers"])
        self.assertTrue(result["evidence"])
        self.assertIn("approvedPayloadFields", result["provenance"])

    def test_stock_workflow_sequence_includes_phase007_agents(self) -> None:
        sequence = resolve_configured_sequence(TASK_TYPE_STOCK_RESEARCH)
        self.assertIn("event_extraction_agent", sequence)
        self.assertIn("industry_research_agent", sequence)
        self.assertLess(sequence.index("evidence_collection_agent"), sequence.index("event_extraction_agent"))
        self.assertLess(sequence.index("event_extraction_agent"), sequence.index("industry_research_agent"))
        self.assertLess(sequence.index("industry_research_agent"), sequence.index("report_generation_agent"))


if __name__ == "__main__":
    unittest.main()
