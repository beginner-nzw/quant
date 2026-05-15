from typing import TypedDict, Dict, Any, List


class WorkflowState(TypedDict, total=False):
    task_id: str
    trace_id: str
    task_type: str
    task_title: str
    priority: str
    target_type: str
    target_code: str
    target_name: str
    tenant_id: str
    biz_key: str
    retry_count: int
    source_context: Dict[str, Any]
    analysis_scope: str
    task_context: Dict[str, Any]
    source_task_context: Dict[str, Any]
    market_context: Dict[str, Any]

    status: str
    current_stage: str
    current_node: str
    progress: int

    workflow_instance_id: str
    need_human_review: bool

    plan_result: Dict[str, Any]
    intent_result: Dict[str, Any]
    financial_result: Dict[str, Any]
    risk_result: Dict[str, Any]
    report_result: Dict[str, Any]
    evidence_items: List[Dict[str, Any]]
    evidence_refs: List[str]
    agent_audits: List[Dict[str, Any]]
