import unittest

from app.agents.financial_analysis_agent import FinancialAnalysisAgent
from app.agents.intent_agent import IntentAgent
from app.agents.planner_agent import PlannerAgent
from app.agents.report_generation_agent import ReportGenerationAgent
from app.agents.risk_review_agent import RiskReviewAgent


class _NoopTaskControlService:
    def check_cancelled(self, task_id: str) -> None:
        return None


class _DisabledLangChainService:
    def __init__(self, reason: str = "LANGCHAIN_DISABLED_FOR_TEST"):
        self.reason = reason

    def is_enabled(self) -> bool:
        return False

    def availability_reason(self) -> str:
        return self.reason


class _DisabledModelClient:
    def is_enabled(self, scene: str | None = None) -> bool:
        return False

    def availability_reason(self, scene: str | None = None) -> str:
        return "MODEL_CONFIG_DISABLED"

    def model_name(self, scene: str | None = None) -> str | None:
        return None


class _EmptyRiskModelClient:
    def is_enabled(self, scene: str | None = None) -> bool:
        return True

    def availability_reason(self, scene: str | None = None) -> str | None:
        return None

    def generate_json_object(self, **kwargs):
        return {"riskPoints": []}

    def model_name(self, scene: str | None = None) -> str:
        return "test-risk-model"


class _PromptBuilder:
    def build_risk_prompts(self, **kwargs):
        return "system", "user"


def _market_context(data_source: str = "fallback") -> dict:
    return {
        "revenueTrend": "STABLE",
        "profitTrend": "STABLE",
        "cashflowSignal": "NORMAL",
        "dataSource": data_source,
        "liveMarketEvents": [],
        "liveEventCount": 0,
    }


class FallbackProvenanceTests(unittest.TestCase):
    def test_planner_and_intent_rule_fallback_keep_reasons(self):
        planner = PlannerAgent.__new__(PlannerAgent)
        planner.langchain_planner_service = _DisabledLangChainService()
        planner.task_control_service = _NoopTaskControlService()

        planner_state = planner.invoke({
            "task_id": "task-plan-fallback",
            "trace_id": "trace-plan-fallback",
            "task_type": "STOCK_RESEARCH",
            "analysis_scope": "DEEP_RESEARCH",
            "task_context": {},
            "source_task_context": {},
            "market_context": {},
        })

        self.assertEqual("RULE_FALLBACK", planner_state["plan_result"]["generationMode"])
        self.assertEqual(
            "LANGCHAIN_DISABLED_FOR_TEST",
            planner_state["plan_result"]["fallbackReason"],
        )

        intent = IntentAgent.__new__(IntentAgent)
        intent.langchain_intent_service = _DisabledLangChainService()
        intent.task_control_service = _NoopTaskControlService()

        intent_state = intent.invoke({
            "task_id": "task-intent-fallback",
            "trace_id": "trace-intent-fallback",
            "task_type": "STOCK_RESEARCH",
            "analysis_scope": "DEEP_RESEARCH",
            "source_context": {},
            "task_context": {},
            "market_context": {},
            "source_task_context": {},
        })

        self.assertEqual("RULE_FALLBACK", intent_state["intent_result"]["generationMode"])
        self.assertEqual(
            "LANGCHAIN_DISABLED_FOR_TEST",
            intent_state["intent_result"]["fallbackReason"],
        )

    def test_financial_rule_fallback_exposes_reason(self):
        agent = FinancialAnalysisAgent.__new__(FinancialAnalysisAgent)
        agent.langchain_financial_service = _DisabledLangChainService()
        agent.model_client = _DisabledModelClient()
        agent.task_control_service = _NoopTaskControlService()

        result_state = agent.invoke({
            "task_id": "task-financial-fallback",
            "trace_id": "trace-financial-fallback",
            "target_code": "600000.SH",
            "target_name": "Test Target",
            "target_type": "STOCK",
            "task_type": "STOCK_RESEARCH",
            "analysis_scope": "DEEP_RESEARCH",
            "market_context": _market_context(),
            "source_task_context": {},
        })

        financial_result = result_state["financial_result"]
        self.assertEqual("RULE_FALLBACK", financial_result["generationMode"])
        self.assertIn("LANGCHAIN_DISABLED_FOR_TEST", financial_result["fallbackReason"])
        self.assertIn("MODEL_CONFIG_DISABLED", financial_result["fallbackReason"])

    def test_risk_rule_fallback_exposes_invalid_model_reason(self):
        agent = RiskReviewAgent.__new__(RiskReviewAgent)
        agent.langchain_risk_service = _DisabledLangChainService()
        agent.model_client = _EmptyRiskModelClient()
        agent.prompt_builder_service = _PromptBuilder()
        agent.task_control_service = _NoopTaskControlService()

        result_state = agent.invoke({
            "task_id": "task-risk-fallback",
            "trace_id": "trace-risk-fallback",
            "task_type": "STOCK_RESEARCH",
            "analysis_scope": "DEEP_RESEARCH",
            "source_context": {},
            "market_context": _market_context(),
            "source_task_context": {},
        })

        risk_result = result_state["risk_result"]
        self.assertEqual("RULE_FALLBACK", risk_result["generationMode"])
        self.assertIn("LANGCHAIN_DISABLED_FOR_TEST", risk_result["fallbackReason"])
        self.assertIn("CUSTOM_HTTP_EMPTY_RISK_POINTS", risk_result["fallbackReason"])

    def test_report_context_snapshot_carries_fallback_provenance(self):
        agent = ReportGenerationAgent.__new__(ReportGenerationAgent)
        agent.langchain_report_service = _DisabledLangChainService()
        agent.model_client = _DisabledModelClient()
        agent.task_control_service = _NoopTaskControlService()

        result_state = agent.invoke({
            "task_id": "task-report-fallback",
            "trace_id": "trace-report-fallback",
            "task_type": "STOCK_RESEARCH",
            "task_title": "Fallback provenance report",
            "analysis_scope": "DEEP_RESEARCH",
            "target_code": "600000.SH",
            "target_name": "Test Target",
            "priority": "MEDIUM",
            "task_context": {
                "contextLoaded": True,
                "contextSource": "TASK_CONTEXT",
                "summary": {"stepCount": 3, "agentCount": 4},
            },
            "source_task_context": {
                "taskDetail": {},
                "report": {},
            },
            "market_context": _market_context(),
            "financial_result": {
                "summary": "Financial rule fallback summary.",
                "generationMode": "RULE_FALLBACK",
                "fallbackReason": "LANGCHAIN_DISABLED_FOR_TEST;MODEL_CONFIG_DISABLED",
            },
            "risk_result": {
                "riskPoints": ["Risk rule fallback point."],
                "riskWarnings": [],
                "needHumanReview": False,
                "generationMode": "RULE_FALLBACK",
                "fallbackReason": "CUSTOM_HTTP_EMPTY_RISK_POINTS",
            },
            "plan_result": {
                "generationMode": "RULE_FALLBACK",
                "fallbackReason": "LANGCHAIN_DISABLED_FOR_TEST",
            },
            "intent_result": {
                "generationMode": "RULE_FALLBACK",
                "fallbackReason": "LANGCHAIN_DISABLED_FOR_TEST",
            },
            "source_context": {},
            "evidence_items": [],
        })

        context_snapshot = result_state["report_result"]["contextSnapshot"]
        self.assertEqual("fallback", context_snapshot["marketDataSource"])
        self.assertEqual(
            "MARKET_DATA_FALLBACK_SNAPSHOT",
            context_snapshot["marketDataFallbackReason"],
        )
        self.assertEqual("RULE_FALLBACK", context_snapshot["financialGenerationMode"])
        self.assertEqual(
            "LANGCHAIN_DISABLED_FOR_TEST;MODEL_CONFIG_DISABLED",
            context_snapshot["financialFallbackReason"],
        )
        self.assertEqual("RULE_FALLBACK", context_snapshot["riskGenerationMode"])
        self.assertEqual(
            "CUSTOM_HTTP_EMPTY_RISK_POINTS",
            context_snapshot["riskFallbackReason"],
        )
        self.assertEqual("RULE_FALLBACK", context_snapshot["generationMode"])
        self.assertEqual("RULE_FALLBACK", context_snapshot["reportGenerationPath"])
        self.assertIn("LANGCHAIN_DISABLED_FOR_TEST", context_snapshot["reportFallbackReason"])


if __name__ == "__main__":
    unittest.main()
