import json
import time
import uuid
from typing import Any

from confluent_kafka import Producer

from app.config.settings import settings
from app.messaging.message_models import (
    AiTaskAuditMessage,
    AiTaskAuditPayload,
    AiTaskMessageEnvelope,
    AiTaskResultMessage,
    AiTaskResultPayload,
    AiTaskStatusMessage,
    AiTaskStatusPayload,
)


class AiKafkaProducer:
    def __init__(self):
        self.producer = Producer({
            "bootstrap.servers": settings.kafka.bootstrap_servers
        })

    def _delivery_report(self, err, msg):
        if err is not None:
            print(f"[AI-ENGINE][KAFKA][ERROR] topic={msg.topic()} key={msg.key()} err={err}")
        else:
            print(f"[AI-ENGINE][KAFKA][OK] topic={msg.topic()} partition={msg.partition()} offset={msg.offset()}")

    def _send(self, topic: str, key: str, value: dict[str, Any]):
        self.producer.produce(
            topic,
            key=key,
            value=json.dumps(value, ensure_ascii=False).encode("utf-8"),
            callback=self._delivery_report
        )
        self.producer.flush()

    def _build_envelope(
        self,
        *,
        task_id: str,
        trace_id: str,
        event_id: str | None = None,
        message_type: str,
        tenant_id: str | None = None,
        biz_key: str | None = None,
        retry_count: int = 0
    ) -> AiTaskMessageEnvelope:
        return AiTaskMessageEnvelope(
            messageId=str(uuid.uuid4()),
            traceId=trace_id,
            taskId=task_id,
            eventId=event_id,
            messageType=message_type,
            sourceService="python-ai-engine",
            targetService="ai-orchestration-service",
            tenantId=tenant_id,
            bizKey=biz_key,
            timestamp=int(time.time() * 1000),
            version="1.0",
            retryCount=max(0, int(retry_count or 0))
        )

    def _normalize_text_list(self, value: Any) -> list[str]:
        if not isinstance(value, list):
            return []
        normalized = []
        for item in value:
            if item is None:
                continue
            text = str(item).strip()
            if text:
                normalized.append(text)
        return normalized

    def _extract_confidence_score(self, state: dict[str, Any]) -> float:
        report_result = state.get("report_result", {})
        report_score = report_result.get("confidenceScore")
        if isinstance(report_score, (int, float)):
            return max(0.0, min(1.0, float(report_score)))

        audits = state.get("agent_audits", [])
        scores = [
            float(item["confidenceScore"])
            for item in audits
            if isinstance(item, dict) and isinstance(item.get("confidenceScore"), (int, float))
        ]
        if not scores:
            return 0.0
        average_score = sum(scores) / len(scores)
        return round(max(0.0, min(1.0, average_score)), 4)

    def _extract_need_human_review(self, state: dict[str, Any]) -> bool:
        report_result = state.get("report_result", {})
        if isinstance(report_result.get("needHumanReview"), bool):
            return report_result["needHumanReview"]

        if isinstance(state.get("need_human_review"), bool):
            return state["need_human_review"]

        risk_result = state.get("risk_result", {})
        if isinstance(risk_result.get("needHumanReview"), bool):
            return risk_result["needHumanReview"]

        audits = state.get("agent_audits", [])
        return any(bool(item.get("needHumanReview")) for item in audits if isinstance(item, dict))

    def _extract_risk_warnings(self, state: dict[str, Any]) -> list[str]:
        report_result = state.get("report_result", {})
        report_risk_warnings = self._normalize_text_list(report_result.get("riskWarnings"))
        if report_risk_warnings:
            return report_risk_warnings

        risk_result = state.get("risk_result", {})
        risk_warnings = self._normalize_text_list(risk_result.get("riskWarnings"))
        if risk_warnings:
            return risk_warnings

        return self._normalize_text_list(risk_result.get("riskPoints"))

    def _build_result_message(
        self,
        *,
        task_id: str,
        trace_id: str,
        task_type: str,
        final_status: str,
        final_stage: str | None = None,
        summary: str,
        workflow_instance_id: str | None = None,
        tenant_id: str | None = None,
        biz_key: str | None = None,
        retry_count: int = 0,
        task_title: str | None = None,
        analysis_scope: str | None = None,
        target_type: str | None = None,
        target_code: str | None = None,
        target_name: str | None = None,
        priority: str | None = None,
        source_context: dict[str, Any] | None = None,
        confidence_score: float = 0.0,
        need_human_review: bool = False,
        risk_warnings: list[str] | None = None,
        report_meta: dict[str, Any] | None = None
    ) -> AiTaskResultMessage:
        source_context = source_context or {}
        envelope = self._build_envelope(
            task_id=task_id,
            trace_id=trace_id,
            event_id=source_context.get("sourceEventId"),
            message_type="AI_TASK_RESULT",
            tenant_id=tenant_id,
            biz_key=biz_key,
            retry_count=retry_count
        )
        payload = AiTaskResultPayload(
            workflowInstanceId=workflow_instance_id,
            taskType=task_type,
            taskTitle=task_title,
            analysisScope=analysis_scope,
            targetType=target_type,
            targetCode=target_code,
            targetName=target_name,
            priority=priority,
            sourceTaskId=source_context.get("sourceTaskId"),
            sourceReportId=source_context.get("sourceReportId"),
            sourceEventId=source_context.get("sourceEventId"),
            sourceDomain=source_context.get("sourceDomain"),
            sourceReviewStatus=source_context.get("sourceReviewStatus"),
            finalStatus=final_status,
            finalStage=final_stage,
            summary=summary,
            confidenceScore=max(0.0, min(1.0, float(confidence_score))),
            needHumanReview=bool(need_human_review),
            riskWarnings=risk_warnings or [],
            reportMeta=report_meta or {},
            resultRef=""
        )
        return AiTaskResultMessage(
            **envelope.model_dump(),
            payload=payload
        )

    def send_status(
        self,
        task_id: str,
        trace_id: str,
        stage: str,
        node: str,
        progress: int,
        status: str,
        workflow_instance_id: str | None = None,
        tenant_id: str | None = None,
        biz_key: str | None = None,
        event_id: str | None = None,
        retry_count: int = 0
    ):
        envelope = self._build_envelope(
            task_id=task_id,
            trace_id=trace_id,
            event_id=event_id,
            message_type="AI_TASK_STATUS",
            tenant_id=tenant_id,
            biz_key=biz_key,
            retry_count=retry_count
        )
        message = AiTaskStatusMessage(
            **envelope.model_dump(),
            payload=AiTaskStatusPayload(
                workflowInstanceId=workflow_instance_id,
                status=status,
                currentStage=stage,
                currentNode=node,
                progress=progress
            )
        )
        self._send(
            settings.kafka.topics.status,
            task_id,
            message.model_dump(mode="json", exclude_none=True)
        )

    def send_result(self, state: dict[str, Any]):
        report_result = state.get("report_result", {})
        message = self._build_result_message(
            task_id=state["task_id"],
            trace_id=state["trace_id"],
            task_type=state["task_type"],
            final_status=state["status"],
            final_stage="FINISHED",
            summary=str(report_result.get("summary") or ""),
            workflow_instance_id=state.get("workflow_instance_id"),
            tenant_id=state.get("tenant_id"),
            biz_key=state.get("biz_key"),
            retry_count=state.get("retry_count", 0),
            task_title=state.get("task_title"),
            analysis_scope=state.get("analysis_scope"),
            target_type=state.get("target_type"),
            target_code=state.get("target_code"),
            target_name=state.get("target_name"),
            priority=state.get("priority"),
            source_context=state.get("source_context"),
            confidence_score=self._extract_confidence_score(state),
            need_human_review=self._extract_need_human_review(state),
            risk_warnings=self._extract_risk_warnings(state),
            report_meta=report_result
        )
        self._send(
            settings.kafka.topics.result,
            state["task_id"],
            message.model_dump(mode="json", exclude_none=True)
        )

    def send_audit(self, state: dict[str, Any]):
        envelope = self._build_envelope(
            task_id=state["task_id"],
            trace_id=state["trace_id"],
            event_id=(state.get("source_context") or {}).get("sourceEventId") or state.get("event_id"),
            message_type="AI_TASK_AUDIT",
            tenant_id=state.get("tenant_id"),
            biz_key=state.get("biz_key"),
            retry_count=state.get("retry_count", 0)
        )
        message = AiTaskAuditMessage(
            **envelope.model_dump(),
            payload=AiTaskAuditPayload(
                workflowInstanceId=state.get("workflow_instance_id"),
                agents=state.get("agent_audits", []),
                reviewSuggestion=state.get("review_suggestion")
                or "Workflow completed without additional review suggestion.",
                evidenceRefs=self._normalize_text_list(state.get("evidence_refs"))
            )
        )
        self._send(
            settings.kafka.topics.audit,
            state["task_id"],
            message.model_dump(mode="json", exclude_none=True)
        )

    def send_failed_result(
        self,
        task_id: str,
        trace_id: str,
        task_type: str,
        error_message: str,
        final_stage: str = "FAILED",
        workflow_instance_id: str | None = None,
        tenant_id: str | None = None,
        biz_key: str | None = None,
        retry_count: int = 0,
        task_title: str | None = None,
        analysis_scope: str | None = None,
        target_type: str | None = None,
        target_code: str | None = None,
        target_name: str | None = None,
        priority: str | None = None,
        source_context: dict[str, Any] | None = None
    ):
        message = self._build_result_message(
            task_id=task_id,
            trace_id=trace_id,
            task_type=task_type,
            final_status="FAILED",
            final_stage=final_stage,
            summary=error_message,
            workflow_instance_id=workflow_instance_id,
            tenant_id=tenant_id,
            biz_key=biz_key,
            retry_count=retry_count,
            task_title=task_title,
            analysis_scope=analysis_scope,
            target_type=target_type,
            target_code=target_code,
            target_name=target_name,
            priority=priority,
            source_context=source_context
        )
        self._send(
            settings.kafka.topics.result,
            task_id,
            message.model_dump(mode="json", exclude_none=True)
        )

    def send_failed_status(
        self,
        task_id: str,
        trace_id: str,
        stage: str,
        node: str,
        workflow_instance_id: str | None = None,
        tenant_id: str | None = None,
        biz_key: str | None = None,
        event_id: str | None = None,
        retry_count: int = 0
    ):
        self.send_status(
            task_id=task_id,
            trace_id=trace_id,
            stage=stage,
            node=node,
            progress=100,
            status="FAILED",
            workflow_instance_id=workflow_instance_id,
            tenant_id=tenant_id,
            biz_key=biz_key,
            event_id=event_id,
            retry_count=retry_count
        )

    def send_cancelled_status(
        self,
        task_id: str,
        trace_id: str,
        workflow_instance_id: str | None = None,
        tenant_id: str | None = None,
        biz_key: str | None = None,
        event_id: str | None = None,
        retry_count: int = 0
    ):
        self.send_status(
            task_id=task_id,
            trace_id=trace_id,
            stage="CANCELLED",
            node="workflow_cancelled",
            progress=100,
            status="CANCELLED",
            workflow_instance_id=workflow_instance_id,
            tenant_id=tenant_id,
            biz_key=biz_key,
            event_id=event_id,
            retry_count=retry_count
        )

    def send_cancelled_result(
        self,
        task_id: str,
        trace_id: str,
        task_type: str,
        reason: str,
        workflow_instance_id: str | None = None,
        tenant_id: str | None = None,
        biz_key: str | None = None,
        retry_count: int = 0,
        task_title: str | None = None,
        analysis_scope: str | None = None,
        target_type: str | None = None,
        target_code: str | None = None,
        target_name: str | None = None,
        priority: str | None = None,
        source_context: dict[str, Any] | None = None
    ):
        message = self._build_result_message(
            task_id=task_id,
            trace_id=trace_id,
            task_type=task_type,
            final_status="CANCELLED",
            final_stage="CANCELLED",
            summary=reason,
            workflow_instance_id=workflow_instance_id,
            tenant_id=tenant_id,
            biz_key=biz_key,
            retry_count=retry_count,
            task_title=task_title,
            analysis_scope=analysis_scope,
            target_type=target_type,
            target_code=target_code,
            target_name=target_name,
            priority=priority,
            source_context=source_context
        )
        self._send(
            settings.kafka.topics.result,
            task_id,
            message.model_dump(mode="json", exclude_none=True)
        )
