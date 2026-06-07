import json
import time
import traceback

from confluent_kafka import Consumer

from app.agents.evidence_collection_agent import EvidenceCollectionAgent
from app.agents.event_extraction_agent import EventExtractionAgent
from app.agents.financial_analysis_agent import FinancialAnalysisAgent
from app.agents.industry_research_agent import IndustryResearchAgent
from app.agents.intent_agent import IntentAgent
from app.agents.planner_agent import PlannerAgent
from app.agents.report_generation_agent import ReportGenerationAgent
from app.agents.risk_review_agent import RiskReviewAgent
from app.agents.strategy_reasoning_agent import StrategyReasoningAgent
from app.agents.audit_compliance_agent import AuditComplianceAgent
from app.common.human_review import HumanReviewRequiredException
from app.common.task_routing import resolve_analysis_scope, resolve_task_type
from app.common.exceptions import TaskCancelledException, TaskTimeoutException
from app.config.settings import settings
from app.graph.workflow_builder import DEFAULT_TIMEOUTS, build_workflow_for_task
from app.messaging.consumer_readiness import CONSUMER_STATE_FILE, load_consumer_runtime_state
from app.messaging.kafka_producer import AiKafkaProducer
from app.messaging.message_models import AiTaskDispatchMessage
from app.observability import record_kafka_lag, record_task
from app.services.task_control_service import TaskControlService
from app.services.workflow_checkpoint_service import WorkflowCheckpointService
from app.services.task_loader_service import TaskLoaderService
from app.utils.logger import log_error, log_info


lag_log_state = {"last_log_ts": 0.0}
consumer_runtime_state = {
    "started": False,
    "subscribed": False,
    "running": False,
    "assigned": False,
    "assignmentCount": 0,
    "assignment": [],
    "lastAssignedAt": None,
    "lastRevokedAt": None,
    "lastPollAt": None,
    "lastMessageAt": None,
    "lastTaskId": None,
    "lastTraceId": None,
    "lastErrorAt": None,
    "lastError": None,
}


def _persist_consumer_state() -> None:
    try:
        CONSUMER_STATE_FILE.write_text(
            json.dumps(consumer_runtime_state, ensure_ascii=False),
            encoding="utf-8",
        )
    except Exception:
        pass


def _update_consumer_state(values: dict) -> None:
    consumer_runtime_state.update(values)
    _persist_consumer_state()


def _mark_consumer_error(error: Exception | str) -> None:
    _update_consumer_state({
        "running": False,
        "lastErrorAt": int(time.time() * 1000),
        "lastError": str(error),
    })


def get_consumer_runtime_state() -> dict:
    return load_consumer_runtime_state(consumer_runtime_state)


def _topic_partition_refs(partitions) -> list[str]:
    refs: list[str] = []
    for partition in partitions or []:
        refs.append(f"{partition.topic}:{partition.partition}")
    return refs


def _on_assign(_consumer, partitions) -> None:
    _update_consumer_state({
        "assigned": bool(partitions),
        "assignmentCount": len(partitions or []),
        "assignment": _topic_partition_refs(partitions),
        "lastAssignedAt": int(time.time() * 1000),
        "lastError": None,
        "lastErrorAt": None,
    })
    log_info(
        "",
        "[AI-ENGINE][KAFKA][ASSIGNED]",
        partitions=consumer_runtime_state["assignment"],
        groupId=settings.kafka.consumer_group,
    )


def _on_revoke(_consumer, partitions) -> None:
    _update_consumer_state({
        "assigned": False,
        "assignmentCount": 0,
        "assignment": [],
        "lastRevokedAt": int(time.time() * 1000),
    })
    log_error(
        "",
        "[AI-ENGINE][KAFKA][REVOKED]",
        partitions=_topic_partition_refs(partitions),
        groupId=settings.kafka.consumer_group,
    )


def observe_consumer_lag(consumer: Consumer) -> int:
    assignment = consumer.assignment()
    if not assignment:
        record_kafka_lag(0)
        return 0

    total_lag = 0
    try:
        positions = consumer.position(assignment)
        for partition in positions:
            if partition.offset < 0:
                continue
            _, high = consumer.get_watermark_offsets(partition, timeout=0.2)
            total_lag += max(0, high - partition.offset)
    except Exception as exc:
        log_error("", "[AI-ENGINE][KAFKA][LAG_OBSERVE_FAILED]", error=str(exc))
        return 0

    record_kafka_lag(total_lag)
    return total_lag


def log_lag_if_needed(trace_id: str, lag_messages: int) -> None:
    now = time.time()
    if lag_messages <= settings.kafka.max_lag_messages:
        return
    if now - lag_log_state["last_log_ts"] < settings.kafka.lag_log_interval_seconds:
        return
    lag_log_state["last_log_ts"] = now
    log_error(
        trace_id,
        "[AI-ENGINE][KAFKA][LAG_HIGH]",
        lagMessages=lag_messages,
        threshold=settings.kafka.max_lag_messages,
        handling="scale-workers-or-pause-low-priority-dispatch",
    )


def apply_checkpoint_control(init_state: dict, checkpoint: dict | None, control_signal: dict | None) -> dict:
    signal = control_signal or {}
    action = str(signal.get("action") or "").upper()
    if action not in {"RESUME", "RERUN_NODE"} or not checkpoint:
        return init_state

    checkpoint_state = checkpoint.get("state")
    if not isinstance(checkpoint_state, dict):
        return init_state

    restored_state = dict(checkpoint_state)
    restored_state.update({
        "status": "RUNNING",
        "trace_id": init_state.get("trace_id"),
        "tenant_id": init_state.get("tenant_id"),
        "biz_key": init_state.get("biz_key"),
        "retry_count": init_state.get("retry_count"),
        "actor_provenance": init_state.get("actor_provenance"),
        "resume_from_checkpoint": True,
        "resume_from_node": str(signal.get("resumeFromNode") or checkpoint.get("currentNode") or ""),
        "checkpoint_status": "RESUMED",
    })
    if action == "RERUN_NODE":
        rerun_node = str(signal.get("nodeName") or "").strip()
        restored_state["rerun_node"] = rerun_node
        restored_state["completed_nodes"] = [
            node for node in (restored_state.get("completed_nodes") or [])
            if node != rerun_node
        ]
        restored_state["checkpoint_status"] = "RERUN_REQUESTED"
    return restored_state


def publish_timeout_failure(
    *,
    producer,
    data,
    task_type: str,
    analysis_scope: str,
    event_id: str | None,
    actor_provenance: dict | None,
    error: Exception,
):
    producer.send_failed_status(
        task_id=data.taskId,
        trace_id=data.traceId,
        stage="TIMEOUT",
        node="workflow_timeout",
        workflow_instance_id=f"wf-{data.taskId}",
        tenant_id=data.tenantId,
        biz_key=data.bizKey,
        event_id=event_id,
        retry_count=data.retryCount,
        actor_provenance=actor_provenance,
    )
    producer.send_failed_result(
        task_id=data.taskId,
        trace_id=data.traceId,
        task_type=task_type,
        error_message=f"AI workflow timeout: {str(error)}",
        final_stage="TIMEOUT",
        workflow_instance_id=f"wf-{data.taskId}",
        tenant_id=data.tenantId,
        biz_key=data.bizKey,
        event_id=event_id,
        retry_count=data.retryCount,
        actor_provenance=actor_provenance,
        task_title=data.payload.taskTitle,
        analysis_scope=analysis_scope,
        target_type=data.payload.targetType,
        target_code=data.payload.targetCode,
        target_name=data.payload.targetName,
        priority=data.payload.priority,
        source_context={
            "sourceTaskId": data.payload.sourceTaskId,
            "sourceReportId": data.payload.sourceReportId,
            "sourceEventId": data.payload.sourceEventId,
            "sourceDomain": data.payload.sourceDomain,
            "sourceReviewStatus": data.payload.sourceReviewStatus,
        },
    )


def start_consumer():
    _update_consumer_state({
        "started": True,
        "running": False,
        "lastError": None,
        "lastErrorAt": None,
    })
    max_poll_interval_ms = max(
        300_000,
        (settings.app.workflow_timeout_seconds + settings.app.workflow_timeout_buffer_seconds + 60) * 1000,
        (sum(DEFAULT_TIMEOUTS.values()) + settings.app.workflow_timeout_buffer_seconds + 120) * 1000,
    )
    try:
        consumer = Consumer({
            "bootstrap.servers": settings.kafka.bootstrap_servers,
            "group.id": settings.kafka.consumer_group,
            "auto.offset.reset": "earliest",
            "enable.auto.commit": True,
            "max.poll.interval.ms": max_poll_interval_ms,
        })
        consumer.subscribe(
            [settings.kafka.topics.dispatch],
            on_assign=_on_assign,
            on_revoke=_on_revoke,
        )
        _update_consumer_state({
            "subscribed": True,
            "running": True,
        })
    except Exception as exc:
        _mark_consumer_error(exc)
        log_error("", "[AI-ENGINE][KAFKA][CONSUMER_START_FAILED]", error=str(exc))
        traceback.print_exc()
        return

    try:
        producer = AiKafkaProducer()
        task_loader_service = TaskLoaderService()
        task_control_service = TaskControlService()
        workflow_checkpoint_service = WorkflowCheckpointService()
        planner_agent = PlannerAgent()
        intent_agent = IntentAgent()
        evidence_collection_agent = EvidenceCollectionAgent()
        event_extraction_agent = EventExtractionAgent()
        industry_research_agent = IndustryResearchAgent()
        financial_analysis_agent = FinancialAnalysisAgent()
        risk_review_agent = RiskReviewAgent()
        strategy_reasoning_agent = StrategyReasoningAgent()
        audit_compliance_agent = AuditComplianceAgent()
        report_generation_agent = ReportGenerationAgent()
    except Exception as exc:
        _mark_consumer_error(exc)
        log_error("", "[AI-ENGINE][KAFKA][CONSUMER_DEPENDENCY_INIT_FAILED]", error=str(exc))
        traceback.print_exc()
        return

    while True:
        _update_consumer_state({
            "running": True,
            "lastPollAt": int(time.time() * 1000),
        })
        try:
            msg = consumer.poll(1.0)
            assignment = consumer.assignment()
            _update_consumer_state({
                "assigned": bool(assignment),
                "assignmentCount": len(assignment or []),
                "assignment": _topic_partition_refs(assignment),
            })
            if msg is None:
                continue
            if msg.error():
                _mark_consumer_error(msg.error())
                log_error("", "[AI-ENGINE][KAFKA][POLL_ERROR]", error=str(msg.error()))
                continue

            raw_message = msg.value().decode("utf-8")
            data = AiTaskDispatchMessage.model_validate(json.loads(raw_message))
            _update_consumer_state({
                "running": True,
                "lastMessageAt": int(time.time() * 1000),
                "lastTaskId": data.taskId,
                "lastTraceId": data.traceId,
                "lastError": None,
                "lastErrorAt": None,
            })
            log_lag_if_needed(data.traceId, observe_consumer_lag(consumer))
            event_id = data.eventId or data.payload.sourceEventId
            task_type = resolve_task_type(data.payload.taskType, data.payload.analysisScope)
            analysis_scope = resolve_analysis_scope(task_type, data.payload.analysisScope)
            actor_provenance = (
                data.payload.actorProvenance.model_dump(mode="json", exclude_none=True)
                if data.payload.actorProvenance is not None
                else None
            )
        except Exception as exc:
            _mark_consumer_error(exc)
            log_error("", "[AI-ENGINE][KAFKA][DISPATCH_MESSAGE_INVALID]", error=str(exc))
            traceback.print_exc()
            continue

        log_info(
            data.traceId,
            f"[AI-ENGINE] received task={data.taskId}, taskType={task_type}, target={data.payload.targetCode}"
        )

        try:
            start_ts = time.time()
            fallback_detail = {
                "taskId": data.taskId,
                "taskType": task_type,
                "taskTitle": data.payload.taskTitle,
                "targetType": data.payload.targetType,
                "targetCode": data.payload.targetCode,
                "targetName": data.payload.targetName,
                "priority": data.payload.priority,
                "sourceTaskId": data.payload.sourceTaskId,
                "sourceReportId": data.payload.sourceReportId,
                "sourceEventId": data.payload.sourceEventId,
                "sourceDomain": data.payload.sourceDomain,
                "sourceReviewStatus": data.payload.sourceReviewStatus,
                "analysisScope": analysis_scope,
            }
            task_context = task_loader_service.load_task_context(
                task_id=data.taskId,
                trace_id=data.traceId,
                fallback_detail=fallback_detail,
            )
            task_detail = task_context.get("taskDetail") or fallback_detail
            source_event = task_context.get("sourceEvent") or {}
            source_context = {
                "sourceTaskId": task_detail.get("sourceTaskId") or data.payload.sourceTaskId,
                "sourceReportId": task_detail.get("sourceReportId") or data.payload.sourceReportId,
                "sourceEventId": task_detail.get("sourceEventId") or event_id,
                "sourceDomain": task_detail.get("sourceDomain") or data.payload.sourceDomain,
                "sourceReviewStatus": task_detail.get("sourceReviewStatus") or data.payload.sourceReviewStatus,
            }
            if source_event:
                source_context.update({
                    "sourceEventType": source_event.get("eventType"),
                    "sourceEventTitle": source_event.get("eventTitle"),
                    "sourceEventSummary": source_event.get("eventSummary"),
                    "sourceEventImpactLevel": source_event.get("impactLevel"),
                    "sourceEventStatus": source_event.get("eventStatus"),
                    "sourceEventOccurredAt": source_event.get("occurredAt"),
                    "sourceEventSourceChannel": source_event.get("sourceChannel"),
                })
            workflow, workflow_timeout_seconds = build_workflow_for_task(
                task_type,
                planner_agent,
                intent_agent,
                evidence_collection_agent,
                event_extraction_agent,
                industry_research_agent,
                financial_analysis_agent,
                risk_review_agent,
                strategy_reasoning_agent,
                audit_compliance_agent,
                report_generation_agent,
                producer
            )

            control_signal = task_control_service.load_control_signal(data.taskId)
            checkpoint = workflow_checkpoint_service.load_checkpoint(data.taskId)
            init_state = {
                "task_id": data.taskId,
                "trace_id": data.traceId,
                "event_id": event_id,
                "task_type": task_type,
                "task_title": task_detail.get("taskTitle") or data.payload.taskTitle,
                "priority": task_detail.get("priority") or data.payload.priority,
                "target_type": task_detail.get("targetType") or data.payload.targetType,
                "target_code": task_detail.get("targetCode") or data.payload.targetCode,
                "target_name": task_detail.get("targetName") or data.payload.targetName,
                "tenant_id": data.tenantId,
                "biz_key": data.bizKey,
                "retry_count": data.retryCount,
                "actor_provenance": actor_provenance,
                "source_context": source_context,
                "analysis_scope": task_detail.get("analysisScope") or analysis_scope,
                "task_context": task_context,
                "source_task_context": task_context.get("sourceTask") or {},
                "market_context": {},
                "event_extraction_result": {},
                "industry_research_result": {},
                "strategy_result": {},
                "audit_result": {},
                "evidence_items": [],
                "workflow_instance_id": f"wf-{data.taskId}",
                "status": "RUNNING",
                "current_stage": "INIT",
                "current_node": "init",
                "progress": 0,
                "resume_from_checkpoint": False,
                "resume_from_node": "",
                "rerun_node": "",
                "completed_nodes": [],
                "branch_decisions": [],
                "evidence_quality": "LOW",
                "risk_level": "MEDIUM",
                "review_result": "APPROVED",
                "checkpoint_status": "NEW",
                "agent_audits": [],
                "evidence_refs": []
            }
            init_state = apply_checkpoint_control(
                init_state=init_state,
                checkpoint=checkpoint,
                control_signal=control_signal,
            )

            result_state = workflow.invoke(init_state)

            elapsed = time.time() - start_ts
            if elapsed > workflow_timeout_seconds:
                raise TaskTimeoutException(
                    f"workflow timeout after {workflow_timeout_seconds}s"
                )

            producer.send_result(result_state)
            producer.send_audit(result_state)
            record_task("succeeded")

            log_info(
                data.traceId,
                f"[AI-ENGINE] workflow finished task={data.taskId}, finalStatus={result_state.get('status')}"
            )

        except TaskCancelledException as e:
            log_info(data.traceId, f"[AI-ENGINE][CANCELLED] task={data.taskId} reason={e}")
            record_task("cancelled")

            producer.send_cancelled_status(
                task_id=data.taskId,
                trace_id=data.traceId,
                workflow_instance_id=f"wf-{data.taskId}",
                tenant_id=data.tenantId,
                biz_key=data.bizKey,
                event_id=event_id,
                retry_count=data.retryCount,
                actor_provenance=actor_provenance
            )
            producer.send_cancelled_result(
                task_id=data.taskId,
                trace_id=data.traceId,
                task_type=task_type,
                reason=f"AI workflow cancelled: {str(e)}",
                workflow_instance_id=f"wf-{data.taskId}",
                tenant_id=data.tenantId,
                biz_key=data.bizKey,
                event_id=event_id,
                retry_count=data.retryCount,
                actor_provenance=actor_provenance,
                task_title=data.payload.taskTitle,
                analysis_scope=analysis_scope,
                target_type=data.payload.targetType,
                target_code=data.payload.targetCode,
                target_name=data.payload.targetName,
                priority=data.payload.priority,
                source_context={
                    "sourceTaskId": data.payload.sourceTaskId,
                    "sourceReportId": data.payload.sourceReportId,
                    "sourceEventId": data.payload.sourceEventId,
                    "sourceDomain": data.payload.sourceDomain,
                    "sourceReviewStatus": data.payload.sourceReviewStatus
                }
            )

        except HumanReviewRequiredException as e:
            log_info(data.traceId, f"[AI-ENGINE][WAITING_HUMAN_REVIEW] task={data.taskId} reason={e}")
            record_task("waiting_human_review")
            producer.send_status(
                task_id=data.taskId,
                trace_id=data.traceId,
                stage="WAITING_HUMAN_REVIEW",
                node=e.node_name,
                progress=e.state.get("progress", 94),
                status="WAITING_HUMAN_REVIEW",
                workflow_instance_id=f"wf-{data.taskId}",
                tenant_id=data.tenantId,
                biz_key=data.bizKey,
                event_id=event_id,
                retry_count=data.retryCount,
                actor_provenance=actor_provenance,
            )

        except TaskTimeoutException as e:
            log_error(data.traceId, f"[AI-ENGINE][TIMEOUT] task={data.taskId} reason={e}")
            record_task("timeout")
            traceback.print_exc()

            publish_timeout_failure(
                producer=producer,
                data=data,
                task_type=task_type,
                analysis_scope=analysis_scope,
                event_id=event_id,
                actor_provenance=actor_provenance,
                error=e,
            )

        except Exception as e:
            log_error(data.traceId, f"[AI-ENGINE][ERROR] task={data.taskId} err={e}")
            record_task("failed")
            traceback.print_exc()

            producer.send_failed_status(
                task_id=data.taskId,
                trace_id=data.traceId,
                stage="FAILED",
                node="workflow_exception",
                workflow_instance_id=f"wf-{data.taskId}",
                tenant_id=data.tenantId,
                biz_key=data.bizKey,
                event_id=event_id,
                retry_count=data.retryCount,
                actor_provenance=actor_provenance
            )
            producer.send_failed_result(
                task_id=data.taskId,
                trace_id=data.traceId,
                task_type=task_type,
                error_message=f"AI workflow execution failed: {str(e)}",
                final_stage="FAILED",
                workflow_instance_id=f"wf-{data.taskId}",
                tenant_id=data.tenantId,
                biz_key=data.bizKey,
                event_id=event_id,
                retry_count=data.retryCount,
                actor_provenance=actor_provenance,
                task_title=data.payload.taskTitle,
                analysis_scope=analysis_scope,
                target_type=data.payload.targetType,
                target_code=data.payload.targetCode,
                target_name=data.payload.targetName,
                priority=data.payload.priority,
                source_context={
                    "sourceTaskId": data.payload.sourceTaskId,
                    "sourceReportId": data.payload.sourceReportId,
                    "sourceEventId": data.payload.sourceEventId,
                    "sourceDomain": data.payload.sourceDomain,
                    "sourceReviewStatus": data.payload.sourceReviewStatus
                }
            )
