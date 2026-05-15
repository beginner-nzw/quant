import json
import unittest

from app.agents.report_generation_agent import ReportGenerationAgent
from app.agents.risk_review_agent import RiskReviewAgent
from app.services.prompt_builder_service import PromptBuilderService


def build_market_context() -> dict:
    return {
        "liveMarketEvents": [
            {
                "eventTitle": "有研复材首次公开发行股票科创板上市公告书",
                "eventSummary": "披露上市公告书",
                "occurredAt": "2026-04-09 09:00:00",
                "impactLevel": "HIGH",
            },
            {
                "eventTitle": "有研复材首次公开发行股票并在科创板上市提示性公告",
                "eventSummary": "披露提示性公告",
                "occurredAt": "2026-04-09 08:30:00",
                "impactLevel": "HIGH",
            },
            {
                "eventTitle": "有研复材公司章程",
                "eventSummary": "披露公司章程",
                "occurredAt": "2026-04-09 08:00:00",
                "impactLevel": "MEDIUM",
            },
            {
                "eventTitle": "有研复材年度报告摘要",
                "eventSummary": "披露年度报告摘要",
                "occurredAt": "2026-04-10 10:00:00",
                "impactLevel": "HIGH",
            },
        ]
    }


def build_policy_risk_market_context() -> dict:
    policy_highlight = "2026-04-20 12:00:00 / 国务院发布支持先进制造业政策 / HIGH"
    risk_highlight = "2026-04-20 13:00:00 / 证监会发布行政处罚决定 / HIGH"
    return {
        "liveMarketEvents": [
            {
                "eventTitle": "国务院发布支持先进制造业政策",
                "eventSummary": "中国政府网政策更新",
                "occurredAt": "2026-04-20 12:00:00",
                "impactLevel": "HIGH",
                "eventType": "POLICY",
                "sourceCode": "POLICY_TRACKER",
                "sourceCategory": "POLICY",
                "sourceChannel": "POLICY_MONITOR",
            },
            {
                "eventTitle": "证监会发布行政处罚决定",
                "eventSummary": "结构化要素：监管类型=行政处罚；处罚/监管对象=贵州茅台；罚没金额=罚款100万元；违规事项=信息披露违规。正文摘要：贵州茅台因信息披露违规被采取行政处罚。",
                "occurredAt": "2026-04-20 13:00:00",
                "impactLevel": "HIGH",
                "eventType": "RISK_ALERT",
                "sourceCode": "RISK_MONITOR",
                "sourceCategory": "RISK",
                "sourceChannel": "RISK_MONITOR",
            },
        ],
        "policyLiveEvents": [
            {
                "eventTitle": "国务院发布支持先进制造业政策",
                "eventSummary": "中国政府网政策更新",
                "occurredAt": "2026-04-20 12:00:00",
                "impactLevel": "HIGH",
            }
        ],
        "policyLiveEventCount": 1,
        "policyLiveEventHighlights": [policy_highlight],
        "regulatoryRiskLiveEvents": [
            {
                "eventTitle": "证监会发布行政处罚决定",
                "eventSummary": "结构化要素：监管类型=行政处罚；处罚/监管对象=贵州茅台；罚没金额=罚款100万元；违规事项=信息披露违规。正文摘要：贵州茅台因信息披露违规被采取行政处罚。",
                "occurredAt": "2026-04-20 13:00:00",
                "impactLevel": "HIGH",
            }
        ],
        "regulatoryRiskLiveEventCount": 1,
        "regulatoryRiskLiveEventHighlights": [risk_highlight],
        "priorityExternalRiskEventSummary": risk_highlight,
    }


def build_cross_day_high_market_context() -> dict:
    return {
        "liveMarketEvents": [
            {
                "eventTitle": "甲公司上市公告书",
                "eventSummary": "披露上市公告书",
                "occurredAt": "2026-04-09 09:00:00",
                "impactLevel": "HIGH",
            },
            {
                "eventTitle": "甲公司提示性公告",
                "eventSummary": "披露提示性公告",
                "occurredAt": "2026-04-10 08:00:00",
                "impactLevel": "HIGH",
            },
        ]
    }


def build_report_state() -> dict:
    return {
        "task_id": "task-688811",
        "task_type": "STOCK_RESEARCH",
        "task_title": "有研复材投研分析",
        "analysis_scope": "REPORT_FOLLOW_UP",
        "target_code": "688811.SH",
        "target_name": "有研复材",
        "priority": "HIGH",
        "task_context": {
            "contextLoaded": True,
            "contextSource": "TASK_CONTEXT",
            "summary": {"stepCount": 5, "agentCount": 4},
            "report": {},
        },
        "source_task_context": {
            "taskDetail": {},
            "report": {},
        },
        "market_context": build_market_context(),
        "financial_result": {
            "summary": "公司当前财务表现稳定。",
        },
        "risk_result": {
            "riskPoints": ["IPO 初期披露深度有限。"],
            "riskWarnings": [],
        },
        "plan_result": {},
        "intent_result": {},
        "source_context": {},
        "evidence_items": [],
    }


def build_non_cluster_report_state() -> dict:
    return {
        "task_id": "task-cross-day-high",
        "task_type": "STOCK_RESEARCH",
        "task_title": "跨日高影响公告分析",
        "analysis_scope": "REPORT_FOLLOW_UP",
        "target_code": "600000.SH",
        "target_name": "甲公司",
        "priority": "HIGH",
        "task_context": {
            "contextLoaded": True,
            "contextSource": "TASK_CONTEXT",
            "summary": {"stepCount": 3, "agentCount": 3},
            "report": {},
        },
        "source_task_context": {
            "taskDetail": {},
            "report": {},
        },
        "market_context": build_cross_day_high_market_context(),
        "financial_result": {
            "summary": "公司经营延续平稳。",
        },
        "risk_result": {
            "riskPoints": ["仍需关注后续披露。"],
            "riskWarnings": [],
        },
        "plan_result": {},
        "intent_result": {},
        "source_context": {},
        "evidence_items": [],
    }


def build_no_event_report_state() -> dict:
    return {
        "task_id": "task-no-live-event",
        "task_type": "STOCK_RESEARCH",
        "task_title": "无实时事件场景分析",
        "analysis_scope": "REPORT_FOLLOW_UP",
        "target_code": "000001.SZ",
        "target_name": "示例公司",
        "priority": "MEDIUM",
        "task_context": {
            "contextLoaded": True,
            "contextSource": "TASK_CONTEXT",
            "summary": {"stepCount": 2, "agentCount": 2},
            "report": {},
        },
        "source_task_context": {
            "taskDetail": {},
            "report": {},
        },
        "market_context": {
            "liveMarketEvents": [],
            "liveEventCount": 0,
            "liveMarketEventSourceCode": "",
            "liveMarketEventSourceName": "",
        },
        "financial_result": {
            "summary": "公司经营延续平稳。",
        },
        "risk_result": {
            "riskPoints": ["仍需持续跟踪。"],
            "riskWarnings": [],
        },
        "plan_result": {},
        "intent_result": {},
        "source_context": {},
        "evidence_items": [],
    }


class _DisabledModelClient:
    def is_enabled(self, scene: str | None = None) -> bool:
        return False


class _DisabledLangChainReportService:
    def is_enabled(self) -> bool:
        return False


class _NoopTaskControlService:
    def check_cancelled(self, task_id: str) -> None:
        return None


class LiveEventPriorityTests(unittest.TestCase):
    def setUp(self) -> None:
        self.prompt_builder = PromptBuilderService.__new__(PromptBuilderService)
        self.prompt_builder_full = PromptBuilderService()
        self.report_agent = ReportGenerationAgent.__new__(ReportGenerationAgent)
        self.risk_agent = RiskReviewAgent.__new__(RiskReviewAgent)

    def test_prompt_builder_cluster_primary_event_prefers_core_listing_file(self) -> None:
        cluster = self.prompt_builder._build_high_impact_live_event_cluster(build_market_context())

        self.assertIsNotNone(cluster)
        self.assertEqual("2026-04-09", cluster["date"])
        self.assertEqual(2, cluster["count"])
        self.assertEqual("有研复材首次公开发行股票科创板上市公告书", cluster["primaryEvent"]["title"])
        self.assertEqual(
            [
                "有研复材首次公开发行股票科创板上市公告书",
                "有研复材首次公开发行股票并在科创板上市提示性公告",
            ],
            cluster["titles"],
        )

    def test_prompt_builder_live_event_briefs_put_cluster_events_first(self) -> None:
        briefs = self.prompt_builder._extract_live_event_briefs(build_market_context(), limit=4)

        self.assertEqual(
            [
                "有研复材首次公开发行股票科创板上市公告书",
                "有研复材首次公开发行股票并在科创板上市提示性公告",
                "有研复材年度报告摘要",
                "有研复材公司章程",
            ],
            [item["title"] for item in briefs],
        )

    def test_report_agent_priority_live_event_aligns_with_cluster_primary_event(self) -> None:
        priority_live_event = self.report_agent._extract_priority_live_event(build_market_context())

        self.assertIsNotNone(priority_live_event)
        self.assertEqual("有研复材首次公开发行股票科创板上市公告书", priority_live_event["title"])
        self.assertEqual("HIGH", priority_live_event["impactLevel"])

    def test_high_impact_cluster_requires_same_date(self) -> None:
        prompt_cluster = self.prompt_builder._build_high_impact_live_event_cluster(
            build_cross_day_high_market_context()
        )
        report_cluster = self.report_agent._build_high_impact_live_event_cluster(
            build_cross_day_high_market_context()
        )

        self.assertIsNone(prompt_cluster)
        self.assertIsNone(report_cluster)

    def test_report_agent_summary_anchor_includes_cluster_and_priority_event(self) -> None:
        summary, changed, anchor_text, anchor_status = self.report_agent._ensure_live_event_summary(
            summary="公司当前财务表现稳定。",
            market_context=build_market_context(),
        )

        self.assertTrue(changed)
        self.assertEqual("POST_PROCESS_ANCHORED", anchor_status)
        self.assertIn("2026-04-09 同步披露 2 份高影响公告", summary)
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", summary)
        self.assertIn("重点实时事件", summary)
        self.assertIn("重点实时事件", anchor_text)

    def test_report_agent_highlights_anchor_puts_cluster_highlight_first(self) -> None:
        highlights, changed, anchor_text, anchor_status = self.report_agent._ensure_live_event_highlights(
            highlights=["财务表现稳定"],
            market_context=build_market_context(),
        )

        self.assertTrue(changed)
        self.assertEqual("POST_PROCESS_ANCHORED", anchor_status)
        self.assertGreaterEqual(len(highlights), 1)
        self.assertIn("同步披露 2 份高影响公告", highlights[0])
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", highlights[0])
        self.assertGreaterEqual(len(highlights), 2)
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", highlights[1])
        self.assertIn("高影响公告", anchor_text)

    def test_report_agent_summary_anchor_keeps_model_native_when_lead_is_covered(self) -> None:
        market_context = build_market_context()
        native_summary = (
            f"{self.report_agent._build_live_event_cluster_summary_anchor(market_context)}"
            f"{self.report_agent._build_live_event_summary_anchor(market_context)}"
            "公司当前财务表现稳定。"
        )

        summary, changed, anchor_text, anchor_status = self.report_agent._ensure_live_event_summary(
            summary=native_summary,
            market_context=market_context,
        )

        self.assertFalse(changed)
        self.assertEqual("MODEL_NATIVE", anchor_status)
        self.assertEqual(native_summary, summary)
        self.assertIn("重点实时事件", anchor_text)

    def test_report_agent_highlights_keep_model_native_when_lead_is_covered(self) -> None:
        market_context = build_market_context()
        native_highlights = [
            self.report_agent._build_live_event_cluster_highlight(market_context),
            self.report_agent._build_primary_live_event_highlight(market_context),
            "财务表现稳定",
        ]

        highlights, changed, anchor_text, anchor_status = self.report_agent._ensure_live_event_highlights(
            highlights=native_highlights,
            market_context=market_context,
        )

        self.assertFalse(changed)
        self.assertEqual("MODEL_NATIVE", anchor_status)
        self.assertEqual(native_highlights, highlights)
        self.assertIn("同步披露 2 份高影响公告", anchor_text)
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", anchor_text)

    def test_report_agent_summary_anchor_accepts_natural_live_event_wording(self) -> None:
        summary = "2026-04-09 同日披露上市公告书与提示性公告，上市公告书是本轮高影响文件，公司当前财务表现稳定。"

        result, changed, anchor_text, anchor_status = self.report_agent._ensure_live_event_summary(
            summary=summary,
            market_context=build_market_context(),
        )

        self.assertFalse(changed)
        self.assertEqual("MODEL_NATIVE", anchor_status)
        self.assertEqual(summary, result)
        self.assertIn("重点实时事件", anchor_text)

    def test_report_agent_highlights_accept_natural_live_event_wording(self) -> None:
        highlights = [
            "实时事件：2026-04-09 同日披露上市公告书与提示性公告",
            "实时事件：上市公告书是本轮高影响文件",
            "财务表现稳定",
        ]

        result, changed, anchor_text, anchor_status = self.report_agent._ensure_live_event_highlights(
            highlights=highlights,
            market_context=build_market_context(),
        )

        self.assertFalse(changed)
        self.assertEqual("MODEL_NATIVE", anchor_status)
        self.assertEqual(highlights, result)
        self.assertIn("同步披露 2 份高影响公告", anchor_text)
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", anchor_text)

    def test_report_agent_summary_anchor_accepts_chinese_date_cluster_wording(self) -> None:
        summary = "4月9日披露两份高影响公告，其中上市公告书更关键，公司当前财务表现稳定。"

        result, changed, anchor_text, anchor_status = self.report_agent._ensure_live_event_summary(
            summary=summary,
            market_context=build_market_context(),
        )

        self.assertFalse(changed)
        self.assertEqual("MODEL_NATIVE", anchor_status)
        self.assertEqual(summary, result)
        self.assertIn("重点实时事件", anchor_text)

    def test_report_agent_highlights_accept_chinese_date_cluster_wording(self) -> None:
        highlights = [
            "实时事件：4月9日披露两份高影响公告",
            "实时事件：上市公告书更关键",
            "财务表现稳定",
        ]

        result, changed, anchor_text, anchor_status = self.report_agent._ensure_live_event_highlights(
            highlights=highlights,
            market_context=build_market_context(),
        )

        self.assertFalse(changed)
        self.assertEqual("MODEL_NATIVE", anchor_status)
        self.assertEqual(highlights, result)
        self.assertIn("同步披露 2 份高影响公告", anchor_text)
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", anchor_text)

    def test_report_agent_primary_highlight_is_not_misclassified_as_cluster(self) -> None:
        market_context = build_market_context()
        primary_highlight = self.report_agent._build_primary_live_event_highlight(market_context)

        self.assertIn("有研复材首次公开发行股票科创板上市公告书", primary_highlight)
        self.assertTrue(self.report_agent._mentions_live_event(primary_highlight, market_context))
        self.assertFalse(self.report_agent._mentions_live_event_cluster(primary_highlight, market_context))

    def test_build_report_prompts_exposes_cluster_priority_consistently(self) -> None:
        system_prompt, user_prompt = self.prompt_builder_full.build_report_prompts(
            state=build_report_state(),
            fallback_report={
                "summary": "回退摘要",
                "highlights": ["回退亮点"],
                "riskPoints": [],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.8,
                "needHumanReview": False,
            },
        )
        payload = json.loads(user_prompt)
        market_context = payload["marketContext"]

        self.assertIn("报告生成 Agent", system_prompt)
        self.assertEqual("HIGH_IMPACT_CLUSTER_FIRST_THEN_IMPACT_DESC_THEN_TITLE_DESC_THEN_TIME_DESC", market_context["liveEventPriorityRule"])
        self.assertEqual("2026-04-09", market_context["highImpactCluster"]["date"])
        self.assertEqual(2, market_context["highImpactCluster"]["count"])
        self.assertEqual(
            "有研复材首次公开发行股票科创板上市公告书",
            market_context["priorityLiveEvent"]["title"],
        )
        self.assertEqual(
            [
                "有研复材首次公开发行股票科创板上市公告书",
                "有研复材首次公开发行股票并在科创板上市提示性公告",
            ],
            market_context["priorityLiveEventTitles"],
        )
        self.assertEqual(
            [
                "有研复材首次公开发行股票科创板上市公告书",
                "有研复材首次公开发行股票并在科创板上市提示性公告",
                "有研复材年度报告摘要",
            ],
            [item["title"] for item in market_context["liveEventBriefs"]],
        )
        self.assertIn("同步披露 2 份高影响公告", market_context["summaryLeadAnchors"][0])
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", market_context["summaryLeadAnchors"][1])
        self.assertIn("同步披露 2 份高影响公告", market_context["highlightLeadAnchors"][0])
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", market_context["highlightLeadAnchors"][1])

    def test_build_report_prompts_without_cluster_keeps_priority_live_event(self) -> None:
        _, user_prompt = self.prompt_builder_full.build_report_prompts(
            state=build_non_cluster_report_state(),
            fallback_report={
                "summary": "回退摘要",
                "highlights": ["回退亮点"],
                "riskPoints": [],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.8,
                "needHumanReview": False,
            },
        )
        payload = json.loads(user_prompt)
        market_context = payload["marketContext"]

        self.assertEqual({}, market_context["highImpactCluster"])
        self.assertEqual("甲公司上市公告书", market_context["priorityLiveEvent"]["title"])
        self.assertEqual(
            [
                "甲公司上市公告书",
                "甲公司提示性公告",
            ],
            [item["title"] for item in market_context["liveEventBriefs"]],
        )
        self.assertEqual(1, len(market_context["summaryLeadAnchors"]))
        self.assertIn("甲公司上市公告书", market_context["summaryLeadAnchors"][0])
        self.assertEqual(1, len(market_context["highlightLeadAnchors"]))
        self.assertIn("甲公司上市公告书", market_context["highlightLeadAnchors"][0])

    def test_build_report_prompts_without_live_events_keeps_event_fields_empty(self) -> None:
        _, user_prompt = self.prompt_builder_full.build_report_prompts(
            state=build_no_event_report_state(),
            fallback_report={
                "summary": "回退摘要",
                "highlights": ["回退亮点"],
                "riskPoints": [],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.8,
                "needHumanReview": False,
            },
        )
        payload = json.loads(user_prompt)
        market_context = payload["marketContext"]

        self.assertEqual([], market_context["liveEventBriefs"])
        self.assertEqual({}, market_context["priorityLiveEvent"])
        self.assertEqual([], market_context["priorityLiveEventTitles"])
        self.assertEqual({}, market_context["highImpactCluster"])

    def test_build_risk_prompts_exposes_policy_and_regulatory_event_briefs(self) -> None:
        state = build_report_state()
        state["market_context"] = build_policy_risk_market_context()

        _, user_prompt = self.prompt_builder_full.build_risk_prompts(
            state=state,
            fallback_result={
                "riskLevel": "MEDIUM",
                "riskPoints": ["基础风险点"],
                "riskWarnings": [],
                "needHumanReview": False,
            },
        )
        payload = json.loads(user_prompt)
        market_context = payload["marketContext"]

        self.assertEqual(1, market_context["policyLiveEventCount"])
        self.assertEqual(1, market_context["regulatoryRiskLiveEventCount"])
        self.assertEqual("国务院发布支持先进制造业政策", market_context["policyLiveEventBriefs"][0]["title"])
        self.assertEqual("证监会发布行政处罚决定", market_context["regulatoryRiskLiveEventBriefs"][0]["title"])
        self.assertIn("处罚/监管对象=贵州茅台", market_context["regulatoryRiskLiveEventBriefs"][0]["summary"])
        self.assertIn("结构化要素", "\n".join(payload["outputRules"]))
        self.assertIn("监管处罚", "\n".join(payload["outputRules"]))

    def test_report_agent_risk_points_include_policy_and_regulatory_context(self) -> None:
        risk_points = self.report_agent._ensure_live_event_risk_points(
            risk_points=[],
            market_context=build_policy_risk_market_context(),
        )

        self.assertTrue(any("监管风险事件显示" in item for item in risk_points))
        self.assertTrue(any("处罚/监管对象=贵州茅台" in item for item in risk_points))
        self.assertTrue(any("政策事件显示" in item for item in risk_points))

    def test_risk_agent_postprocess_promotes_regulatory_event_to_high_risk(self) -> None:
        risk_result = self.risk_agent._ensure_priority_external_risk_context(
            risk_result={
                "riskLevel": "MEDIUM",
                "riskPoints": ["基础风险点"],
                "riskWarnings": [],
                "needHumanReview": False,
            },
            market_context=build_policy_risk_market_context(),
        )

        self.assertEqual("HIGH", risk_result["riskLevel"])
        self.assertTrue(risk_result["needHumanReview"])
        self.assertIn("监管风险待核实", risk_result["riskWarnings"])
        self.assertTrue(any("监管风险事件提示" in item for item in risk_result["riskPoints"]))
        self.assertTrue(any("罚款100万元" in item for item in risk_result["riskPoints"]))
        self.assertTrue(any("政策事件提示" in item for item in risk_result["riskPoints"]))

    def test_invoke_outputs_cluster_first_report_result(self) -> None:
        report_agent = ReportGenerationAgent.__new__(ReportGenerationAgent)
        report_agent.model_client = _DisabledModelClient()
        report_agent.langchain_report_service = _DisabledLangChainReportService()
        report_agent.task_control_service = _NoopTaskControlService()
        report_agent._generate_model_report = lambda state, fallback_report: (
            {
                "summary": "公司当前财务表现稳定。",
                "highlights": ["财务表现稳定"],
                "riskPoints": ["IPO 初期披露深度有限。"],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.86,
                "needHumanReview": False,
            },
            None,
            None,
            "TEST_MODEL",
            None,
        )

        result_state = report_agent.invoke(build_report_state())
        report_result = result_state["report_result"]
        context_snapshot = report_result["contextSnapshot"]

        self.assertIn("2026-04-09 同步披露 2 份高影响公告", report_result["summary"])
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", report_result["summary"])
        self.assertIn("同步披露 2 份高影响公告", report_result["highlights"][0])
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", report_result["highlights"][1])
        self.assertEqual(
            "有研复材首次公开发行股票科创板上市公告书",
            context_snapshot["priorityLiveEventTitle"],
        )
        self.assertEqual("2026-04-09", context_snapshot["highImpactLiveEventClusterDate"])
        self.assertEqual(2, context_snapshot["highImpactLiveEventClusterCount"])
        self.assertTrue(context_snapshot["liveEventSummaryAnchored"])
        self.assertEqual("POST_PROCESS_ANCHORED", context_snapshot["liveEventSummaryAnchorStatus"])
        self.assertTrue(context_snapshot["summaryLeadAnchorsCovered"])
        self.assertEqual("POST_PROCESS_ANCHORED", context_snapshot["summaryLeadCoverageStatus"])
        self.assertIn("同步披露 2 份高影响公告", context_snapshot["summaryLeadAnchors"][0])
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", context_snapshot["summaryLeadAnchors"][1])
        self.assertTrue(context_snapshot["liveEventHighlightAnchored"])
        self.assertEqual("POST_PROCESS_ANCHORED", context_snapshot["liveEventHighlightAnchorStatus"])
        self.assertTrue(context_snapshot["highlightLeadAnchorsCovered"])
        self.assertEqual("POST_PROCESS_ANCHORED", context_snapshot["highlightLeadCoverageStatus"])
        self.assertIn("同步披露 2 份高影响公告", context_snapshot["highlightLeadAnchors"][0])
        self.assertIn("有研复材首次公开发行股票科创板上市公告书", context_snapshot["highlightLeadAnchors"][1])

    def test_invoke_keeps_model_native_live_event_coverage(self) -> None:
        report_agent = ReportGenerationAgent.__new__(ReportGenerationAgent)
        report_agent.model_client = _DisabledModelClient()
        report_agent.langchain_report_service = _DisabledLangChainReportService()
        report_agent.task_control_service = _NoopTaskControlService()
        market_context = build_market_context()
        native_summary = (
            f"{report_agent._build_live_event_cluster_summary_anchor(market_context)}"
            f"{report_agent._build_live_event_summary_anchor(market_context)}"
            "公司当前财务表现稳定。"
        )
        native_highlights = [
            report_agent._build_live_event_cluster_highlight(market_context),
            report_agent._build_primary_live_event_highlight(market_context),
            "财务表现稳定",
        ]
        report_agent._generate_model_report = lambda state, fallback_report: (
            {
                "summary": native_summary,
                "highlights": native_highlights,
                "riskPoints": ["IPO 初期披露深度有限。"],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.9,
                "needHumanReview": False,
            },
            None,
            None,
            "TEST_MODEL",
            None,
        )

        result_state = report_agent.invoke(build_report_state())
        report_result = result_state["report_result"]
        context_snapshot = report_result["contextSnapshot"]

        self.assertEqual(native_summary, report_result["summary"])
        self.assertEqual(native_highlights, report_result["highlights"])
        self.assertFalse(context_snapshot["liveEventSummaryAnchored"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["liveEventSummaryAnchorStatus"])
        self.assertTrue(context_snapshot["summaryLeadAnchorsCovered"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["summaryLeadCoverageStatus"])
        self.assertFalse(context_snapshot["liveEventHighlightAnchored"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["liveEventHighlightAnchorStatus"])
        self.assertTrue(context_snapshot["highlightLeadAnchorsCovered"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["highlightLeadCoverageStatus"])

    def test_invoke_keeps_model_native_for_natural_live_event_wording(self) -> None:
        report_agent = ReportGenerationAgent.__new__(ReportGenerationAgent)
        report_agent.model_client = _DisabledModelClient()
        report_agent.langchain_report_service = _DisabledLangChainReportService()
        report_agent.task_control_service = _NoopTaskControlService()
        natural_summary = "2026-04-09 同日披露上市公告书与提示性公告，上市公告书是本轮高影响文件，公司当前财务表现稳定。"
        natural_highlights = [
            "实时事件：2026-04-09 同日披露上市公告书与提示性公告",
            "实时事件：上市公告书是本轮高影响文件",
            "财务表现稳定",
        ]
        report_agent._generate_model_report = lambda state, fallback_report: (
            {
                "summary": natural_summary,
                "highlights": natural_highlights,
                "riskPoints": ["IPO 初期披露深度有限。"],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.9,
                "needHumanReview": False,
            },
            None,
            None,
            "TEST_MODEL",
            None,
        )

        result_state = report_agent.invoke(build_report_state())
        report_result = result_state["report_result"]
        context_snapshot = report_result["contextSnapshot"]

        self.assertEqual(natural_summary, report_result["summary"])
        self.assertEqual(natural_highlights, report_result["highlights"])
        self.assertFalse(context_snapshot["liveEventSummaryAnchored"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["liveEventSummaryAnchorStatus"])
        self.assertTrue(context_snapshot["summaryLeadAnchorsCovered"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["summaryLeadCoverageStatus"])
        self.assertFalse(context_snapshot["liveEventHighlightAnchored"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["liveEventHighlightAnchorStatus"])
        self.assertTrue(context_snapshot["highlightLeadAnchorsCovered"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["highlightLeadCoverageStatus"])

    def test_invoke_keeps_model_native_for_chinese_date_cluster_wording(self) -> None:
        report_agent = ReportGenerationAgent.__new__(ReportGenerationAgent)
        report_agent.model_client = _DisabledModelClient()
        report_agent.langchain_report_service = _DisabledLangChainReportService()
        report_agent.task_control_service = _NoopTaskControlService()
        natural_summary = "4月9日披露两份高影响公告，其中上市公告书更关键，公司当前财务表现稳定。"
        natural_highlights = [
            "实时事件：4月9日披露两份高影响公告",
            "实时事件：上市公告书更关键",
            "财务表现稳定",
        ]
        report_agent._generate_model_report = lambda state, fallback_report: (
            {
                "summary": natural_summary,
                "highlights": natural_highlights,
                "riskPoints": ["IPO 初期披露深度有限。"],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.9,
                "needHumanReview": False,
            },
            None,
            None,
            "TEST_MODEL",
            None,
        )

        result_state = report_agent.invoke(build_report_state())
        report_result = result_state["report_result"]
        context_snapshot = report_result["contextSnapshot"]

        self.assertEqual(natural_summary, report_result["summary"])
        self.assertEqual(natural_highlights, report_result["highlights"])
        self.assertFalse(context_snapshot["liveEventSummaryAnchored"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["liveEventSummaryAnchorStatus"])
        self.assertTrue(context_snapshot["summaryLeadAnchorsCovered"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["summaryLeadCoverageStatus"])
        self.assertFalse(context_snapshot["liveEventHighlightAnchored"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["liveEventHighlightAnchorStatus"])
        self.assertTrue(context_snapshot["highlightLeadAnchorsCovered"])
        self.assertEqual("MODEL_NATIVE", context_snapshot["highlightLeadCoverageStatus"])

    def test_invoke_outputs_priority_event_without_cluster_phrase(self) -> None:
        report_agent = ReportGenerationAgent.__new__(ReportGenerationAgent)
        report_agent.model_client = _DisabledModelClient()
        report_agent.langchain_report_service = _DisabledLangChainReportService()
        report_agent.task_control_service = _NoopTaskControlService()
        report_agent._generate_model_report = lambda state, fallback_report: (
            {
                "summary": "公司经营延续平稳。",
                "highlights": ["经营延续平稳"],
                "riskPoints": ["仍需关注后续披露。"],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.82,
                "needHumanReview": False,
            },
            None,
            None,
            "TEST_MODEL",
            None,
        )

        result_state = report_agent.invoke(build_non_cluster_report_state())
        report_result = result_state["report_result"]
        context_snapshot = report_result["contextSnapshot"]

        self.assertIn("甲公司上市公告书", report_result["summary"])
        self.assertNotIn("同步披露 2 份高影响公告", report_result["summary"])
        self.assertIn("甲公司上市公告书", report_result["highlights"][0])
        self.assertNotIn("高影响公告", report_result["highlights"][0])
        self.assertEqual("甲公司上市公告书", context_snapshot["priorityLiveEventTitle"])
        self.assertIsNone(context_snapshot["highImpactLiveEventClusterDate"])
        self.assertIsNone(context_snapshot["highImpactLiveEventClusterCount"])
        self.assertTrue(context_snapshot["summaryLeadAnchorsCovered"])
        self.assertEqual("POST_PROCESS_ANCHORED", context_snapshot["summaryLeadCoverageStatus"])
        self.assertEqual(1, len(context_snapshot["summaryLeadAnchors"]))
        self.assertIn("甲公司上市公告书", context_snapshot["summaryLeadAnchors"][0])
        self.assertTrue(context_snapshot["highlightLeadAnchorsCovered"])
        self.assertEqual("POST_PROCESS_ANCHORED", context_snapshot["highlightLeadCoverageStatus"])
        self.assertEqual(1, len(context_snapshot["highlightLeadAnchors"]))
        self.assertIn("甲公司上市公告书", context_snapshot["highlightLeadAnchors"][0])

    def test_invoke_without_live_events_does_not_anchor_summary_or_highlights(self) -> None:
        report_agent = ReportGenerationAgent.__new__(ReportGenerationAgent)
        report_agent.model_client = _DisabledModelClient()
        report_agent.langchain_report_service = _DisabledLangChainReportService()
        report_agent.task_control_service = _NoopTaskControlService()
        report_agent._generate_model_report = lambda state, fallback_report: (
            {
                "summary": "公司经营延续平稳。",
                "highlights": ["经营延续平稳"],
                "riskPoints": ["仍需持续跟踪。"],
                "riskWarnings": [],
                "reviewSuggestion": "",
                "confidenceScore": 0.8,
                "needHumanReview": False,
            },
            None,
            None,
            "TEST_MODEL",
            None,
        )

        result_state = report_agent.invoke(build_no_event_report_state())
        report_result = result_state["report_result"]
        context_snapshot = report_result["contextSnapshot"]

        self.assertEqual("公司经营延续平稳。", report_result["summary"])
        self.assertEqual(["经营延续平稳"], report_result["highlights"])
        self.assertNotIn("实时事件", report_result["summary"])
        self.assertEqual([], context_snapshot["summaryLeadAnchors"])
        self.assertEqual("NOT_APPLICABLE", context_snapshot["summaryLeadCoverageStatus"])
        self.assertEqual("NOT_APPLICABLE", context_snapshot["liveEventSummaryAnchorStatus"])
        self.assertEqual([], context_snapshot["highlightLeadAnchors"])
        self.assertEqual("NOT_APPLICABLE", context_snapshot["highlightLeadCoverageStatus"])
        self.assertEqual("NOT_APPLICABLE", context_snapshot["liveEventHighlightAnchorStatus"])
        self.assertFalse(context_snapshot["liveEventSummaryAnchored"])
        self.assertFalse(context_snapshot["liveEventHighlightAnchored"])


if __name__ == "__main__":
    unittest.main()
