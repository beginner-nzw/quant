import json
import sys
import types
import unittest

confluent_kafka_stub = types.ModuleType("confluent_kafka")
confluent_kafka_stub.Consumer = object
sys.modules.setdefault("confluent_kafka", confluent_kafka_stub)

for module_name, class_name in [
    ("app.agents.evidence_collection_agent", "EvidenceCollectionAgent"),
    ("app.agents.event_extraction_agent", "EventExtractionAgent"),
    ("app.agents.financial_analysis_agent", "FinancialAnalysisAgent"),
    ("app.agents.industry_research_agent", "IndustryResearchAgent"),
    ("app.agents.intent_agent", "IntentAgent"),
    ("app.agents.planner_agent", "PlannerAgent"),
    ("app.agents.report_generation_agent", "ReportGenerationAgent"),
    ("app.agents.risk_review_agent", "RiskReviewAgent"),
    ("app.agents.strategy_reasoning_agent", "StrategyReasoningAgent"),
    ("app.agents.audit_compliance_agent", "AuditComplianceAgent"),
]:
    module = types.ModuleType(module_name)
    setattr(module, class_name, type(class_name, (), {}))
    sys.modules.setdefault(module_name, module)

human_review_stub = types.ModuleType("app.common.human_review")
human_review_stub.HumanReviewRequiredException = type("HumanReviewRequiredException", (Exception,), {})
sys.modules.setdefault("app.common.human_review", human_review_stub)

task_routing_stub = types.ModuleType("app.common.task_routing")
task_routing_stub.resolve_analysis_scope = lambda *_args, **_kwargs: "FULL"
task_routing_stub.resolve_task_type = lambda *_args, **_kwargs: "EQUITY_RESEARCH"
sys.modules.setdefault("app.common.task_routing", task_routing_stub)

workflow_builder_stub = types.ModuleType("app.graph.workflow_builder")
workflow_builder_stub.build_workflow_for_task = lambda *_args, **_kwargs: None
workflow_builder_stub.DEFAULT_TIMEOUTS = {"planner_agent": 75, "report_generation_agent": 75}
sys.modules.setdefault("app.graph.workflow_builder", workflow_builder_stub)

settings_stub = types.ModuleType("app.config.settings")
settings_stub.settings = types.SimpleNamespace(
    kafka=types.SimpleNamespace(max_lag_messages=1000, lag_log_interval_seconds=60)
)
sys.modules.setdefault("app.config.settings", settings_stub)

redis_client_stub = types.ModuleType("app.clients.redis_client")
redis_client_stub.RedisClient = type("RedisClient", (), {})
sys.modules.setdefault("app.clients.redis_client", redis_client_stub)

kafka_producer_stub = types.ModuleType("app.messaging.kafka_producer")
kafka_producer_stub.AiKafkaProducer = type("AiKafkaProducer", (), {})
sys.modules.setdefault("app.messaging.kafka_producer", kafka_producer_stub)

message_models_stub = types.ModuleType("app.messaging.message_models")
message_models_stub.AiTaskDispatchMessage = type("AiTaskDispatchMessage", (), {})
sys.modules.setdefault("app.messaging.message_models", message_models_stub)

observability_stub = types.ModuleType("app.observability")
observability_stub.record_kafka_lag = lambda *_args, **_kwargs: None
observability_stub.record_task = lambda *_args, **_kwargs: None
sys.modules.setdefault("app.observability", observability_stub)

task_control_stub = types.ModuleType("app.services.task_control_service")
task_control_stub.TaskControlService = type("TaskControlService", (), {})
sys.modules.setdefault("app.services.task_control_service", task_control_stub)

task_loader_stub = types.ModuleType("app.services.task_loader_service")
task_loader_stub.TaskLoaderService = type("TaskLoaderService", (), {})
sys.modules.setdefault("app.services.task_loader_service", task_loader_stub)

logger_stub = types.ModuleType("app.utils.logger")
logger_stub.log_error = lambda *_args, **_kwargs: None
logger_stub.log_info = lambda *_args, **_kwargs: None
sys.modules.setdefault("app.utils.logger", logger_stub)

from app.messaging.kafka_consumer import apply_checkpoint_control, publish_timeout_failure
from app.services.workflow_checkpoint_service import (
    NODE_STATE_KEY,
    WORKFLOW_CHECKPOINT_KEY,
    WorkflowCheckpointService,
)

for stubbed_module_name in [
    "confluent_kafka",
    "app.common.human_review",
    "app.common.task_routing",
    "app.config.settings",
    "app.graph.workflow_builder",
    "app.messaging.message_models",
    "app.messaging.kafka_producer",
    "app.observability",
    "app.services.task_control_service",
    "app.services.task_loader_service",
    "app.clients.redis_client",
    "app.utils.logger",
]:
    sys.modules.pop(stubbed_module_name, None)


class FakeRedisClient:
    def __init__(self):
        self.values = {}
        self.deleted = []

    def get(self, key):
        return self.values.get(key)

    def setex(self, key, seconds, value):
        self.values[key] = value

    def delete(self, key):
        self.deleted.append(key)
        self.values.pop(key, None)


class FakeProducer:
    def __init__(self):
        self.failed_statuses = []
        self.failed_results = []

    def send_failed_status(self, **kwargs):
        self.failed_statuses.append(kwargs)

    def send_failed_result(self, **kwargs):
        self.failed_results.append(kwargs)


class Phase017RecoveryHarnessTests(unittest.TestCase):
    def test_ai_timeout_publishes_failed_status_and_result_for_recovery_loop(self):
        class Payload:
            taskTitle = "Phase 17 AI timeout task"
            targetType = "STOCK"
            targetCode = "PH17-AI-TIMEOUT"
            targetName = "Phase 17 Timeout Target"
            priority = "HIGH"
            sourceTaskId = "source-task-17"
            sourceReportId = "source-report-17"
            sourceEventId = "event-17"
            sourceDomain = "PHASE_017"
            sourceReviewStatus = "PENDING"

        class Dispatch:
            taskId = "phase-017-timeout"
            traceId = "trace-phase-017"
            tenantId = "tenant-phase-017"
            bizKey = "STOCK:PH17-AI-TIMEOUT"
            retryCount = 2
            payload = Payload()

        producer = FakeProducer()
        publish_timeout_failure(
            producer=producer,
            data=Dispatch(),
            task_type="EQUITY_RESEARCH",
            analysis_scope="FULL",
            event_id="event-17",
            actor_provenance={"identitySource": "PHASE_017_TEST"},
            error=TimeoutError("workflow timeout after 1s"),
        )

        self.assertEqual("TIMEOUT", producer.failed_statuses[0]["stage"])
        self.assertEqual("workflow_timeout", producer.failed_statuses[0]["node"])
        self.assertEqual("TIMEOUT", producer.failed_results[0]["final_stage"])
        self.assertEqual("PHASE_017_TEST", producer.failed_results[0]["actor_provenance"]["identitySource"])
        self.assertEqual("event-17", producer.failed_results[0]["event_id"])

    def test_checkpoint_recovery_resume_preserves_new_dispatch_context(self):
        checkpoint = {
            "currentNode": "audit_compliance_agent",
            "state": {
                "task_id": "phase-017-recovery",
                "trace_id": "old-trace",
                "tenant_id": "old-tenant",
                "completed_nodes": ["planner_agent", "risk_review_agent", "audit_compliance_agent"],
                "risk_result": {"riskLevel": "HIGH", "needHumanReview": True},
            },
        }
        init_state = {
            "task_id": "phase-017-recovery",
            "trace_id": "new-trace",
            "tenant_id": "new-tenant",
            "biz_key": "STOCK:PH17",
            "retry_count": 3,
            "actor_provenance": {"identitySource": "USER_CONTEXT", "actorId": "phase017-runner"},
        }

        resumed = apply_checkpoint_control(init_state, checkpoint, {"action": "RESUME"})

        self.assertTrue(resumed["resume_from_checkpoint"])
        self.assertEqual("audit_compliance_agent", resumed["resume_from_node"])
        self.assertEqual("new-trace", resumed["trace_id"])
        self.assertEqual("new-tenant", resumed["tenant_id"])
        self.assertEqual("phase017-runner", resumed["actor_provenance"]["actorId"])

    def test_checkpoint_recovery_rerun_removes_requested_node_completion_marker(self):
        checkpoint = {
            "currentNode": "report_generation_agent",
            "state": {
                "task_id": "phase-017-rerun",
                "completed_nodes": [
                    "planner_agent",
                    "evidence_collection_agent",
                    "risk_review_agent",
                    "report_generation_agent",
                ],
                "report_result": {"summary": "stale"},
            },
        }
        init_state = {
            "task_id": "phase-017-rerun",
            "trace_id": "rerun-trace",
            "tenant_id": "tenant-phase-017",
            "retry_count": 1,
            "actor_provenance": {"identitySource": "USER_CONTEXT"},
        }

        rerun = apply_checkpoint_control(
            init_state,
            checkpoint,
            {"action": "RERUN_NODE", "nodeName": "risk_review_agent"},
        )

        self.assertEqual("risk_review_agent", rerun["rerun_node"])
        self.assertNotIn("risk_review_agent", rerun["completed_nodes"])
        self.assertIn("report_generation_agent", rerun["completed_nodes"])

    def test_checkpoint_service_marks_failed_state_for_redis_backed_recovery(self):
        redis_client = FakeRedisClient()
        checkpoint_service = WorkflowCheckpointService()
        checkpoint_service.redis_client = redis_client

        checkpoint_service.mark_failed(
            task_id="phase-017-failed",
            node_name="strategy_reasoning_agent",
            state={
                "task_id": "phase-017-failed",
                "workflow_instance_id": "wf-phase-017",
                "current_stage": "STRATEGY",
                "progress": 70,
            },
            error_message="redis unavailable during downstream projection",
        )

        checkpoint_key = WORKFLOW_CHECKPOINT_KEY.format(task_id="phase-017-failed")
        node_key = NODE_STATE_KEY.format(task_id="phase-017-failed", node_name="strategy_reasoning_agent")
        checkpoint = json.loads(redis_client.values[checkpoint_key])
        node_state = json.loads(redis_client.values[node_key])

        self.assertEqual("FAILED", checkpoint["status"])
        self.assertEqual("FAILED", node_state["status"])
        self.assertEqual("redis unavailable during downstream projection", checkpoint["state"]["checkpoint_error"])


if __name__ == "__main__":
    unittest.main()
