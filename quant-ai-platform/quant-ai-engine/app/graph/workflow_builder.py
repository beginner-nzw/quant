import math

from langgraph.graph import END, StateGraph

from app.common.task_routing import (
    TASK_TYPE_AUDIT_REVIEW,
    TASK_TYPE_FOLLOW_UP_RESEARCH,
    TASK_TYPE_REPORT_REVIEW,
    TASK_TYPE_RISK_REVIEW,
    TASK_TYPE_STOCK_RESEARCH,
)
from app.config.settings import settings
from app.common.human_review import HumanReviewRequiredException
from app.graph.node_executor import NodeExecutor
from app.graph.state import WorkflowState
from app.services.agent_config_repository import AgentConfigRepository
from app.services.timeout_executor import TimeoutExecutor
from app.services.workflow_checkpoint_service import WorkflowCheckpointService
from app.services.workflow_config_repository import WorkflowConfigRepository

timeout_executor = TimeoutExecutor()
agent_config_repository = AgentConfigRepository()
workflow_config_repository = WorkflowConfigRepository()
workflow_checkpoint_service = WorkflowCheckpointService()

MODEL_BACKED_NODES = {
    "planner_agent",
    "intent_agent",
    "event_extraction_agent",
    "industry_research_agent",
    "financial_analysis_agent",
    "risk_review_agent",
    "strategy_reasoning_agent",
    "audit_compliance_agent",
    "report_generation_agent",
}
HUMAN_REVIEW_GATE_NODE = "human_review_gate"
REQUIRED_AGENT_CODES = {
    "report_generation_agent",
}
DEFAULT_TIMEOUTS = {
    "planner_agent": 75,
    "intent_agent": 75,
    "evidence_collection_agent": 75,
    "event_extraction_agent": 75,
    "industry_research_agent": 75,
    "financial_analysis_agent": 75,
    "risk_review_agent": 75,
    "strategy_reasoning_agent": 75,
    "audit_compliance_agent": 75,
    "report_generation_agent": 75,
}
DEFAULT_PROGRESS = {
    "planner_agent": 10,
    "intent_agent": 35,
    "evidence_collection_agent": 55,
    "event_extraction_agent": 60,
    "industry_research_agent": 68,
    "financial_analysis_agent": 70,
    "risk_review_agent": 82,
    "strategy_reasoning_agent": 88,
    "audit_compliance_agent": 92,
    "report_generation_agent": 95,
    HUMAN_REVIEW_GATE_NODE: 94,
}
DEFAULT_WORKFLOW_SEQUENCES = {
    TASK_TYPE_STOCK_RESEARCH: [
        "planner_agent",
        "intent_agent",
        "evidence_collection_agent",
        "event_extraction_agent",
        "industry_research_agent",
        "financial_analysis_agent",
        "risk_review_agent",
        "strategy_reasoning_agent",
        "audit_compliance_agent",
        "report_generation_agent",
    ],
    TASK_TYPE_FOLLOW_UP_RESEARCH: [
        "planner_agent",
        "intent_agent",
        "evidence_collection_agent",
        "event_extraction_agent",
        "industry_research_agent",
        "financial_analysis_agent",
        "strategy_reasoning_agent",
        "audit_compliance_agent",
        "report_generation_agent",
    ],
    TASK_TYPE_REPORT_REVIEW: [
        "planner_agent",
        "intent_agent",
        "evidence_collection_agent",
        "event_extraction_agent",
        "industry_research_agent",
        "risk_review_agent",
        "strategy_reasoning_agent",
        "audit_compliance_agent",
        "report_generation_agent",
    ],
    TASK_TYPE_AUDIT_REVIEW: [
        "planner_agent",
        "intent_agent",
        "evidence_collection_agent",
        "event_extraction_agent",
        "industry_research_agent",
        "risk_review_agent",
        "strategy_reasoning_agent",
        "audit_compliance_agent",
        "report_generation_agent",
    ],
    TASK_TYPE_RISK_REVIEW: [
        "planner_agent",
        "evidence_collection_agent",
        "event_extraction_agent",
        "industry_research_agent",
        "risk_review_agent",
        "strategy_reasoning_agent",
        "audit_compliance_agent",
        "report_generation_agent",
    ],
}


def wrap_with_timeout(fn, timeout_seconds: int):
    def _wrapped(state: WorkflowState):
        return timeout_executor.run_with_timeout(fn, state, timeout_seconds)

    return _wrapped


def resolve_node_timeout_seconds(node_name: str, fallback_timeout_seconds: int) -> int:
    agent_config = agent_config_repository.load_agent(node_name)
    if agent_config and agent_config.get("timeoutSeconds") is not None:
        return agent_config_repository.timeout_seconds(node_name, fallback_timeout_seconds)

    timeout_seconds = fallback_timeout_seconds
    if settings.model.enabled and node_name in MODEL_BACKED_NODES:
        timeout_seconds = max(
            timeout_seconds,
            int(
                math.ceil(
                    settings.model.request_timeout_seconds
                    + settings.app.model_node_timeout_buffer_seconds
                )
            ),
        )
    return timeout_seconds


def resolve_workflow_timeout_seconds(enabled_chain: list[str], fallback_timeouts: dict[str, int]) -> int:
    required_timeout_seconds = sum(
        resolve_node_timeout_seconds(node_name, fallback_timeouts.get(node_name, 10))
        for node_name in enabled_chain
    )
    required_timeout_seconds += settings.app.workflow_timeout_buffer_seconds
    return max(settings.app.workflow_timeout_seconds, required_timeout_seconds)


def build_workflow_for_task(
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
):
    available_agents = {
        "planner_agent": planner_agent,
        "intent_agent": intent_agent,
        "evidence_collection_agent": evidence_collection_agent,
        "event_extraction_agent": event_extraction_agent,
        "industry_research_agent": industry_research_agent,
        "financial_analysis_agent": financial_analysis_agent,
        "risk_review_agent": risk_review_agent,
        "strategy_reasoning_agent": strategy_reasoning_agent,
        "audit_compliance_agent": audit_compliance_agent,
        "report_generation_agent": report_generation_agent,
    }

    configured_sequence = resolve_configured_sequence(task_type)
    return build_conditional_workflow(
        producer,
        available_agents,
        configured_sequence,
        DEFAULT_TIMEOUTS,
        DEFAULT_PROGRESS,
    )


def build_conditional_workflow(
    producer,
    available_agents: dict[str, object],
    default_chain: list[str],
    fallback_timeouts: dict[str, int],
    progress_map: dict[str, int],
):
    enabled_chain = resolve_enabled_chain(default_chain)
    node_executor = NodeExecutor(producer, progress_map)
    graph = StateGraph(WorkflowState)

    for node_name in enabled_chain:
        agent = available_agents[node_name]
        effective_timeout_seconds = resolve_node_timeout_seconds(
            node_name,
            fallback_timeouts.get(node_name, 10),
        )
        graph.add_node(
            node_name,
            wrap_with_timeout(
                node_executor.wrap(node_name, agent.invoke),
                effective_timeout_seconds,
            ),
        )

    if should_include_human_review_gate(enabled_chain):
        graph.add_node(
            HUMAN_REVIEW_GATE_NODE,
            build_human_review_gate_node(node_executor.checkpoint_service),
        )

    graph.set_entry_point(enabled_chain[0])
    add_conditional_edges(graph, enabled_chain)
    return graph.compile(), resolve_workflow_timeout_seconds(enabled_chain, fallback_timeouts)


build_linear_workflow = build_conditional_workflow


def add_conditional_edges(graph: StateGraph, enabled_chain: list[str]):
    for index, node_name in enumerate(enabled_chain):
        if node_name == enabled_chain[-1]:
            graph.add_edge(node_name, END)
            continue
        candidates = enabled_chain[index + 1:]
        route_map = {candidate: candidate for candidate in candidates}
        if should_include_human_review_gate(candidates):
            route_map[HUMAN_REVIEW_GATE_NODE] = HUMAN_REVIEW_GATE_NODE
        route_map["__end__"] = END
        graph.add_conditional_edges(
            node_name,
            lambda state, current=node_name, remaining=candidates: route_next_node(state, current, remaining),
            route_map,
        )
    if should_include_human_review_gate(enabled_chain):
        graph.add_edge(HUMAN_REVIEW_GATE_NODE, END)


def route_next_node(state: dict, current_node: str, remaining_nodes: list[str]) -> str:
    if not remaining_nodes:
        return "__end__"

    decision = resolve_branch_decision(state, current_node, remaining_nodes)
    state.setdefault("branch_decisions", []).append(decision)
    workflow_checkpoint_service.save_checkpoint(
        task_id=state["task_id"],
        workflow_instance_id=state.get("workflow_instance_id"),
        node_name=current_node,
        state=state,
        status=state.get("checkpoint_status") or "READY",
    )
    return decision["nextNode"]


def resolve_branch_decision(state: dict, current_node: str, remaining_nodes: list[str]) -> dict:
    task_type = state.get("task_type")
    evidence_quality = resolve_evidence_quality(state)
    risk_level = resolve_risk_level(state)
    review_result = resolve_review_result(state)

    state["evidence_quality"] = evidence_quality
    state["risk_level"] = risk_level
    state["review_result"] = review_result

    next_node = remaining_nodes[0]
    reason = "DEFAULT_SEQUENCE"

    if should_wait_for_human_review(state, current_node, remaining_nodes):
        next_node = HUMAN_REVIEW_GATE_NODE
        reason = "WAITING_HUMAN_DECISION"
    elif current_node == "planner_agent" and task_type == TASK_TYPE_RISK_REVIEW:
        next_node = first_available(remaining_nodes, "evidence_collection_agent", "risk_review_agent", fallback=next_node)
        reason = "TASK_TYPE_RISK_REVIEW"
    elif current_node == "intent_agent" and task_type in {TASK_TYPE_REPORT_REVIEW, TASK_TYPE_AUDIT_REVIEW}:
        next_node = first_available(remaining_nodes, "evidence_collection_agent", "risk_review_agent", fallback=next_node)
        reason = "TASK_TYPE_REVIEW"
    elif current_node == "evidence_collection_agent":
        if evidence_quality == "LOW":
            next_node = first_available(remaining_nodes, "event_extraction_agent", "industry_research_agent", fallback=next_node)
            reason = "LOW_EVIDENCE_QUALITY"
        elif task_type == TASK_TYPE_FOLLOW_UP_RESEARCH:
            next_node = first_available(remaining_nodes, "financial_analysis_agent", "strategy_reasoning_agent", fallback=next_node)
            reason = "FOLLOW_UP_RESEARCH_FAST_PATH"
    elif current_node == "financial_analysis_agent" and risk_level == "LOW" and evidence_quality == "HIGH":
        next_node = first_available(remaining_nodes, "strategy_reasoning_agent", "report_generation_agent", fallback=next_node)
        reason = "LOW_RISK_HIGH_EVIDENCE_FAST_PATH"
    elif current_node == "risk_review_agent":
        if risk_level == "HIGH" or bool(state.get("need_human_review")):
            next_node = first_available(remaining_nodes, "audit_compliance_agent", "report_generation_agent", fallback=next_node)
            reason = "HIGH_RISK_OR_HUMAN_REVIEW"
        else:
            next_node = first_available(remaining_nodes, "strategy_reasoning_agent", "report_generation_agent", fallback=next_node)
            reason = "RISK_ACCEPTED"
    elif current_node == "strategy_reasoning_agent":
        if review_result in {"REJECTED", "REVIEW_REQUIRED"} or risk_level == "HIGH":
            next_node = first_available(remaining_nodes, "audit_compliance_agent", "report_generation_agent", fallback=next_node)
            reason = "REVIEW_OR_HIGH_RISK_AUDIT"
        elif evidence_quality == "HIGH" and risk_level == "LOW":
            next_node = first_available(remaining_nodes, "report_generation_agent", fallback=next_node)
            reason = "REPORT_READY"
    elif current_node == "audit_compliance_agent":
        next_node = first_available(remaining_nodes, "report_generation_agent", fallback=next_node)
        reason = "AUDIT_SUPPORT_COMPLETE"

    return {
        "fromNode": current_node,
        "nextNode": next_node,
        "reason": reason,
        "taskType": task_type,
        "evidenceQuality": evidence_quality,
        "riskLevel": risk_level,
        "reviewResult": review_result,
    }


def should_include_human_review_gate(nodes: list[str]) -> bool:
    return "audit_compliance_agent" in nodes or "report_generation_agent" in nodes


def should_wait_for_human_review(state: dict, current_node: str, remaining_nodes: list[str]) -> bool:
    if bool(state.get("resume_from_checkpoint")) or bool(state.get("rerun_node")):
        return False
    if "report_generation_agent" not in remaining_nodes:
        return False
    if current_node not in {"risk_review_agent", "audit_compliance_agent"}:
        return False
    if bool(state.get("waiting_human_review")):
        return False
    if current_node == "risk_review_agent" and (
        resolve_risk_level(state) == "HIGH" or bool(state.get("need_human_review"))
    ):
        return True
    if current_node == "audit_compliance_agent":
        return resolve_review_result(state) in {"REJECTED", "REVIEW_REQUIRED"}
    return False


def build_human_review_gate_node(checkpoint_service: WorkflowCheckpointService):
    def _gate(state: dict) -> dict:
        reason = "Human decision required before report generation"
        state["current_node"] = HUMAN_REVIEW_GATE_NODE
        state["current_stage"] = "WAITING_HUMAN_REVIEW"
        state["progress"] = DEFAULT_PROGRESS[HUMAN_REVIEW_GATE_NODE]
        state["status"] = "WAITING_HUMAN_REVIEW"
        state["waiting_human_review"] = True
        checkpoint_service.mark_waiting_human_review(
            task_id=state["task_id"],
            workflow_instance_id=state.get("workflow_instance_id"),
            node_name=HUMAN_REVIEW_GATE_NODE,
            state=state,
            reason=reason,
        )
        raise HumanReviewRequiredException(state, HUMAN_REVIEW_GATE_NODE, reason)

    return _gate


def first_available(remaining_nodes: list[str], *preferred: str, fallback: str) -> str:
    for node_name in preferred:
        if node_name in remaining_nodes:
            return node_name
    return fallback


def resolve_evidence_quality(state: dict) -> str:
    evidence_items = state.get("evidence_items") or []
    evidence_refs = state.get("evidence_refs") or []
    structured_count = len(evidence_items) if isinstance(evidence_items, list) else 0
    ref_count = len(evidence_refs) if isinstance(evidence_refs, list) else 0
    traceable_refs = [
        ref for ref in evidence_refs
        if isinstance(ref, str) and ":" in ref
    ] if isinstance(evidence_refs, list) else []
    if structured_count >= 3 and len(traceable_refs) >= 2:
        return "HIGH"
    if structured_count > 0 or ref_count > 0:
        return "MEDIUM"
    return "LOW"


def resolve_risk_level(state: dict) -> str:
    risk_result = state.get("risk_result") or {}
    raw_level = str(risk_result.get("riskLevel") or state.get("risk_level") or "MEDIUM").upper()
    if raw_level in {"HIGH", "MEDIUM", "LOW"}:
        return raw_level
    return "MEDIUM"


def resolve_review_result(state: dict) -> str:
    audit_result = state.get("audit_result") or {}
    report_review = audit_result.get("reportReview") if isinstance(audit_result, dict) else {}
    support_status = str((report_review or {}).get("supportStatus") or "").upper()
    source_status = str((state.get("source_context") or {}).get("sourceReviewStatus") or "").upper()
    if source_status == "REJECTED":
        return "REJECTED"
    if support_status in {"REVIEW_SUGGESTED", "READY_FOR_HUMAN_REVIEW"}:
        return "REVIEW_REQUIRED"
    if state.get("need_human_review") is True:
        return "REVIEW_REQUIRED"
    return "APPROVED"


def resolve_configured_sequence(task_type: str | None) -> list[str]:
    fallback_sequence = DEFAULT_WORKFLOW_SEQUENCES.get(task_type) or DEFAULT_WORKFLOW_SEQUENCES[TASK_TYPE_STOCK_RESEARCH]
    workflow = workflow_config_repository.resolve_workflow(task_type)
    configured_sequence = normalize_sequence(workflow.get("nodeSequence"))
    if not configured_sequence:
        return fallback_sequence
    if "report_generation_agent" not in configured_sequence:
        configured_sequence.append("report_generation_agent")
    elif configured_sequence[-1] != "report_generation_agent":
        configured_sequence = [
            agent_code for agent_code in configured_sequence
            if agent_code != "report_generation_agent"
        ] + ["report_generation_agent"]
    return configured_sequence


def normalize_sequence(values) -> list[str]:
    if not isinstance(values, list):
        return []
    result: list[str] = []
    for value in values:
        agent_code = str(value or "").strip()
        if not agent_code or agent_code in result:
            continue
        if agent_code not in DEFAULT_TIMEOUTS:
            continue
        result.append(agent_code)
    return result


def resolve_enabled_chain(default_chain: list[str]) -> list[str]:
    enabled_chain = [
        agent_code
        for agent_code in default_chain
        if should_include_agent(agent_code)
    ]
    if enabled_chain:
        return enabled_chain
    if default_chain:
        return [default_chain[-1]]
    return []


def should_include_agent(agent_code: str) -> bool:
    if agent_code in REQUIRED_AGENT_CODES:
        return True
    return agent_config_repository.is_enabled(agent_code, True)
