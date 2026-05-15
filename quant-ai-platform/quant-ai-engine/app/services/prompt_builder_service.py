import json
from typing import Any

from app.services.prompt_template_repository import PromptTemplateRepository


class PromptBuilderService:
    PLANNER_PROMPT_VERSION = "planner-v1"
    INTENT_PROMPT_VERSION = "intent-v1"
    REPORT_PROMPT_VERSION = "report-v14"
    FINANCIAL_PROMPT_VERSION = "financial-v3"
    RISK_PROMPT_VERSION = "risk-v5"

    def __init__(self) -> None:
        self.prompt_template_repository = PromptTemplateRepository()

    def build_planner_prompts(
        self,
        *,
        state: dict[str, Any],
        fallback_result: dict[str, Any],
        format_instructions: str | None = None,
    ) -> tuple[str, str]:
        task_context = state.get("task_context") or {}
        source_task_context = state.get("source_task_context") or {}
        market_context = state.get("market_context") or {}
        source_context = state.get("source_context") or {}

        system_prompt = self.prompt_template_repository.load_system_prompt(
            "planner_agent_template",
            (
                "你是企业级金融投研平台中的任务规划 Agent。"
                "请根据当前任务、来源上下文、平台快照和已有任务详情，"
                "输出本轮工作流应采用的规划模式和执行重点。"
                "必须只输出 JSON，不要输出解释或额外文本。"
            ),
        )
        if format_instructions:
            system_prompt = f"{system_prompt}\n\n输出格式要求：\n{format_instructions}"

        prompt_payload = {
            "promptVersion": self.PLANNER_PROMPT_VERSION,
            "task": self._extract_runtime_task(state),
            "sourceContext": source_context,
            "taskContext": {
                "contextLoaded": task_context.get("contextLoaded"),
                "contextSource": task_context.get("contextSource"),
                "summary": task_context.get("summary") or {},
            },
            "sourceTaskContext": {
                "taskDetail": self._extract_task_context(source_task_context.get("taskDetail")),
                "report": self._extract_report_context(source_task_context.get("report")),
            },
            "marketContext": {
                "dataSource": market_context.get("dataSource"),
                "latestInsightSummary": market_context.get("latestInsightSummary"),
                "taskCount": market_context.get("taskCount"),
                "reportCount": market_context.get("reportCount"),
                "pendingReviewCount": market_context.get("pendingReviewCount"),
            },
            "fallbackPlanResult": fallback_result,
            "outputSchema": {
                "planningMode": "enum(CONTEXT_ENRICHED,DISPATCH_ONLY,REVIEW_DRIVEN)",
                "contextReady": "boolean",
                "sourceReportAvailable": "boolean",
                "marketSnapshotReady": "boolean",
                "executionFocus": ["string"],
            },
            "outputRules": [
                "executionFocus 保留 3 到 6 项，使用英文大写下划线风格短语。",
                "如果来源报告和市场快照都可用，优先选择 CONTEXT_ENRICHED。",
                "如果当前任务属于复核类任务，可根据上下文调整为 REVIEW_DRIVEN。",
                "布尔字段必须与当前上下文是否真实可用保持一致。",
            ],
        }

        user_prompt = json.dumps(prompt_payload, ensure_ascii=False, indent=2)
        return system_prompt, user_prompt

    def build_intent_prompts(
        self,
        *,
        state: dict[str, Any],
        fallback_result: dict[str, Any],
        format_instructions: str | None = None,
    ) -> tuple[str, str]:
        task_context = state.get("task_context") or {}
        source_task_context = state.get("source_task_context") or {}
        market_context = state.get("market_context") or {}
        source_context = state.get("source_context") or {}
        plan_result = state.get("plan_result") or {}

        system_prompt = self.prompt_template_repository.load_system_prompt(
            "intent_agent_template",
            (
                "你是企业级金融投研平台中的意图识别 Agent。"
                "请根据任务类型、分析范围、来源上下文、规划结果和平台快照，"
                "输出本轮任务的分析模式、关注维度和审核压力判断。"
                "必须只输出 JSON，不要输出解释或额外文本。"
            ),
        )
        if format_instructions:
            system_prompt = f"{system_prompt}\n\n输出格式要求：\n{format_instructions}"

        prompt_payload = {
            "promptVersion": self.INTENT_PROMPT_VERSION,
            "task": self._extract_runtime_task(state),
            "sourceContext": source_context,
            "planResult": plan_result,
            "taskContext": {
                "contextLoaded": task_context.get("contextLoaded"),
                "contextSource": task_context.get("contextSource"),
                "summary": task_context.get("summary") or {},
            },
            "sourceTaskContext": {
                "taskDetail": self._extract_task_context(source_task_context.get("taskDetail")),
                "report": self._extract_report_context(source_task_context.get("report")),
            },
            "marketContext": {
                "dataSource": market_context.get("dataSource"),
                "latestInsightSummary": market_context.get("latestInsightSummary"),
                "latestInsightReportId": market_context.get("latestInsightReportId"),
                "pendingReviewCount": market_context.get("pendingReviewCount"),
            },
            "fallbackIntentResult": fallback_result,
            "outputSchema": {
                "analysisMode": "string",
                "focusDimensions": ["string"],
                "sourceDomain": "string",
                "contextSource": "string|null",
                "sourceReportId": "string|null",
                "latestInsightReportId": "string|null",
                "pendingReviewCount": "integer",
                "reviewPressure": "enum(LOW,MEDIUM,HIGH)",
            },
            "outputRules": [
                "focusDimensions 保留 3 到 6 项，使用英文大写单词。",
                "如果来源报告存在，应优先体现 SOURCE_REPORT 维度。",
                "如果平台快照存在最新洞察，应优先体现 PLATFORM_INSIGHT 维度。",
                "reviewPressure 必须与 pendingReviewCount 保持一致。",
            ],
        }

        user_prompt = json.dumps(prompt_payload, ensure_ascii=False, indent=2)
        return system_prompt, user_prompt

    def build_report_prompts(
        self,
        *,
        state: dict[str, Any],
        fallback_report: dict[str, Any],
        format_instructions: str | None = None,
    ) -> tuple[str, str]:
        task_context = state.get("task_context") or {}
        source_task_context = state.get("source_task_context") or {}
        market_context = state.get("market_context") or {}
        financial_result = state.get("financial_result") or {}
        risk_result = state.get("risk_result") or {}
        plan_result = state.get("plan_result") or {}
        intent_result = state.get("intent_result") or {}
        source_context = state.get("source_context") or {}
        live_event_briefs = self._extract_live_event_briefs(market_context)
        latest_live_event = self._extract_latest_live_event(market_context) or {}
        live_event_stats = self._build_live_event_stats(market_context)
        high_impact_cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        priority_live_event = self._extract_priority_live_event(market_context) or {}
        priority_live_event_titles = self._extract_priority_live_event_titles(market_context)
        summary_lead_anchors = self._build_live_event_summary_lead_anchors(market_context)
        highlight_lead_anchors = self._build_live_event_highlight_lead_anchors(market_context)
        policy_live_event_briefs = self._extract_policy_live_event_briefs(market_context)
        regulatory_risk_live_event_briefs = self._extract_regulatory_risk_live_event_briefs(market_context)

        system_prompt = self.prompt_template_repository.load_system_prompt(
            "report_generation_agent_template",
            (
                "你是企业级金融投研平台中的报告生成 Agent。"
                "请基于任务上下文、来源报告、平台快照、财务分析结果和风险审查结果，"
                "生成一份可直接入库的结构化研究报告结果。"
                "必须只输出 JSON，不要输出 Markdown、解释或额外文本。"
            ),
        )
        if format_instructions:
            system_prompt = f"{system_prompt}\n\n输出格式要求：\n{format_instructions}"

        prompt_payload = {
            "promptVersion": self.REPORT_PROMPT_VERSION,
            "task": {
                "taskId": state.get("task_id"),
                "taskType": state.get("task_type"),
                "taskTitle": state.get("task_title"),
                "analysisScope": state.get("analysis_scope"),
                "targetCode": state.get("target_code"),
                "targetName": state.get("target_name"),
                "priority": state.get("priority"),
            },
            "sourceContext": source_context,
            "taskContext": {
                "contextLoaded": task_context.get("contextLoaded"),
                "contextSource": task_context.get("contextSource"),
                "summary": task_context.get("summary") or {},
                "report": self._extract_report_context(task_context.get("report")),
            },
            "sourceTaskContext": {
                "taskDetail": self._extract_task_context(source_task_context.get("taskDetail")),
                "report": self._extract_report_context(source_task_context.get("report")),
            },
            "marketContext": {
                "dataSource": market_context.get("dataSource"),
                "latestInsightSummary": market_context.get("latestInsightSummary"),
                "latestHighlights": market_context.get("latestHighlights") or [],
                "latestRiskPoints": market_context.get("latestRiskPoints") or [],
                "riskWarnings": market_context.get("riskWarnings") or [],
                "riskWarningCount": market_context.get("riskWarningCount"),
                "latestRiskWarningSummary": market_context.get("latestRiskWarningSummary"),
                "strategySignals": market_context.get("strategySignals") or [],
                "strategySignalCount": market_context.get("strategySignalCount"),
                "latestStrategySignalSummary": market_context.get("latestStrategySignalSummary"),
                "marketIntelligence": market_context.get("marketIntelligence") or [],
                "marketIntelligenceCount": market_context.get("marketIntelligenceCount"),
                "latestMarketIntelligenceSummary": market_context.get("latestMarketIntelligenceSummary"),
                "taskCount": market_context.get("taskCount"),
                "reportCount": market_context.get("reportCount"),
                "pendingReviewCount": market_context.get("pendingReviewCount"),
                "latestInsightReportId": market_context.get("latestInsightReportId"),
                "liveMarketEventSourceCode": market_context.get("liveMarketEventSourceCode"),
                "liveMarketEventSourceName": market_context.get("liveMarketEventSourceName"),
                "liveMarketEventSourceCodes": market_context.get("liveMarketEventSourceCodes") or [],
                "liveMarketEventSourceNames": market_context.get("liveMarketEventSourceNames") or [],
                "liveMarketEventSources": market_context.get("liveMarketEventSources") or [],
                "liveEventCount": market_context.get("liveEventCount"),
                "liveMarketEvents": market_context.get("liveMarketEvents") or [],
                "liveEventBriefs": live_event_briefs,
                "priorityLiveEvent": priority_live_event,
                "priorityLiveEventTitles": priority_live_event_titles,
                "latestLiveEvent": latest_live_event,
                "liveEventStats": live_event_stats,
                "highImpactCluster": high_impact_cluster,
                "liveEventHighlights": self._build_live_event_highlights(live_event_briefs),
                "policyLiveEvents": market_context.get("policyLiveEvents") or [],
                "policyLiveEventCount": market_context.get("policyLiveEventCount"),
                "policyLiveEventHighlights": market_context.get("policyLiveEventHighlights") or [],
                "policyLiveEventBriefs": policy_live_event_briefs,
                "regulatoryRiskLiveEvents": market_context.get("regulatoryRiskLiveEvents") or [],
                "regulatoryRiskLiveEventCount": market_context.get("regulatoryRiskLiveEventCount"),
                "regulatoryRiskLiveEventHighlights": market_context.get("regulatoryRiskLiveEventHighlights") or [],
                "regulatoryRiskLiveEventBriefs": regulatory_risk_live_event_briefs,
                "priorityExternalRiskEventSummary": market_context.get("priorityExternalRiskEventSummary"),
                "summaryLeadAnchors": summary_lead_anchors,
                "highlightLeadAnchors": highlight_lead_anchors,
                "summaryMustMentionPriorityLiveEvent": bool(priority_live_event),
                "highlightsMustIncludePriorityLiveEvent": bool(priority_live_event),
                "liveEventPriorityRule": "HIGH_IMPACT_CLUSTER_FIRST_THEN_IMPACT_DESC_THEN_TITLE_DESC_THEN_TIME_DESC",
                "reportingPriority": "PRIORITY_LIVE_EVENT_FIRST" if priority_live_event else "DEFAULT",
            },
            "evidenceItems": self._extract_evidence_items(state.get("evidence_items")),
            "planResult": plan_result,
            "intentResult": intent_result,
            "financialResult": financial_result,
            "riskResult": risk_result,
            "fallbackReport": fallback_report,
            "outputSchema": {
                "summary": "string",
                "highlights": ["string"],
                "riskPoints": ["string"],
                "riskWarnings": ["string"],
                "reviewSuggestion": "string",
                "confidenceScore": "number(0-1)",
                "needHumanReview": "boolean",
            },
            "outputRules": [
                "summary 必须是自然中文，长度控制在 120 到 220 字。",
                "highlights 保留 3 到 6 条，聚焦平台上下文引用、关键结论和变化点。",
                "riskPoints 保留 0 到 5 条，必须与财务或平台上下文一致。",
                "riskWarnings 只保留需要重点关注的预警项。",
                "confidenceScore 取值在 0 到 1 之间。",
                "needHumanReview 为 true 时，reviewSuggestion 必须明确说明还需补充什么证据。",
                "如存在 riskWarnings、strategySignals 或 marketIntelligence，应在 summary、highlights 或 riskPoints 中吸收这些外部信号，不能只围绕历史工作台摘要写结论。",
                "如存在 priorityLiveEvent，summary 必须显式提到该事件的标题、时间或影响等级，不能只笼统写‘已结合实时事件’。",
                "如存在 priorityLiveEvent，highlights 至少 1 条必须直接描述该实时事件。",
                "如存在 priorityLiveEvent 或 highImpactCluster，riskPoints 至少 1 条应体现后续披露、经营细节或盈利兑现仍待验证的风险。",
                "如存在 regulatoryRiskLiveEventBriefs，必须优先读取其 summary 中的结构化要素和正文摘要，并在 summary、highlights 或 riskPoints 至少 1 处明确吸收监管处罚、监管措施或监管风险背景。",
                "如存在 policyLiveEventBriefs，summary 或 highlights 至少 1 处应吸收政策背景，不得完全忽略政策侧实时事件。",
                "如 needHumanReview 为 true 且存在 priorityLiveEvent，reviewSuggestion 应明确补充哪类官方公告、证据链或后续定期报告。",
                "如存在 summaryLeadAnchors，summary 前半段应按数组顺序优先吸收这些锚点事实，不得整体后置到摘要结尾。",
                "如存在 highlightLeadAnchors，highlights 前 2 条应按数组顺序优先覆盖这些锚点事实，可自然改写但不得丢失 priorityLiveEvent 的标题、时间或影响等级。",
                "如存在 highImpactCluster，highlights 第 1 条优先描述该同日高影响公告簇，第 2 条再落到最重要的 priorityLiveEvent。",
                "只出现实时事件来源名称、事件条数或‘已结合实时事件’不算合格引用，必须落到 priorityLiveEvent 的具体标题、发生时间或影响等级。",
                "如果存在 highImpactCluster，summary 和 highlights 应优先体现该同日高影响公告簇，而不是只提一条普通公告。",
                "不要仅凭公告标题外推公司进入某个经营或治理阶段；证据不足时，使用‘发布/披露/同步了某公告’这类客观表述。",
            ],
        }

        user_prompt = json.dumps(prompt_payload, ensure_ascii=False, indent=2)
        return system_prompt, user_prompt

    def build_financial_prompts(
        self,
        *,
        state: dict[str, Any],
        fallback_result: dict[str, Any],
        format_instructions: str | None = None,
    ) -> tuple[str, str]:
        task_context = state.get("task_context") or {}
        source_task_context = state.get("source_task_context") or {}
        market_context = state.get("market_context") or {}
        source_context = state.get("source_context") or {}
        live_event_briefs = self._extract_live_event_briefs(market_context)
        policy_live_event_briefs = self._extract_policy_live_event_briefs(market_context)
        regulatory_risk_live_event_briefs = self._extract_regulatory_risk_live_event_briefs(market_context)

        system_prompt = self.prompt_template_repository.load_system_prompt(
            "financial_analysis_agent_template",
            (
                "你是企业级金融投研平台中的财务分析 Agent。"
                "请基于任务上下文、来源报告和平台快照，输出结构化财务分析结论。"
                "必须只输出 JSON，不要输出解释文本。"
            ),
        )
        if format_instructions:
            system_prompt = f"{system_prompt}\n\n输出格式要求：\n{format_instructions}"

        prompt_payload = {
            "promptVersion": self.FINANCIAL_PROMPT_VERSION,
            "task": self._extract_runtime_task(state),
            "sourceContext": source_context,
            "taskContext": {
                "contextLoaded": task_context.get("contextLoaded"),
                "contextSource": task_context.get("contextSource"),
                "summary": task_context.get("summary") or {},
            },
            "sourceTaskContext": {
                "taskDetail": self._extract_task_context(source_task_context.get("taskDetail")),
                "report": self._extract_report_context(source_task_context.get("report")),
            },
            "marketContext": {
                "dataSource": market_context.get("dataSource"),
                "revenueTrend": market_context.get("revenueTrend"),
                "profitTrend": market_context.get("profitTrend"),
                "cashflowSignal": market_context.get("cashflowSignal"),
                "latestInsightSummary": market_context.get("latestInsightSummary"),
                "latestHighlights": market_context.get("latestHighlights") or [],
                "latestRiskPoints": market_context.get("latestRiskPoints") or [],
                "riskWarnings": market_context.get("riskWarnings") or [],
                "riskWarningCount": market_context.get("riskWarningCount"),
                "latestRiskWarningSummary": market_context.get("latestRiskWarningSummary"),
                "strategySignals": market_context.get("strategySignals") or [],
                "strategySignalCount": market_context.get("strategySignalCount"),
                "latestStrategySignalSummary": market_context.get("latestStrategySignalSummary"),
                "marketIntelligence": market_context.get("marketIntelligence") or [],
                "marketIntelligenceCount": market_context.get("marketIntelligenceCount"),
                "latestMarketIntelligenceSummary": market_context.get("latestMarketIntelligenceSummary"),
                "taskCount": market_context.get("taskCount"),
                "reportCount": market_context.get("reportCount"),
                "pendingReviewCount": market_context.get("pendingReviewCount"),
                "liveMarketEventSourceCode": market_context.get("liveMarketEventSourceCode"),
                "liveMarketEventSourceName": market_context.get("liveMarketEventSourceName"),
                "liveMarketEventSourceCodes": market_context.get("liveMarketEventSourceCodes") or [],
                "liveMarketEventSourceNames": market_context.get("liveMarketEventSourceNames") or [],
                "liveMarketEventSources": market_context.get("liveMarketEventSources") or [],
                "liveEventCount": market_context.get("liveEventCount"),
                "liveMarketEvents": market_context.get("liveMarketEvents") or [],
                "liveEventBriefs": live_event_briefs,
                "policyLiveEventBriefs": policy_live_event_briefs,
                "regulatoryRiskLiveEventBriefs": regulatory_risk_live_event_briefs,
            },
            "evidenceItems": self._extract_evidence_items(state.get("evidence_items")),
            "fallbackFinancialResult": fallback_result,
            "outputSchema": {
                "summary": "string",
                "revenueTrend": "enum(UP,DOWN,STABLE,PRESSURED)",
                "profitTrend": "enum(UP,DOWN,STABLE)",
                "cashflowSignal": "enum(NORMAL,WATCH,PRESSURED)",
            },
            "outputRules": [
                "summary 必须是自然中文，长度控制在 80 到 180 字。",
                "优先解释当前财务趋势、平台洞察和来源报告之间的关系。",
                "如存在 strategySignals、marketIntelligence 或 riskWarnings，应综合这些外部信号解释财务趋势，不能只重复工作台摘要。",
                "revenueTrend、profitTrend、cashflowSignal 必须严格使用枚举值。",
            ],
        }

        user_prompt = json.dumps(prompt_payload, ensure_ascii=False, indent=2)
        return system_prompt, user_prompt

    def build_risk_prompts(
        self,
        *,
        state: dict[str, Any],
        fallback_result: dict[str, Any],
        format_instructions: str | None = None,
    ) -> tuple[str, str]:
        source_task_context = state.get("source_task_context") or {}
        market_context = state.get("market_context") or {}
        source_context = state.get("source_context") or {}
        financial_result = state.get("financial_result") or {}
        live_event_briefs = self._extract_live_event_briefs(market_context)
        policy_live_event_briefs = self._extract_policy_live_event_briefs(market_context)
        regulatory_risk_live_event_briefs = self._extract_regulatory_risk_live_event_briefs(market_context)

        system_prompt = self.prompt_template_repository.load_system_prompt(
            "risk_review_agent_template",
            (
                "你是企业级金融投研平台中的风险复核 Agent。"
                "请基于任务类型、来源报告、平台快照和财务分析结果，输出结构化风险判断。"
                "必须只输出 JSON，不要输出解释文本。"
            ),
        )
        if format_instructions:
            system_prompt = f"{system_prompt}\n\n输出格式要求：\n{format_instructions}"

        prompt_payload = {
            "promptVersion": self.RISK_PROMPT_VERSION,
            "task": self._extract_runtime_task(state),
            "sourceContext": source_context,
            "sourceTaskContext": {
                "taskDetail": self._extract_task_context(source_task_context.get("taskDetail")),
                "report": self._extract_report_context(source_task_context.get("report")),
            },
            "marketContext": {
                "dataSource": market_context.get("dataSource"),
                "latestInsightSummary": market_context.get("latestInsightSummary"),
                "latestRiskPoints": market_context.get("latestRiskPoints") or [],
                "riskWarnings": market_context.get("riskWarnings") or [],
                "riskWarningCount": market_context.get("riskWarningCount"),
                "latestRiskWarningSummary": market_context.get("latestRiskWarningSummary"),
                "strategySignals": market_context.get("strategySignals") or [],
                "strategySignalCount": market_context.get("strategySignalCount"),
                "latestStrategySignalSummary": market_context.get("latestStrategySignalSummary"),
                "marketIntelligence": market_context.get("marketIntelligence") or [],
                "marketIntelligenceCount": market_context.get("marketIntelligenceCount"),
                "latestMarketIntelligenceSummary": market_context.get("latestMarketIntelligenceSummary"),
                "failedTaskCount": market_context.get("failedTaskCount"),
                "pendingReviewCount": market_context.get("pendingReviewCount"),
                "latestInsightReviewStatus": market_context.get("latestInsightReviewStatus"),
                "liveMarketEventSourceCode": market_context.get("liveMarketEventSourceCode"),
                "liveMarketEventSourceName": market_context.get("liveMarketEventSourceName"),
                "liveMarketEventSourceCodes": market_context.get("liveMarketEventSourceCodes") or [],
                "liveMarketEventSourceNames": market_context.get("liveMarketEventSourceNames") or [],
                "liveMarketEventSources": market_context.get("liveMarketEventSources") or [],
                "liveEventCount": market_context.get("liveEventCount"),
                "liveMarketEvents": market_context.get("liveMarketEvents") or [],
                "liveEventBriefs": live_event_briefs,
                "policyLiveEvents": market_context.get("policyLiveEvents") or [],
                "policyLiveEventCount": market_context.get("policyLiveEventCount"),
                "policyLiveEventHighlights": market_context.get("policyLiveEventHighlights") or [],
                "policyLiveEventBriefs": policy_live_event_briefs,
                "regulatoryRiskLiveEvents": market_context.get("regulatoryRiskLiveEvents") or [],
                "regulatoryRiskLiveEventCount": market_context.get("regulatoryRiskLiveEventCount"),
                "regulatoryRiskLiveEventHighlights": market_context.get("regulatoryRiskLiveEventHighlights") or [],
                "regulatoryRiskLiveEventBriefs": regulatory_risk_live_event_briefs,
                "priorityExternalRiskEventSummary": market_context.get("priorityExternalRiskEventSummary"),
            },
            "financialResult": {
                "summary": financial_result.get("summary"),
                "revenueTrend": financial_result.get("revenueTrend"),
                "profitTrend": financial_result.get("profitTrend"),
                "cashflowSignal": financial_result.get("cashflowSignal"),
            },
            "evidenceItems": self._extract_evidence_items(state.get("evidence_items")),
            "fallbackRiskResult": fallback_result,
            "outputSchema": {
                "riskLevel": "enum(HIGH,MEDIUM,LOW)",
                "riskPoints": ["string"],
                "riskWarnings": ["string"],
                "needHumanReview": "boolean",
            },
            "outputRules": [
                "riskPoints 保留 1 到 5 条，必须是可解释的风险结论。",
                "riskWarnings 只保留需要立即关注的预警。",
                "如存在 riskWarnings、marketIntelligence、strategySignals 或 liveMarketEvents，应优先把这些外部风险与情报信号纳入风险判断。",
                "如存在 regulatoryRiskLiveEventBriefs，应优先读取其 summary 中的结构化要素和正文摘要，把监管处罚、监管措施、处罚对象、罚没金额或违规事项写入 riskPoints 或 riskWarnings。",
                "如存在 policyLiveEventBriefs，应至少 1 条风险点体现政策背景或政策执行不确定性。",
                "riskLevel 必须严格使用 HIGH、MEDIUM 或 LOW。",
                "needHumanReview 为 true 时，风险点或预警中必须能看出人工复核原因。",
            ],
        }

        user_prompt = json.dumps(prompt_payload, ensure_ascii=False, indent=2)
        return system_prompt, user_prompt

    def _extract_report_context(self, report: Any) -> dict[str, Any]:
        if not isinstance(report, dict):
            return {}
        return {
            "reportId": report.get("reportId"),
            "reportType": report.get("reportType"),
            "summary": report.get("displaySummary") or report.get("summary") or report.get("originalSummary"),
            "reviewStatus": report.get("reviewStatus"),
            "highlights": report.get("displayHighlights") or report.get("originalHighlights") or [],
            "riskPoints": report.get("displayRiskPoints") or report.get("originalRiskPoints") or [],
        }

    def _extract_task_context(self, task_detail: Any) -> dict[str, Any]:
        if not isinstance(task_detail, dict):
            return {}
        return {
            "taskId": task_detail.get("taskId"),
            "taskType": task_detail.get("taskType"),
            "taskTitle": task_detail.get("taskTitle"),
            "targetCode": task_detail.get("targetCode"),
            "targetName": task_detail.get("targetName"),
            "priority": task_detail.get("priority"),
            "status": task_detail.get("status"),
        }

    def _extract_runtime_task(self, state: dict[str, Any]) -> dict[str, Any]:
        return {
            "taskId": state.get("task_id"),
            "taskType": state.get("task_type"),
            "taskTitle": state.get("task_title"),
            "analysisScope": state.get("analysis_scope"),
            "targetCode": state.get("target_code"),
            "targetName": state.get("target_name"),
            "priority": state.get("priority"),
        }

    def _extract_evidence_items(self, value: Any) -> list[dict[str, Any]]:
        if not isinstance(value, list):
            return []
        items: list[dict[str, Any]] = []
        for item in value:
            if not isinstance(item, dict):
                continue
            evidence = {
                "evidenceId": item.get("evidenceId"),
                "evidenceType": item.get("evidenceType"),
                "source": item.get("source"),
                "title": item.get("title"),
                "summary": item.get("summary"),
                "url": item.get("url"),
                "occurredAt": item.get("occurredAt"),
                "referenceId": item.get("referenceId"),
                "relevance": item.get("relevance"),
            }
            if evidence["evidenceId"] or evidence["title"] or evidence["summary"]:
                items.append(evidence)
        return items

    def _extract_live_event_briefs(self, market_context: dict[str, Any], limit: int = 3) -> list[dict[str, Any]]:
        live_events = market_context.get("liveMarketEvents") or []
        if not isinstance(live_events, list):
            return []

        cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        cluster_date = self._normalize_text(cluster.get("date"))
        candidates: list[dict[str, Any]] = []
        for index, item in enumerate(live_events):
            if not isinstance(item, dict):
                continue
            brief = self._to_live_event_brief(item)
            if not brief:
                continue
            brief["_originalIndex"] = index
            candidates.append(brief)
        candidates.sort(
            key=lambda item: (
                -self._live_event_cluster_rank(item, cluster_date),
                -self._live_event_priority_rank(item.get("impactLevel")),
                -self._live_event_title_rank(item.get("title")),
                -self._live_event_time_rank(item.get("occurredAt")),
                item.get("_originalIndex", 0),
            )
        )

        briefs: list[dict[str, Any]] = []
        for item in candidates[:limit]:
            briefs.append({
                "title": item.get("title"),
                "summary": item.get("summary"),
                "occurredAt": item.get("occurredAt"),
                "impactLevel": item.get("impactLevel"),
                "sourceUrl": item.get("sourceUrl"),
            })
        return briefs

    def _extract_latest_live_event(self, market_context: dict[str, Any]) -> dict[str, Any] | None:
        live_events = market_context.get("liveMarketEvents") or []
        if not isinstance(live_events, list):
            return None

        latest_event: dict[str, Any] | None = None
        latest_time_rank = -1
        for item in live_events:
            if not isinstance(item, dict):
                continue
            title = self._normalize_text(item.get("eventTitle"))
            summary = self._normalize_text(item.get("eventSummary")) or title
            occurred_at = self._normalize_text(item.get("occurredAt"))
            impact_level = self._normalize_text(item.get("impactLevel"))
            source_url = self._normalize_text(item.get("sourceUrl"))
            if not title and not summary:
                continue
            time_rank = self._live_event_time_rank(occurred_at)
            if time_rank >= latest_time_rank:
                latest_time_rank = time_rank
                latest_event = {
                    "title": title,
                    "summary": summary,
                    "occurredAt": occurred_at,
                    "impactLevel": impact_level,
                    "sourceUrl": source_url,
                }
        return latest_event

    def _extract_policy_live_event_briefs(
        self,
        market_context: dict[str, Any],
        limit: int = 2,
    ) -> list[dict[str, Any]]:
        return self._extract_special_live_event_briefs(
            market_context.get("policyLiveEvents"),
            fallback_live_events=market_context.get("liveMarketEvents"),
            predicate=self._is_policy_live_event,
            limit=limit,
        )

    def _extract_regulatory_risk_live_event_briefs(
        self,
        market_context: dict[str, Any],
        limit: int = 2,
    ) -> list[dict[str, Any]]:
        return self._extract_special_live_event_briefs(
            market_context.get("regulatoryRiskLiveEvents"),
            fallback_live_events=market_context.get("liveMarketEvents"),
            predicate=self._is_regulatory_risk_live_event,
            limit=limit,
        )

    def _extract_special_live_event_briefs(
        self,
        live_events: Any,
        *,
        fallback_live_events: Any,
        predicate,
        limit: int,
    ) -> list[dict[str, Any]]:
        using_preclassified_items = isinstance(live_events, list)
        source_items = live_events if using_preclassified_items else fallback_live_events
        if not isinstance(source_items, list):
            return []
        filtered: list[dict[str, Any]] = []
        for item in source_items:
            if not isinstance(item, dict):
                continue
            if not using_preclassified_items and not predicate(item):
                continue
            brief = self._to_live_event_brief(item)
            if brief:
                filtered.append(brief)
        filtered.sort(
            key=lambda item: (
                -self._live_event_priority_rank(item.get("impactLevel")),
                -self._live_event_time_rank(item.get("occurredAt")),
                -self._live_event_title_rank(item.get("title")),
            )
        )
        return filtered[:limit]

    def _to_live_event_brief(self, item: dict[str, Any]) -> dict[str, Any] | None:
        title = self._normalize_text(item.get("eventTitle") or item.get("title"))
        summary = self._normalize_text(item.get("eventSummary") or item.get("summary")) or title
        occurred_at = self._normalize_text(item.get("occurredAt"))
        impact_level = self._normalize_text(item.get("impactLevel"))
        source_url = self._normalize_text(item.get("sourceUrl"))
        if not title and not summary:
            return None
        return {
            "title": title,
            "summary": summary,
            "occurredAt": occurred_at,
            "impactLevel": impact_level,
            "sourceUrl": source_url,
            "eventType": self._normalize_text(item.get("eventType")),
            "sourceCode": self._normalize_text(item.get("sourceCode")),
            "sourceName": self._normalize_text(item.get("sourceName")),
            "sourceCategory": self._normalize_text(item.get("sourceCategory")),
            "sourceChannel": self._normalize_text(item.get("sourceChannel")),
        }

    def _is_policy_live_event(self, item: dict[str, Any]) -> bool:
        source_code = self._normalize_text(item.get("sourceCode")).upper()
        source_category = self._normalize_text(item.get("sourceCategory")).upper()
        source_channel = self._normalize_text(item.get("sourceChannel")).upper()
        event_type = self._normalize_text(item.get("eventType")).upper()
        combined_text = (
            f"{self._normalize_text(item.get('eventTitle') or item.get('title'))} "
            f"{self._normalize_text(item.get('eventSummary') or item.get('summary'))}"
        )
        return (
            source_code == "POLICY_TRACKER"
            or source_category == "POLICY"
            or source_channel == "POLICY_MONITOR"
            or event_type == "POLICY"
            or "政策" in combined_text
            or "国务院" in combined_text
            or "中国政府网" in combined_text
        )

    def _is_regulatory_risk_live_event(self, item: dict[str, Any]) -> bool:
        source_code = self._normalize_text(item.get("sourceCode")).upper()
        source_category = self._normalize_text(item.get("sourceCategory")).upper()
        source_channel = self._normalize_text(item.get("sourceChannel")).upper()
        event_type = self._normalize_text(item.get("eventType")).upper()
        combined_text = (
            f"{self._normalize_text(item.get('eventTitle') or item.get('title'))} "
            f"{self._normalize_text(item.get('eventSummary') or item.get('summary'))}"
        )
        return (
            source_code == "RISK_MONITOR"
            or source_category == "RISK"
            or source_channel == "RISK_MONITOR"
            or event_type == "RISK_ALERT"
            or "证监会" in combined_text
            or "行政处罚" in combined_text
            or "市场禁入" in combined_text
            or "监管措施" in combined_text
            or "监管风险" in combined_text
        )

    def _build_live_event_highlights(self, live_event_briefs: list[dict[str, Any]]) -> list[str]:
        highlights: list[str] = []
        for item in live_event_briefs:
            title = self._normalize_text(item.get("title"))
            summary = self._normalize_text(item.get("summary"))
            occurred_at = self._normalize_text(item.get("occurredAt"))
            impact_level = self._normalize_text(item.get("impactLevel"))
            if not title and not summary:
                continue
            parts = [part for part in (occurred_at, title or summary, impact_level) if part]
            highlights.append(" / ".join(parts))
        return highlights

    def _build_live_event_summary_lead_anchors(
        self,
        market_context: dict[str, Any],
    ) -> list[str]:
        return self._dedupe_text_list([
            self._build_live_event_cluster_summary_anchor(market_context),
            self._build_live_event_summary_anchor(market_context),
        ])

    def _build_live_event_highlight_lead_anchors(
        self,
        market_context: dict[str, Any],
    ) -> list[str]:
        return self._dedupe_text_list([
            self._build_live_event_cluster_highlight(market_context),
            self._build_primary_live_event_highlight(market_context),
        ])

    def _build_live_event_summary_anchor(self, market_context: dict[str, Any]) -> str:
        priority_live_event = self._extract_priority_live_event(market_context)
        priority_live_event_highlight = self._format_live_event_brief(priority_live_event)
        if not priority_live_event_highlight:
            return ""
        source_name = self._normalize_text(
            market_context.get("liveMarketEventSourceName")
        ) or self._normalize_text(market_context.get("liveMarketEventSourceCode"))
        if source_name:
            return f"重点实时事件来自{source_name}：{priority_live_event_highlight}。"
        return f"重点实时事件：{priority_live_event_highlight}。"

    def _build_live_event_cluster_summary_anchor(self, market_context: dict[str, Any]) -> str:
        cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        cluster_count = cluster.get("count") or 0
        if cluster_count < 2:
            return ""
        cluster_date = self._normalize_text(cluster.get("date"))
        titles = cluster.get("titles") or []
        prefix = f"{cluster_date} 同步披露 {cluster_count} 份高影响公告"
        if titles:
            return f"{prefix}，包括{self._join_title_list(titles)}。"
        return f"{prefix}。"

    def _build_primary_live_event_highlight(self, market_context: dict[str, Any]) -> str:
        priority_live_event = self._extract_priority_live_event(market_context)
        priority_live_event_highlight = self._format_live_event_brief(priority_live_event)
        if not priority_live_event_highlight:
            return ""
        return f"实时事件：{priority_live_event_highlight}"

    def _build_live_event_cluster_highlight(self, market_context: dict[str, Any]) -> str:
        cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        cluster_count = cluster.get("count") or 0
        if cluster_count < 2:
            return ""
        cluster_date = self._normalize_text(cluster.get("date"))
        titles = cluster.get("titles") or []
        prefix = f"实时事件：{cluster_date} 同步披露 {cluster_count} 份高影响公告"
        if titles:
            return f"{prefix}，包括{self._join_title_list(titles)}"
        return prefix

    def _format_live_event_brief(self, live_event: Any) -> str:
        if not isinstance(live_event, dict):
            return ""
        title = self._normalize_text(live_event.get("title"))
        summary = self._normalize_text(live_event.get("summary"))
        occurred_at = self._normalize_text(live_event.get("occurredAt"))
        impact_level = self._normalize_text(live_event.get("impactLevel"))
        parts = [part for part in (occurred_at, title or summary, impact_level) if part]
        if parts:
            return " / ".join(parts)
        return summary

    def _normalize_text(self, value: Any) -> str:
        if value is None:
            return ""
        return str(value).strip()

    def _dedupe_text_list(self, values: list[str]) -> list[str]:
        deduped: list[str] = []
        seen: set[str] = set()
        for item in values:
            text = self._normalize_text(item)
            if not text or text in seen:
                continue
            seen.add(text)
            deduped.append(text)
        return deduped

    def _build_live_event_stats(self, market_context: dict[str, Any]) -> dict[str, int]:
        counts = {"highCount": 0, "mediumCount": 0, "lowCount": 0}
        live_events = market_context.get("liveMarketEvents") or []
        if not isinstance(live_events, list):
            return counts

        for item in live_events:
            if not isinstance(item, dict):
                continue
            level = self._normalize_text(item.get("impactLevel")).upper()
            if level == "HIGH":
                counts["highCount"] += 1
            elif level == "LOW":
                counts["lowCount"] += 1
            else:
                counts["mediumCount"] += 1
        return counts

    def _extract_priority_live_event_titles(
        self,
        market_context: dict[str, Any],
        limit: int = 2,
    ) -> list[str]:
        cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        cluster_titles = cluster.get("titles")
        if isinstance(cluster_titles, list) and cluster_titles:
            return [
                self._normalize_text(title)
                for title in cluster_titles[:limit]
                if self._normalize_text(title)
            ]

        titles: list[str] = []
        for item in self._extract_live_event_briefs(market_context, limit=limit):
            title = self._normalize_text(item.get("title"))
            if title:
                titles.append(title)
        return titles

    def _build_high_impact_live_event_cluster(self, market_context: dict[str, Any]) -> dict[str, Any] | None:
        live_events = market_context.get("liveMarketEvents") or []
        if not isinstance(live_events, list):
            return None

        grouped: dict[str, list[dict[str, Any]]] = {}
        for index, item in enumerate(live_events):
            if not isinstance(item, dict):
                continue
            if self._normalize_text(item.get("impactLevel")).upper() != "HIGH":
                continue
            date_label = self._extract_live_event_date_label(item.get("occurredAt"))
            if not date_label:
                continue
            title = self._normalize_text(item.get("eventTitle"))
            summary = self._normalize_text(item.get("eventSummary")) or title
            if not title and not summary:
                continue
            grouped.setdefault(date_label, []).append({
                "title": title,
                "summary": summary,
                "occurredAt": self._normalize_text(item.get("occurredAt")),
                "impactLevel": self._normalize_text(item.get("impactLevel")),
                "sourceUrl": self._normalize_text(item.get("sourceUrl")),
                "_originalIndex": index,
            })

        best_date = ""
        best_items: list[dict[str, Any]] = []
        for date_label, items in grouped.items():
            if len(items) < 2:
                continue
            if len(items) > len(best_items) or (len(items) == len(best_items) and date_label > best_date):
                best_date = date_label
                best_items = items

        if len(best_items) < 2:
            return None

        ranked_items = sorted(
            best_items,
            key=lambda item: (
                -self._live_event_title_rank(item.get("title")),
                -self._live_event_time_rank(item.get("occurredAt")),
                item.get("_originalIndex", 0),
            ),
        )
        titles = [
            self._normalize_text(item.get("title"))
            for item in ranked_items[:2]
            if self._normalize_text(item.get("title"))
        ]
        return {
            "date": best_date,
            "count": len(best_items),
            "titles": titles,
            "primaryEvent": {
                "title": self._normalize_text(ranked_items[0].get("title")),
                "summary": self._normalize_text(ranked_items[0].get("summary")),
                "occurredAt": self._normalize_text(ranked_items[0].get("occurredAt")),
                "impactLevel": "HIGH",
                "sourceUrl": self._normalize_text(ranked_items[0].get("sourceUrl")),
            },
        }

    def _extract_priority_live_event(self, market_context: dict[str, Any]) -> dict[str, Any] | None:
        cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        primary_event = cluster.get("primaryEvent")
        if isinstance(primary_event, dict) and (
            self._normalize_text(primary_event.get("title"))
            or self._normalize_text(primary_event.get("summary"))
        ):
            return {
                "title": self._normalize_text(primary_event.get("title")),
                "summary": self._normalize_text(primary_event.get("summary")),
                "occurredAt": self._normalize_text(primary_event.get("occurredAt")),
                "impactLevel": self._normalize_text(primary_event.get("impactLevel")),
                "sourceUrl": self._normalize_text(primary_event.get("sourceUrl")),
            }

        live_event_briefs = self._extract_live_event_briefs(market_context, limit=1)
        return live_event_briefs[0] if live_event_briefs else None

    def _join_title_list(self, titles: list[str]) -> str:
        normalized_titles = [
            self._normalize_text(title)
            for title in titles
            if self._normalize_text(title)
        ]
        if not normalized_titles:
            return ""
        quoted_titles = [f"《{title}》" for title in normalized_titles]
        return "、".join(quoted_titles)

    def _live_event_priority_rank(self, impact_level: Any) -> int:
        normalized = self._normalize_text(impact_level).upper()
        if normalized == "HIGH":
            return 3
        if normalized == "MEDIUM":
            return 2
        if normalized == "LOW":
            return 1
        return 0

    def _live_event_time_rank(self, occurred_at: Any) -> int:
        normalized = self._normalize_text(occurred_at)
        digits = "".join(char for char in normalized if char.isdigit())
        if not digits:
            return 0
        try:
            return int(digits)
        except ValueError:
            return 0

    def _live_event_title_rank(self, title: Any) -> int:
        normalized = self._normalize_text(title)
        if not normalized:
            return 0
        ranked_keywords = [
            (10, "上市公告书"),
            (9, "招股说明书"),
            (8, "募集说明书"),
            (7, "发行公告"),
            (6, "定期报告"),
            (6, "年度报告"),
            (6, "半年度报告"),
            (6, "季度报告"),
            (3, "提示性公告"),
            (1, "公司章程"),
            (1, "章程"),
        ]
        for score, keyword in ranked_keywords:
            if keyword in normalized:
                return score
        return 0

    def _extract_live_event_date_label(self, occurred_at: Any) -> str:
        normalized = self._normalize_text(occurred_at)
        digits = "".join(char for char in normalized if char.isdigit())
        if len(digits) >= 8:
            return f"{digits[:4]}-{digits[4:6]}-{digits[6:8]}"
        return ""

    def _live_event_cluster_rank(self, live_event: dict[str, Any], cluster_date: str) -> int:
        if not cluster_date:
            return 0
        if self._normalize_text(live_event.get("impactLevel")).upper() != "HIGH":
            return 0
        occurred_at = self._normalize_text(live_event.get("occurredAt"))
        if self._extract_live_event_date_label(occurred_at) != cluster_date:
            return 0
        return 1
