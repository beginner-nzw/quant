import unittest

from app.agents.audit_compliance_agent import AuditComplianceAgent
from app.agents.strategy_reasoning_agent import StrategyReasoningAgent
from app.common.task_routing import TASK_TYPE_STOCK_RESEARCH
from app.graph.workflow_builder import resolve_configured_sequence


class _NoopTaskControlService:
    def check_cancelled(self, task_id: str) -> None:
        return None


class Phase008StrategyAuditAgentsTests(unittest.TestCase):
    def test_strategy_agent_outputs_candidate_factor_confidence_and_evidence(self) -> None:
        agent = StrategyReasoningAgent.__new__(StrategyReasoningAgent)
        agent.task_control_service = _NoopTaskControlService()

        state = agent.invoke({
            "task_id": "task-strategy",
            "trace_id": "trace-strategy",
            "target_name": "Test Co",
            "financial_result": {"summary": "Margins improved", "confidenceScore": 0.9},
            "risk_result": {"riskPoints": [], "riskWarnings": [], "needHumanReview": False},
            "industry_research_result": {"industryDrivers": ["Policy demand improving"]},
            "event_extraction_result": {"eventThemes": ["new contract"]},
            "market_context": {"dataSource": "fallback"},
            "evidence_items": [
                {
                    "evidenceId": "evidence-1",
                    "evidenceType": "MARKET_EVENT",
                    "title": "Contract",
                    "summary": "Contract signed",
                    "referenceId": "event-1",
                    "relevance": "HIGH",
                }
            ],
            "agent_audits": [],
        })

        result = state["strategy_result"]
        self.assertEqual("AI_STRATEGY_SIGNAL_CANDIDATE", result["candidateType"])
        self.assertIn(result["direction"], {"POSITIVE", "NEUTRAL", "NEGATIVE"})
        self.assertGreater(result["confidence"], 0)
        self.assertTrue(result["factors"])
        self.assertTrue(result["evidence"])
        self.assertEqual("CANDIDATE_ONLY", result["trace"]["authority"])
        self.assertEqual("strategy_reasoning_agent", state["agent_audits"][-1]["agentCode"])

    def test_audit_agent_outputs_support_without_business_approval(self) -> None:
        agent = AuditComplianceAgent.__new__(AuditComplianceAgent)
        agent.task_control_service = _NoopTaskControlService()

        state = agent.invoke({
            "task_id": "task-audit",
            "trace_id": "trace-audit",
            "risk_result": {"needHumanReview": True, "riskPoints": ["risk"]},
            "strategy_result": {"trace": {"authority": "CANDIDATE_ONLY"}},
            "evidence_items": [{"evidenceId": "evidence-1", "summary": "source"}],
            "evidence_refs": ["sourceEvent:event-1"],
            "agent_audits": [],
        })

        result = state["audit_result"]
        self.assertEqual("SUPPORT_ONLY_NO_BUSINESS_APPROVAL", result["authority"])
        self.assertTrue(result["policyChecks"])
        self.assertTrue(result["evidenceChecks"])
        self.assertTrue(result["reportReview"]["doesNotApproveReport"])
        self.assertTrue(result["reviewSuggestions"])
        self.assertEqual("audit_compliance_agent", state["agent_audits"][-1]["agentCode"])

    def test_stock_workflow_sequence_includes_strategy_and_audit_before_report(self) -> None:
        sequence = resolve_configured_sequence(TASK_TYPE_STOCK_RESEARCH)
        self.assertIn("strategy_reasoning_agent", sequence)
        self.assertIn("audit_compliance_agent", sequence)
        self.assertLess(sequence.index("risk_review_agent"), sequence.index("strategy_reasoning_agent"))
        self.assertLess(sequence.index("strategy_reasoning_agent"), sequence.index("audit_compliance_agent"))
        self.assertLess(sequence.index("audit_compliance_agent"), sequence.index("report_generation_agent"))


if __name__ == "__main__":
    unittest.main()
