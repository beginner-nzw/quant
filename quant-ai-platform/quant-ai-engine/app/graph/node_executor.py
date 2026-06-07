import time
from typing import Callable

from app.messaging.kafka_producer import AiKafkaProducer
from app.services.workflow_checkpoint_service import WorkflowCheckpointService


class NodeExecutor:
    def __init__(
        self,
        producer: AiKafkaProducer,
        progress_map: dict[str, int],
        checkpoint_service: WorkflowCheckpointService | None = None,
    ):
        self.producer = producer
        self.progress_map = progress_map
        self.checkpoint_service = checkpoint_service or WorkflowCheckpointService()
        self.stage_map = {
            "planner_agent": "PLANNING",
            "intent_agent": "INTENT_UNDERSTANDING",
            "evidence_collection_agent": "EVIDENCE_COLLECTION",
            "event_extraction_agent": "EVENT_EXTRACTION",
            "industry_research_agent": "INDUSTRY_RESEARCH",
            "financial_analysis_agent": "FINANCIAL_ANALYSIS",
            "risk_review_agent": "RISK_REVIEW",
            "strategy_reasoning_agent": "STRATEGY_REASONING",
            "audit_compliance_agent": "AUDIT_COMPLIANCE",
            "report_generation_agent": "REPORT_GENERATION",
        }

    def wrap(self, node_name: str, agent_invoke: Callable[[dict], dict]):
        def _wrapped(state: dict) -> dict:
            if self._should_skip_node(state, node_name):
                return state

            start_ts = int(time.time() * 1000)
            self.checkpoint_service.save_checkpoint(
                task_id=state["task_id"],
                workflow_instance_id=state.get("workflow_instance_id"),
                node_name=node_name,
                state=state,
                status="RUNNING",
            )

            try:
                new_state = agent_invoke(state)
            except Exception as exc:
                self.checkpoint_service.mark_failed(
                    task_id=state["task_id"],
                    node_name=node_name,
                    state=state,
                    error_message=str(exc),
                )
                raise

            new_state["current_node"] = node_name
            new_state["progress"] = self.progress_map.get(node_name, 0)

            stage = self.stage_map.get(node_name) or new_state.get("current_stage") or node_name.upper()
            new_state["current_stage"] = stage

            self.producer.send_status(
                task_id=new_state["task_id"],
                trace_id=new_state["trace_id"],
                stage=stage,
                node=node_name,
                progress=self.progress_map.get(node_name, 0),
                status="RUNNING",
                workflow_instance_id=new_state.get("workflow_instance_id"),
                tenant_id=new_state.get("tenant_id"),
                biz_key=new_state.get("biz_key"),
                event_id=new_state.get("event_id") or (new_state.get("source_context") or {}).get("sourceEventId"),
                retry_count=new_state.get("retry_count", 0),
                actor_provenance=new_state.get("actor_provenance"),
            )

            end_ts = int(time.time() * 1000)
            audits = new_state.get("agent_audits", [])
            if audits:
                last = audits[-1]
                if last.get("nodeCode") == node_name:
                    last["startTimestamp"] = last.get("startTimestamp") or start_ts
                    last["finishTimestamp"] = end_ts
                    last["durationMs"] = max(0, end_ts - last["startTimestamp"])

            completed_nodes = new_state.setdefault("completed_nodes", [])
            if node_name not in completed_nodes:
                completed_nodes.append(node_name)
            new_state["checkpoint_status"] = "READY"
            self.checkpoint_service.save_node_state(
                task_id=new_state["task_id"],
                node_name=node_name,
                state=new_state,
                status="SUCCESS",
            )
            self.checkpoint_service.save_checkpoint(
                task_id=new_state["task_id"],
                workflow_instance_id=new_state.get("workflow_instance_id"),
                node_name=node_name,
                state=new_state,
                status="READY",
            )
            return new_state

        return _wrapped

    def _should_skip_node(self, state: dict, node_name: str) -> bool:
        completed_nodes = state.get("completed_nodes") or []
        rerun_node = state.get("rerun_node")
        if rerun_node:
            return node_name in completed_nodes and node_name != rerun_node
        return bool(state.get("resume_from_checkpoint")) and node_name in completed_nodes
