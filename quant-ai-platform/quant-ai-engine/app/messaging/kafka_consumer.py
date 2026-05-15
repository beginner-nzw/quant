import json
import time
import traceback

from confluent_kafka import Consumer

from app.agents.evidence_collection_agent import EvidenceCollectionAgent
from app.agents.financial_analysis_agent import FinancialAnalysisAgent
from app.agents.intent_agent import IntentAgent
from app.agents.planner_agent import PlannerAgent
from app.agents.report_generation_agent import ReportGenerationAgent
from app.agents.risk_review_agent import RiskReviewAgent
from app.common.task_routing import resolve_analysis_scope, resolve_task_type
from app.common.exceptions import TaskCancelledException, TaskTimeoutException
from app.config.settings import settings
from app.graph.workflow_builder import build_workflow_for_task
from app.messaging.kafka_producer import AiKafkaProducer
from app.messaging.message_models import AiTaskDispatchMessage
from app.services.task_loader_service import TaskLoaderService
from app.utils.logger import log_error, log_info


def start_consumer():
    consumer = Consumer({
        "bootstrap.servers": settings.kafka.bootstrap_servers,
        "group.id": settings.kafka.consumer_group,
        "auto.offset.reset": "earliest"
    })
    consumer.subscribe([settings.kafka.topics.dispatch])

    producer = AiKafkaProducer()
    task_loader_service = TaskLoaderService()
    planner_agent = PlannerAgent()
    intent_agent = IntentAgent()
    evidence_collection_agent = EvidenceCollectionAgent()
    financial_analysis_agent = FinancialAnalysisAgent()
    risk_review_agent = RiskReviewAgent()
    report_generation_agent = ReportGenerationAgent()

    while True:
        msg = consumer.poll(1.0)
        if msg is None:
            continue
        if msg.error():
            continue

        data = AiTaskDispatchMessage.model_validate(json.loads(msg.value().decode("utf-8")))
        event_id = data.eventId or data.payload.sourceEventId
        task_type = resolve_task_type(data.payload.taskType, data.payload.analysisScope)
        analysis_scope = resolve_analysis_scope(task_type, data.payload.analysisScope)

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
                financial_analysis_agent,
                risk_review_agent,
                report_generation_agent,
                producer
            )

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
                "source_context": source_context,
                "analysis_scope": task_detail.get("analysisScope") or analysis_scope,
                "task_context": task_context,
                "source_task_context": task_context.get("sourceTask") or {},
                "market_context": {},
                "evidence_items": [],
                "workflow_instance_id": f"wf-{data.taskId}",
                "status": "RUNNING",
                "current_stage": "INIT",
                "current_node": "init",
                "progress": 0,
                "agent_audits": [],
                "evidence_refs": []
            }

            result_state = workflow.invoke(init_state)

            elapsed = time.time() - start_ts
            if elapsed > workflow_timeout_seconds:
                raise TaskTimeoutException(
                    f"workflow timeout after {workflow_timeout_seconds}s"
                )

            producer.send_result(result_state)
            producer.send_audit(result_state)

            log_info(
                data.traceId,
                f"[AI-ENGINE] workflow finished task={data.taskId}, finalStatus={result_state.get('status')}"
            )

        except TaskCancelledException as e:
            log_info(data.traceId, f"[AI-ENGINE][CANCELLED] task={data.taskId} reason={e}")

            producer.send_cancelled_status(
                task_id=data.taskId,
                trace_id=data.traceId,
                workflow_instance_id=f"wf-{data.taskId}",
                tenant_id=data.tenantId,
                biz_key=data.bizKey,
                event_id=event_id,
                retry_count=data.retryCount
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

        except TaskTimeoutException as e:
            log_error(data.traceId, f"[AI-ENGINE][TIMEOUT] task={data.taskId} reason={e}")
            traceback.print_exc()

            producer.send_failed_status(
                task_id=data.taskId,
                trace_id=data.traceId,
                stage="TIMEOUT",
                node="workflow_timeout",
                workflow_instance_id=f"wf-{data.taskId}",
                tenant_id=data.tenantId,
                biz_key=data.bizKey,
                event_id=event_id,
                retry_count=data.retryCount
            )
            producer.send_failed_result(
                task_id=data.taskId,
                trace_id=data.traceId,
                task_type=task_type,
                error_message=f"AI workflow timeout: {str(e)}",
                final_stage="TIMEOUT",
                workflow_instance_id=f"wf-{data.taskId}",
                tenant_id=data.tenantId,
                biz_key=data.bizKey,
                event_id=event_id,
                retry_count=data.retryCount,
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

        except Exception as e:
            log_error(data.traceId, f"[AI-ENGINE][ERROR] task={data.taskId} err={e}")
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
                retry_count=data.retryCount
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
