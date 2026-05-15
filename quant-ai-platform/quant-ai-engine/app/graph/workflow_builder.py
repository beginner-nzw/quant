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
from app.graph.node_executor import NodeExecutor
from app.graph.state import WorkflowState
from app.services.agent_config_repository import AgentConfigRepository
from app.services.timeout_executor import TimeoutExecutor
from app.services.workflow_config_repository import WorkflowConfigRepository

timeout_executor = TimeoutExecutor()
agent_config_repository = AgentConfigRepository()
workflow_config_repository = WorkflowConfigRepository()

MODEL_BACKED_NODES = {
    "planner_agent",
    "intent_agent",
    "financial_analysis_agent",
    "risk_review_agent",
    "report_generation_agent",
}
REQUIRED_AGENT_CODES = {
    "report_generation_agent",
}
DEFAULT_TIMEOUTS = {
    "planner_agent": 5,
    "intent_agent": 5,
    "evidence_collection_agent": 5,
    "financial_analysis_agent": 10,
    "risk_review_agent": 10,
    "report_generation_agent": 10,
}
DEFAULT_PROGRESS = {
    "planner_agent": 10,
    "intent_agent": 35,
    "evidence_collection_agent": 55,
    "financial_analysis_agent": 70,
    "risk_review_agent": 82,
    "report_generation_agent": 95,
}
DEFAULT_WORKFLOW_SEQUENCES = {
    TASK_TYPE_STOCK_RESEARCH: [
        "planner_agent",
        "intent_agent",
        "evidence_collection_agent",
        "financial_analysis_agent",
        "risk_review_agent",
        "report_generation_agent",
    ],
    TASK_TYPE_FOLLOW_UP_RESEARCH: [
        "planner_agent",
        "intent_agent",
        "evidence_collection_agent",
        "financial_analysis_agent",
        "report_generation_agent",
    ],
    TASK_TYPE_REPORT_REVIEW: [
        "planner_agent",
        "intent_agent",
        "evidence_collection_agent",
        "risk_review_agent",
        "report_generation_agent",
    ],
    TASK_TYPE_AUDIT_REVIEW: [
        "planner_agent",
        "intent_agent",
        "evidence_collection_agent",
        "risk_review_agent",
        "report_generation_agent",
    ],
    TASK_TYPE_RISK_REVIEW: [
        "planner_agent",
        "evidence_collection_agent",
        "risk_review_agent",
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
    financial_analysis_agent,
    risk_review_agent,
    report_generation_agent,
    producer
):
    available_agents = {
        "planner_agent": planner_agent,
        "intent_agent": intent_agent,
        "evidence_collection_agent": evidence_collection_agent,
        "financial_analysis_agent": financial_analysis_agent,
        "risk_review_agent": risk_review_agent,
        "report_generation_agent": report_generation_agent,
    }

    configured_sequence = resolve_configured_sequence(task_type)
    return build_linear_workflow(
        producer,
        available_agents,
        configured_sequence,
        DEFAULT_TIMEOUTS,
        DEFAULT_PROGRESS,
    )


def build_linear_workflow(
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

    entry_node = enabled_chain[0]
    graph.set_entry_point(entry_node)

    for index in range(len(enabled_chain) - 1):
        graph.add_edge(enabled_chain[index], enabled_chain[index + 1])

    graph.add_edge(enabled_chain[-1], END)
    return graph.compile(), resolve_workflow_timeout_seconds(enabled_chain, fallback_timeouts)


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
