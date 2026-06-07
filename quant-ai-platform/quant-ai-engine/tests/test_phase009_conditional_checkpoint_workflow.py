import json
import unittest

from app.graph import workflow_builder
from app.graph.node_executor import NodeExecutor
from app.messaging.kafka_consumer import apply_checkpoint_control, publish_timeout_failure
from app.services.workflow_checkpoint_service import (
    NODE_STATE_KEY,
    WORKFLOW_CHECKPOINT_KEY,
    WorkflowCheckpointService,
)


class FakeRedisClient:
    def __init__(self):
        self.values = {}
        self.setex_calls = []

    def get(self, key):
        return self.values.get(key)

    def setex(self, key, seconds, value):
        self.values[key] = value
        self.setex_calls.append((key, seconds, value))

    def delete(self, key):
        self.values.pop(key, None)


class FakeProducer:
    def __init__(self):
        self.statuses = []
        self.failed_statuses = []
        self.failed_results = []

    def send_status(self, **kwargs):
        self.statuses.append(kwargs)

    def send_failed_status(self, **kwargs):
        self.failed_statuses.append(kwargs)

    def send_failed_result(self, **kwargs):
        self.failed_results.append(kwargs)


class Phase009ConditionalCheckpointWorkflowTests(unittest.TestCase):
    def test_default_node_timeouts_are_runtime_safe_for_phase017_e2e(self):
        self.assertGreaterEqual(workflow_builder.DEFAULT_TIMEOUTS["planner_agent"], 60)
        self.assertGreaterEqual(min(workflow_builder.DEFAULT_TIMEOUTS.values()), 60)

    def test_low_evidence_routes_to_event_extraction(self):
        decision = workflow_builder.resolve_branch_decision(
            {
                "task_type": "EQUITY_RESEARCH",
                "evidence_items": [],
                "evidence_refs": [],
            },
            "evidence_collection_agent",
            ["event_extraction_agent", "industry_research_agent", "report_generation_agent"],
        )

        self.assertEqual("event_extraction_agent", decision["nextNode"])
        self.assertEqual("LOW_EVIDENCE_QUALITY", decision["reason"])
        self.assertEqual("LOW", decision["evidenceQuality"])

    def test_high_risk_routes_to_audit_before_report(self):
        decision = workflow_builder.resolve_branch_decision(
            {
                "task_type": "EQUITY_RESEARCH",
                "risk_result": {"riskLevel": "HIGH", "needHumanReview": True},
                "evidence_items": [{"evidenceId": "e-1"}],
                "evidence_refs": ["source:e-1"],
            },
            "risk_review_agent",
            ["strategy_reasoning_agent", "audit_compliance_agent", "report_generation_agent"],
        )

        self.assertEqual("human_review_gate", decision["nextNode"])
        self.assertEqual("WAITING_HUMAN_DECISION", decision["reason"])
        self.assertEqual("HIGH", decision["riskLevel"])

    def test_node_executor_persists_checkpoint_and_node_state(self):
        redis_client = FakeRedisClient()
        checkpoint_service = WorkflowCheckpointService()
        checkpoint_service.redis_client = redis_client
        producer = FakeProducer()
        executor = NodeExecutor(
            producer,
            {"planner_agent": 10},
            checkpoint_service=checkpoint_service,
        )

        wrapped = executor.wrap(
            "planner_agent",
            lambda state: {**state, "plan_result": {"ok": True}, "agent_audits": []},
        )

        result = wrapped({
            "task_id": "task-1",
            "trace_id": "trace-1",
            "workflow_instance_id": "wf-task-1",
            "completed_nodes": [],
        })

        self.assertEqual(["planner_agent"], result["completed_nodes"])
        self.assertIn(WORKFLOW_CHECKPOINT_KEY.format(task_id="task-1"), redis_client.values)
        self.assertIn(NODE_STATE_KEY.format(task_id="task-1", node_name="planner_agent"), redis_client.values)
        checkpoint = json.loads(redis_client.values[WORKFLOW_CHECKPOINT_KEY.format(task_id="task-1")])
        self.assertEqual("READY", checkpoint["status"])
        self.assertEqual("planner_agent", checkpoint["currentNode"])
        self.assertEqual("planner_agent", producer.statuses[-1]["node"])

    def test_node_executor_marks_failed_checkpoint_and_node_state_on_exception(self):
        redis_client = FakeRedisClient()
        checkpoint_service = WorkflowCheckpointService()
        checkpoint_service.redis_client = redis_client
        executor = NodeExecutor(
            FakeProducer(),
            {"risk_review_agent": 82},
            checkpoint_service=checkpoint_service,
        )

        def failing_agent(_state):
            raise RuntimeError("risk model unavailable")

        wrapped = executor.wrap("risk_review_agent", failing_agent)

        with self.assertRaises(RuntimeError):
            wrapped({
                "task_id": "task-1",
                "trace_id": "trace-1",
                "workflow_instance_id": "wf-task-1",
                "completed_nodes": [],
            })

        checkpoint = json.loads(redis_client.values[WORKFLOW_CHECKPOINT_KEY.format(task_id="task-1")])
        node_state = json.loads(redis_client.values[NODE_STATE_KEY.format(task_id="task-1", node_name="risk_review_agent")])
        self.assertEqual("FAILED", checkpoint["status"])
        self.assertEqual("FAILED", node_state["status"])
        self.assertEqual("risk model unavailable", checkpoint["state"]["checkpoint_error"])
        self.assertEqual("risk model unavailable", node_state["state"]["checkpoint_error"])

    def test_apply_checkpoint_control_restores_resume_and_rerun_state(self):
        checkpoint = {
            "currentNode": "risk_review_agent",
            "state": {
                "task_id": "task-1",
                "trace_id": "old-trace",
                "completed_nodes": ["planner_agent", "risk_review_agent"],
                "risk_result": {"riskLevel": "HIGH"},
            },
        }
        init_state = {
            "task_id": "task-1",
            "trace_id": "new-trace",
            "tenant_id": "tenant-1",
            "biz_key": "STOCK:000001",
            "retry_count": 2,
            "actor_provenance": {"source": "test"},
        }

        resumed = apply_checkpoint_control(init_state, checkpoint, {"action": "RESUME"})
        self.assertTrue(resumed["resume_from_checkpoint"])
        self.assertEqual("risk_review_agent", resumed["resume_from_node"])
        self.assertEqual("new-trace", resumed["trace_id"])

        rerun = apply_checkpoint_control(
            init_state,
            checkpoint,
            {"action": "RERUN_NODE", "nodeName": "risk_review_agent"},
        )
        self.assertEqual("risk_review_agent", rerun["rerun_node"])
        self.assertEqual(["planner_agent"], rerun["completed_nodes"])

    def test_route_next_node_persists_branch_decision_into_checkpoint(self):
        redis_client = FakeRedisClient()
        checkpoint_service = WorkflowCheckpointService()
        checkpoint_service.redis_client = redis_client
        original_service = workflow_builder.workflow_checkpoint_service
        workflow_builder.workflow_checkpoint_service = checkpoint_service
        try:
            state = {
                "task_id": "task-1",
                "workflow_instance_id": "wf-task-1",
                "task_type": "EQUITY_RESEARCH",
                "current_stage": "EVIDENCE_COLLECTION",
                "current_node": "evidence_collection_agent",
                "checkpoint_status": "READY",
                "evidence_items": [],
                "evidence_refs": [],
            }

            next_node = workflow_builder.route_next_node(
                state,
                "evidence_collection_agent",
                ["event_extraction_agent", "report_generation_agent"],
            )
        finally:
            workflow_builder.workflow_checkpoint_service = original_service

        self.assertEqual("event_extraction_agent", next_node)
        checkpoint = json.loads(redis_client.values[WORKFLOW_CHECKPOINT_KEY.format(task_id="task-1")])
        self.assertEqual("LOW_EVIDENCE_QUALITY", checkpoint["branchDecisions"][-1]["reason"])
        self.assertEqual("LOW_EVIDENCE_QUALITY", checkpoint["state"]["branch_decisions"][-1]["reason"])

    def test_timeout_failure_publishes_failed_status_and_result(self):
        class Payload:
            taskTitle = "Timeout task"
            targetType = "STOCK"
            targetCode = "000001"
            targetName = "Ping An Bank"
            priority = "HIGH"
            sourceTaskId = "source-task-1"
            sourceReportId = "source-report-1"
            sourceEventId = "event-1"
            sourceDomain = "MARKET_EVENT"
            sourceReviewStatus = "PENDING"

        class Dispatch:
            taskId = "task-1"
            traceId = "trace-1"
            tenantId = "tenant-1"
            bizKey = "STOCK:000001"
            retryCount = 1
            payload = Payload()

        producer = FakeProducer()

        publish_timeout_failure(
            producer=producer,
            data=Dispatch(),
            task_type="EQUITY_RESEARCH",
            analysis_scope="FULL",
            event_id="event-1",
            actor_provenance={"identitySource": "USER_CONTEXT"},
            error=TimeoutError("workflow timeout after 1s"),
        )

        self.assertEqual(1, len(producer.failed_statuses))
        self.assertEqual(1, len(producer.failed_results))
        self.assertEqual("TIMEOUT", producer.failed_statuses[0]["stage"])
        self.assertEqual("workflow_timeout", producer.failed_statuses[0]["node"])
        self.assertEqual("TIMEOUT", producer.failed_results[0]["final_stage"])
        self.assertIn("workflow timeout after 1s", producer.failed_results[0]["error_message"])


if __name__ == "__main__":
    unittest.main()
