import json
import unittest

from app.common.human_review import HumanReviewRequiredException
from app.eval.harness import AiEvalHarness, EvalCase
from app.graph import workflow_builder
from app.services.workflow_checkpoint_service import WORKFLOW_CHECKPOINT_KEY, WorkflowCheckpointService


class FakeRedisClient:
    def __init__(self):
        self.values = {}

    def get(self, key):
        return self.values.get(key)

    def setex(self, key, seconds, value):
        self.values[key] = value

    def delete(self, key):
        self.values.pop(key, None)


class Phase010HumanReviewEvalTests(unittest.TestCase):
    def test_high_risk_routes_to_human_review_gate_before_report(self):
        decision = workflow_builder.resolve_branch_decision(
            {
                "task_id": "task-1",
                "task_type": "EQUITY_RESEARCH",
                "risk_result": {"riskLevel": "HIGH"},
                "need_human_review": True,
            },
            "risk_review_agent",
            ["audit_compliance_agent", "report_generation_agent"],
        )

        self.assertEqual("human_review_gate", decision["nextNode"])
        self.assertEqual("WAITING_HUMAN_DECISION", decision["reason"])

    def test_human_review_gate_persists_waiting_checkpoint(self):
        redis_client = FakeRedisClient()
        checkpoint_service = WorkflowCheckpointService()
        checkpoint_service.redis_client = redis_client
        gate = workflow_builder.build_human_review_gate_node(checkpoint_service)

        with self.assertRaises(HumanReviewRequiredException):
            gate({
                "task_id": "task-1",
                "workflow_instance_id": "wf-task-1",
                "trace_id": "trace-1",
                "progress": 92,
                "branch_decisions": [],
            })

        checkpoint = json.loads(redis_client.values[WORKFLOW_CHECKPOINT_KEY.format(task_id="task-1")])
        self.assertEqual("WAITING_HUMAN_REVIEW", checkpoint["status"])
        self.assertTrue(checkpoint["state"]["waiting_human_review"])
        self.assertEqual("human_review_gate", checkpoint["currentNode"])

    def test_eval_harness_scores_grounded_fallback_report(self):
        harness = AiEvalHarness()
        result = harness.evaluate(EvalCase(
            name="grounded-fallback",
            fallback_expected=True,
            evidence_items=[
                {
                    "evidenceId": "e-1",
                    "title": "Quarterly revenue pressure",
                    "summary": "Margin pressure was disclosed in the quarterly update.",
                    "referenceId": "filing-1",
                }
            ],
            report={
                "summary": "Quarterly revenue pressure is visible in the cited update, so the report keeps a cautious conclusion and asks for human review before approval.",
                "highlights": ["Quarterly revenue pressure cited", "Margin pressure requires review"],
                "riskPoints": ["Margin pressure remains unresolved"],
                "evidenceRefs": ["filing:e-1"],
                "confidenceScore": 0.74,
                "reviewSuggestion": "Human reviewer should verify the cited filing.",
                "contextSnapshot": {
                    "reportGenerationPath": "RULE_FALLBACK",
                    "reportFallbackReason": "model unavailable",
                },
            },
        ))

        self.assertTrue(result["passed"])
        self.assertGreaterEqual(result["scores"]["evidenceGrounding"], 0.9)
        self.assertEqual(1.0, result["scores"]["fallback"])


if __name__ == "__main__":
    unittest.main()
