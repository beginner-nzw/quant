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
    actor_provenance: Dict[str, Any]
    source_context: Dict[str, Any]
    analysis_scope: str
    task_context: Dict[str, Any]
    source_task_context: Dict[str, Any]
    market_context: Dict[str, Any]

    status: str
    current_stage: str
    current_node: str
    progress: int
    resume_from_checkpoint: bool
    resume_from_node: str
    rerun_node: str
    completed_nodes: List[str]
    branch_decisions: List[Dict[str, Any]]
    evidence_quality: str
    risk_level: str
    review_result: str
    checkpoint_status: str

    workflow_instance_id: str
    need_human_review: bool
    waiting_human_review: bool
    human_review_gate: Dict[str, Any]

    plan_result: Dict[str, Any]
    intent_result: Dict[str, Any]
    event_extraction_result: Dict[str, Any]
    industry_research_result: Dict[str, Any]
    financial_result: Dict[str, Any]
    risk_result: Dict[str, Any]
    strategy_result: Dict[str, Any]
    audit_result: Dict[str, Any]
    report_result: Dict[str, Any]
    evidence_items: List[Dict[str, Any]]
    evidence_refs: List[str]
    agent_audits: List[Dict[str, Any]]
