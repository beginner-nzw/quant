import time
from typing import Callable

from app.messaging.kafka_producer import AiKafkaProducer


class NodeExecutor:
    def __init__(self, producer: AiKafkaProducer, progress_map: dict[str, int]):
        self.producer = producer
        self.progress_map = progress_map
        self.stage_map = {
            "planner_agent": "PLANNING",
            "intent_agent": "INTENT_UNDERSTANDING",
            "evidence_collection_agent": "EVIDENCE_COLLECTION",
            "financial_analysis_agent": "FINANCIAL_ANALYSIS",
            "risk_review_agent": "RISK_REVIEW",
            "report_generation_agent": "REPORT_GENERATION",
        }

    def wrap(self, node_name: str, agent_invoke: Callable[[dict], dict]):
        def _wrapped(state: dict) -> dict:
            start_ts = int(time.time() * 1000)

            # 先执行节点
            new_state = agent_invoke(state)

            # 兜底写状态字段，避免 agent 没写全
            new_state["current_node"] = node_name
            new_state["progress"] = self.progress_map.get(node_name, 0)

            # 从 state 中拿 stage；如果 agent 没写，就退化成节点名大写
            stage = self.stage_map.get(node_name) or new_state.get("current_stage") or node_name.upper()
            new_state["current_stage"] = stage

            # 节点完成后立即发送 status
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
                retry_count=new_state.get("retry_count", 0)
            )

            # 如果 agent_audits 没记录 duration，这里顺手补
            end_ts = int(time.time() * 1000)
            audits = new_state.get("agent_audits", [])
            if audits:
                last = audits[-1]
                if last.get("nodeCode") == node_name:
                    last["startTimestamp"] = last.get("startTimestamp") or start_ts
                    last["finishTimestamp"] = end_ts
                    last["durationMs"] = max(0, end_ts - last["startTimestamp"])

            return new_state

        return _wrapped
