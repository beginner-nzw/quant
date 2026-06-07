import copy
import json
import time
from typing import Any

from app.clients.redis_client import RedisClient

WORKFLOW_CHECKPOINT_KEY = "task:workflow:checkpoint:{task_id}"
NODE_STATE_KEY = "task:workflow:node:{task_id}:{node_name}"
DEFAULT_CHECKPOINT_TTL_SECONDS = 72 * 60 * 60


class WorkflowCheckpointService:
    def __init__(self):
        self.redis_client = RedisClient()

    def load_checkpoint(self, task_id: str) -> dict[str, Any] | None:
        raw = self.redis_client.get(WORKFLOW_CHECKPOINT_KEY.format(task_id=task_id))
        if not raw:
            return None
        try:
            value = json.loads(raw)
        except Exception:
            return None
        return value if isinstance(value, dict) else None

    def save_checkpoint(
        self,
        *,
        task_id: str,
        workflow_instance_id: str | None,
        node_name: str,
        state: dict[str, Any],
        status: str,
        branch_decisions: list[dict[str, Any]] | None = None,
        ttl_seconds: int = DEFAULT_CHECKPOINT_TTL_SECONDS,
    ):
        checkpoint = {
            "taskId": task_id,
            "workflowInstanceId": workflow_instance_id,
            "currentNode": node_name,
            "status": status,
            "progress": state.get("progress"),
            "currentStage": state.get("current_stage"),
            "retryCount": state.get("retry_count", 0),
            "updatedAt": int(time.time() * 1000),
            "branchDecisions": branch_decisions or state.get("branch_decisions", []),
            "state": self._json_safe_copy(state),
        }
        self.redis_client.setex(
            WORKFLOW_CHECKPOINT_KEY.format(task_id=task_id),
            ttl_seconds,
            json.dumps(checkpoint, ensure_ascii=False),
        )

    def save_node_state(
        self,
        *,
        task_id: str,
        node_name: str,
        state: dict[str, Any],
        status: str,
        ttl_seconds: int = DEFAULT_CHECKPOINT_TTL_SECONDS,
    ):
        node_state = {
            "taskId": task_id,
            "nodeName": node_name,
            "status": status,
            "progress": state.get("progress"),
            "currentStage": state.get("current_stage"),
            "updatedAt": int(time.time() * 1000),
            "state": self._json_safe_copy(state),
        }
        self.redis_client.setex(
            NODE_STATE_KEY.format(task_id=task_id, node_name=node_name),
            ttl_seconds,
            json.dumps(node_state, ensure_ascii=False),
        )

    def mark_failed(self, *, task_id: str, node_name: str, state: dict[str, Any], error_message: str):
        failure_state = dict(state)
        failure_state["checkpoint_error"] = error_message
        self.save_checkpoint(
            task_id=task_id,
            workflow_instance_id=state.get("workflow_instance_id"),
            node_name=node_name,
            state=failure_state,
            status="FAILED",
        )
        self.save_node_state(
            task_id=task_id,
            node_name=node_name,
            state=failure_state,
            status="FAILED",
        )

    def mark_waiting_human_review(
        self,
        *,
        task_id: str,
        workflow_instance_id: str | None,
        node_name: str,
        state: dict[str, Any],
        reason: str,
    ):
        waiting_state = dict(state)
        waiting_state["status"] = "WAITING_HUMAN_REVIEW"
        waiting_state["checkpoint_status"] = "WAITING_HUMAN_REVIEW"
        waiting_state["waiting_human_review"] = True
        waiting_state["human_review_gate"] = {
            "nodeName": node_name,
            "reason": reason,
            "requestedAt": int(time.time() * 1000),
            "resumeActions": ["APPROVE", "REJECT", "REVISE", "RERUN_NODE"],
        }
        self.save_checkpoint(
            task_id=task_id,
            workflow_instance_id=workflow_instance_id,
            node_name=node_name,
            state=waiting_state,
            status="WAITING_HUMAN_REVIEW",
        )
        self.save_node_state(
            task_id=task_id,
            node_name=node_name,
            state=waiting_state,
            status="WAITING_HUMAN_REVIEW",
        )

    def clear_controlled_checkpoint(self, task_id: str):
        self.redis_client.delete(WORKFLOW_CHECKPOINT_KEY.format(task_id=task_id))

    def _json_safe_copy(self, value: dict[str, Any]) -> dict[str, Any]:
        try:
            return json.loads(json.dumps(value, ensure_ascii=False, default=str))
        except Exception:
            return copy.deepcopy(value)
