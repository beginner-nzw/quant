import time
from typing import Any

from app.common.task_routing import build_report_type, build_review_suggestion
from app.clients.model_client import ModelClient
from app.services.langchain_report_service import LangChainReportService
from app.services.prompt_builder_service import PromptBuilderService
from app.services.task_control_service import TaskControlService
from app.utils.logger import log_error, log_info


class ReportGenerationAgent:
    def __init__(self):
        self.langchain_report_service = LangChainReportService()
        self.model_client = ModelClient()
        self.prompt_builder_service = PromptBuilderService()
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        task_type = state.get("task_type", "")
        analysis_scope = state.get("analysis_scope", "")
        target_name = state.get("target_name") or "该标的"
        source_context = state.get("source_context", {})
        task_context = state.get("task_context") or {}
        source_task_context = state.get("source_task_context") or {}
        market_context = state.get("market_context") or {}
        evidence_items = self._normalize_evidence_items(state.get("evidence_items"))
        plan_result = state.get("plan_result") or {}
        intent_result = state.get("intent_result") or {}
        financial_result = state.get("financial_result", {})
        financial_summary = self._normalize_text(financial_result.get("summary"))
        risk_result = state.get("risk_result", {})
        risk_points = self._ensure_live_event_risk_points(
            risk_points=self._normalize_text_list(risk_result.get("riskPoints")),
            market_context=market_context,
        )
        risk_warnings = self._normalize_text_list(risk_result.get("riskWarnings"))
        need_human_review = bool(
            state.get("need_human_review") or risk_result.get("needHumanReview")
        )

        state["current_stage"] = "REPORT_GENERATION"
        state["current_node"] = "report_generation_agent"
        state["progress"] = 95
        state["status"] = "SUCCESS"

        report_type = build_report_type(task_type, analysis_scope)
        evidence_refs = self._build_evidence_refs(state)
        fallback_highlights = self._build_highlights(
            task_type=task_type,
            analysis_scope=analysis_scope,
            source_context=source_context,
            task_context=task_context,
            source_task_context=source_task_context,
            market_context=market_context,
            risk_points=risk_points,
        )
        fallback_summary = self._build_summary(
            target_name=target_name,
            task_type=task_type,
            analysis_scope=analysis_scope,
            source_context=source_context,
            task_context=task_context,
            source_task_context=source_task_context,
            market_context=market_context,
            financial_summary=financial_summary,
            risk_points=risk_points,
        )
        fallback_report = {
            "reportId": "",
            "reportType": report_type,
            "summary": fallback_summary,
            "highlights": fallback_highlights,
            "riskPoints": risk_points,
            "riskWarnings": risk_warnings,
            "needHumanReview": need_human_review,
            "confidenceScore": 0.76 if need_human_review else 0.88,
            "reviewSuggestion": build_review_suggestion(task_type, need_human_review),
        }
        model_report, llm_framework, model_name, generation_path, fallback_reason = self._generate_model_report(state, fallback_report)
        final_need_human_review = self._resolve_bool(
            model_report.get("needHumanReview") if model_report else None,
            fallback_report["needHumanReview"],
        )
        final_risk_points = self._ensure_live_event_risk_points(
            risk_points=self._resolve_text_list(
                model_report.get("riskPoints") if model_report else None,
                fallback_report["riskPoints"],
            ),
            market_context=market_context,
        )
        final_review_suggestion = self._normalize_text(
            model_report.get("reviewSuggestion") if model_report else None
        ) or build_review_suggestion(task_type, final_need_human_review)
        final_review_suggestion = self._ensure_live_event_review_suggestion(
            review_suggestion=final_review_suggestion,
            need_human_review=final_need_human_review,
            market_context=market_context,
            evidence_items=evidence_items,
        )
        (
            final_summary,
            live_event_summary_anchored,
            live_event_summary_anchor,
            live_event_summary_anchor_status,
        ) = self._ensure_live_event_summary(
            summary=self._normalize_text(
                model_report.get("summary") if model_report else None
            ) or fallback_report["summary"],
            market_context=market_context,
        )
        (
            final_highlights,
            live_event_highlight_anchored,
            live_event_highlight_anchor,
            live_event_highlight_anchor_status,
        ) = self._ensure_live_event_highlights(
            highlights=self._resolve_text_list(
                model_report.get("highlights") if model_report else None,
                fallback_report["highlights"],
            ),
            market_context=market_context,
        )
        summary_lead_anchors = self._build_live_event_summary_lead_anchors(market_context)
        highlight_lead_anchors = self._build_live_event_highlight_lead_anchors(market_context)
        summary_lead_anchors_covered = self._has_summary_lead_anchor_coverage(
            final_summary,
            market_context,
        )
        highlight_lead_anchors_covered = self._has_highlight_lead_anchor_coverage(
            final_highlights,
            market_context,
        )
        summary_lead_coverage_status = self._resolve_live_event_lead_coverage_status(
            lead_anchors=summary_lead_anchors,
            covered=summary_lead_anchors_covered,
            anchor_status=live_event_summary_anchor_status,
        )
        highlight_lead_coverage_status = self._resolve_live_event_lead_coverage_status(
            lead_anchors=highlight_lead_anchors,
            covered=highlight_lead_anchors_covered,
            anchor_status=live_event_highlight_anchor_status,
        )

        state["need_human_review"] = final_need_human_review
        state["review_suggestion"] = final_review_suggestion
        state["report_result"] = {
            "reportId": "",
            "reportType": report_type,
            "summary": final_summary,
            "highlights": final_highlights,
            "riskPoints": final_risk_points,
            "riskWarnings": self._resolve_text_list(
                model_report.get("riskWarnings") if model_report else None,
                fallback_report["riskWarnings"],
            ),
            "needHumanReview": final_need_human_review,
            "confidenceScore": self._resolve_confidence_score(
                model_report.get("confidenceScore") if model_report else None,
                fallback_report["confidenceScore"],
            ),
            "contextSnapshot": self._build_context_snapshot(
                task_context=task_context,
                source_task_context=source_task_context,
                market_context=market_context,
                source_context=source_context,
                plan_result=plan_result,
                intent_result=intent_result,
                generation_mode="MODEL_ASSISTED" if model_report else "RULE_FALLBACK",
                model_name=model_name,
                llm_framework=llm_framework,
                generation_path=generation_path,
                fallback_reason=fallback_reason,
                live_event_summary_anchored=live_event_summary_anchored,
                live_event_summary_anchor=live_event_summary_anchor,
                live_event_summary_anchor_status=live_event_summary_anchor_status,
                summary_lead_anchors=summary_lead_anchors,
                summary_lead_anchors_covered=summary_lead_anchors_covered,
                summary_lead_coverage_status=summary_lead_coverage_status,
                live_event_highlight_anchored=live_event_highlight_anchored,
                live_event_highlight_anchor=live_event_highlight_anchor,
                live_event_highlight_anchor_status=live_event_highlight_anchor_status,
                highlight_lead_anchors=highlight_lead_anchors,
                highlight_lead_anchors_covered=highlight_lead_anchors_covered,
                highlight_lead_coverage_status=highlight_lead_coverage_status,
                evidence_items=evidence_items,
            ),
            "evidenceItems": evidence_items,
            "evidenceRefs": evidence_refs,
            "reviewSuggestion": final_review_suggestion,
        }
        state["evidence_items"] = evidence_items
        state["evidence_refs"] = evidence_refs
        state.setdefault("agent_audits", []).append(
            {
                "executionId": f"exec-{state['task_id']}-report",
                "agentCode": "report_generation_agent",
                "agentName": "Report Generation Agent",
                "nodeCode": "report_generation_agent",
                "status": "SUCCESS",
                "confidenceScore": 0.80 if need_human_review else 0.90,
                "needHumanReview": need_human_review,
                "startTimestamp": now,
                "finishTimestamp": now,
                "durationMs": 0,
            }
        )
        return state

    def _build_summary(
        self,
        *,
        target_name: str,
        task_type: str,
        analysis_scope: str,
        source_context: dict[str, Any],
        task_context: dict[str, Any],
        source_task_context: dict[str, Any],
        market_context: dict[str, Any],
        financial_summary: str,
        risk_points: list[str],
    ) -> str:
        source_domain = source_context.get("sourceDomain") or "当前任务"
        source_event_title = self._normalize_text(source_context.get("sourceEventTitle"))
        latest_insight_summary = self._normalize_text(market_context.get("latestInsightSummary"))
        source_report_summary = self._extract_report_summary(source_task_context.get("report"))
        task_summary = task_context.get("summary") or {}
        step_count = task_summary.get("stepCount")
        agent_count = task_summary.get("agentCount")
        context_tail = self._build_context_tail(step_count, agent_count, market_context)
        base_summary = financial_summary or f"{target_name} 已完成本轮分析。"

        if task_type == "RISK_REVIEW":
            prefix = (
                f"{target_name} 已完成风险复核，"
                f"本轮复核基于 {source_domain} 的来源上下文和平台近期投研快照。"
            )
            if latest_insight_summary:
                return f"{prefix}最新洞察：{latest_insight_summary}{base_summary}{context_tail}"
            return f"{prefix}{base_summary}{context_tail}"

        if task_type == "AUDIT_REVIEW":
            prefix = (
                f"{target_name} 已完成审计复核，"
                "当前结论结合了任务详情、报告状态和最近工作台洞察。"
            )
            return f"{prefix}{base_summary}{context_tail}"

        if task_type == "REPORT_REVIEW":
            prefix = (
                f"{target_name} 已完成报告复核，"
                "本次判断同时参考了原报告摘要、任务执行轨迹和平台最新洞察。"
            )
            if source_report_summary:
                return f"{prefix}原报告摘要：{source_report_summary}{base_summary}{context_tail}"
            return f"{prefix}{base_summary}{context_tail}"

        if task_type == "FOLLOW_UP_RESEARCH":
            follow_up_hint = "本次结果用于跟踪来源线索后的业务变化。"
            if analysis_scope == "SIGNAL_FOLLOW_UP":
                follow_up_hint = "本次结果用于验证策略信号是否延续。"
            elif analysis_scope == "INTELLIGENCE_FOLLOW_UP":
                follow_up_hint = "本次结果用于跟踪市场情报变化是否落地。"
            elif analysis_scope == "REPORT_FOLLOW_UP":
                follow_up_hint = "本次结果用于跟踪报告结论后的最新变化。"
            if source_domain == "MARKET_EVENT" and source_event_title:
                follow_up_hint = f"本次结果用于跟踪市场事件“{source_event_title}”的后续影响。"
            prefix = f"{target_name} 已完成跟踪研究。{follow_up_hint}"
            if latest_insight_summary:
                return f"{prefix}平台最新洞察：{latest_insight_summary}{base_summary}{context_tail}"
            return f"{prefix}{base_summary}{context_tail}"

        if risk_points:
            return (
                f"{target_name} 已完成深度研究，"
                "报告覆盖财务分析、平台近期洞察与关键风险判断。"
                f"{base_summary}{context_tail}"
            )
        return f"{target_name} 已完成深度研究。{base_summary}{context_tail}"

    def _build_highlights(
        self,
        *,
        task_type: str,
        analysis_scope: str,
        source_context: dict[str, Any],
        task_context: dict[str, Any],
        source_task_context: dict[str, Any],
        market_context: dict[str, Any],
        risk_points: list[str],
    ) -> list[str]:
        highlights: list[str] = []
        source_domain = source_context.get("sourceDomain")
        source_event_title = self._normalize_text(source_context.get("sourceEventTitle"))
        context_source = task_context.get("contextSource")
        market_data_source = market_context.get("dataSource")
        latest_insight_summary = self._normalize_text(market_context.get("latestInsightSummary"))
        source_report_summary = self._extract_report_summary(source_task_context.get("report"))
        live_event_briefs = self._extract_live_event_briefs(market_context)
        live_event_source_name = self._normalize_text(
            market_context.get("liveMarketEventSourceName")
        ) or self._normalize_text(market_context.get("liveMarketEventSourceCode"))
        task_summary = task_context.get("summary") or {}

        if context_source:
            highlights.append(f"任务上下文来自 {context_source}")
        if market_data_source:
            highlights.append(f"市场快照来自 {market_data_source}")
        if source_domain:
            highlights.append(f"已承接 {source_domain} 的来源上下文")
        if source_event_title:
            highlights.append(f"已承接市场事件：{source_event_title}")
        if source_report_summary:
            highlights.append("已引用来源报告摘要进行交叉验证")
        if latest_insight_summary:
            highlights.append("已合并平台最新投研洞察")

        if live_event_briefs:
            if live_event_source_name:
                highlights.append(f"已引入 {live_event_source_name} 的实时事件")
            for item in live_event_briefs[:2]:
                parts = [part for part in (item["occurredAt"], item["title"], item["impactLevel"]) if part]
                if parts:
                    highlights.append(f"实时事件：{' / '.join(parts)}")

        task_count = market_context.get("taskCount")
        report_count = market_context.get("reportCount")
        pending_review_count = market_context.get("pendingReviewCount")
        if isinstance(task_count, int) and isinstance(report_count, int):
            highlights.append(
                f"相同标的历史任务 {task_count} 条，报告 {report_count} 份"
            )
        if isinstance(pending_review_count, int) and pending_review_count > 0:
            highlights.append(f"当前仍有 {pending_review_count} 份报告处于待审核状态")

        step_count = task_summary.get("stepCount")
        agent_count = task_summary.get("agentCount")
        if step_count and agent_count:
            highlights.append(f"本次任务已累积 {step_count} 个步骤，涉及 {agent_count} 个 Agent")

        if task_type == "FOLLOW_UP_RESEARCH":
            highlights.append(f"已围绕 {analysis_scope} 场景生成跟踪结论")
        elif task_type == "REPORT_REVIEW":
            highlights.append("已对原报告结论和新增上下文进行差异比对")
        elif task_type == "AUDIT_REVIEW":
            highlights.append("已同步合规留痕与报告状态")
        elif task_type == "RISK_REVIEW":
            highlights.append("已结合风险场景和平台近期洞察产生复核结论")
        else:
            highlights.append("已基于平台上下文生成本轮投研结论")

        if risk_points:
            highlights.append(f"报告已覆盖 {len(risk_points)} 条关键风险点")

        return highlights[:6]

    def _build_evidence_refs(self, state: dict) -> list[str]:
        refs = list(state.get("evidence_refs") or [])
        refs.extend([
            f"stock:{state['target_code']}",
            f"taskType:{state.get('task_type', '')}",
            f"analysisScope:{state.get('analysis_scope', '')}",
        ])

        task_context = state.get("task_context") or {}
        source_task_context = state.get("source_task_context") or {}
        market_context = state.get("market_context") or {}
        source_context = state.get("source_context") or {}
        financial_result = state.get("financial_result") or {}
        risk_result = state.get("risk_result") or {}
        evidence_items = self._normalize_evidence_items(state.get("evidence_items"))
        priority_live_event = self._extract_priority_live_event(market_context) or {}
        priority_live_event_evidence = self._find_live_event_evidence(
            priority_live_event,
            evidence_items,
        )
        priority_live_event_evidence_status = self._resolve_live_event_evidence_status(
            priority_live_event,
            priority_live_event_evidence,
        )

        if task_context.get("contextSource"):
            refs.append(f"taskContext:{task_context['contextSource']}")

        task_detail = task_context.get("taskDetail") or {}
        if task_detail.get("traceId"):
            refs.append(f"taskTrace:{task_detail['traceId']}")

        if source_context.get("sourceTaskId"):
            refs.append(f"sourceTask:{source_context['sourceTaskId']}")
        if source_context.get("sourceReportId"):
            refs.append(f"sourceReport:{source_context['sourceReportId']}")
        if source_context.get("sourceEventId"):
            refs.append(f"sourceEvent:{source_context['sourceEventId']}")
        if source_context.get("sourceDomain"):
            refs.append(f"sourceDomain:{source_context['sourceDomain']}")
        if source_context.get("sourceEventType"):
            refs.append(f"eventType:{source_context['sourceEventType']}")
        if source_context.get("sourceEventImpactLevel"):
            refs.append(f"eventImpact:{source_context['sourceEventImpactLevel']}")

        source_report = source_task_context.get("report") or {}
        if source_report.get("reportId"):
            refs.append(f"sourceTaskReport:{source_report['reportId']}")

        if market_context.get("dataSource"):
            refs.append(f"marketData:{market_context['dataSource']}")
        if market_context.get("reportCount") is not None:
            refs.append(f"reportCount:{market_context['reportCount']}")
        if market_context.get("taskCount") is not None:
            refs.append(f"taskCount:{market_context['taskCount']}")
        if market_context.get("pendingReviewCount") is not None:
            refs.append(f"pendingReviewCount:{market_context['pendingReviewCount']}")
        if market_context.get("latestInsightReportId"):
            refs.append(f"latestInsightReport:{market_context['latestInsightReportId']}")
        live_event_source_codes = market_context.get("liveMarketEventSourceCodes") or []
        if isinstance(live_event_source_codes, list):
            for source_code in live_event_source_codes:
                normalized_source_code = self._normalize_text(source_code)
                if normalized_source_code:
                    refs.append(f"liveEventSource:{normalized_source_code}")
        elif market_context.get("liveMarketEventSourceCode"):
            refs.append(f"liveEventSource:{market_context['liveMarketEventSourceCode']}")
        if market_context.get("liveEventCount") is not None:
            refs.append(f"liveEventCount:{market_context['liveEventCount']}")
        if market_context.get("riskWarningCount") is not None:
            refs.append(f"riskWarningCount:{market_context['riskWarningCount']}")
        if market_context.get("strategySignalCount") is not None:
            refs.append(f"strategySignalCount:{market_context['strategySignalCount']}")
        if market_context.get("marketIntelligenceCount") is not None:
            refs.append(f"marketIntelligenceCount:{market_context['marketIntelligenceCount']}")
        if priority_live_event:
            if self._normalize_text(priority_live_event.get("title")):
                refs.append(
                    f"priorityLiveEventTitle:{priority_live_event['title']}"
                )
            if self._normalize_text(priority_live_event.get("occurredAt")):
                refs.append(
                    f"priorityLiveEventOccurredAt:{priority_live_event['occurredAt']}"
                )
            refs.append(
                "priorityLiveEventEvidenceMatchRule:URL_THEN_TITLE_TIME_THEN_TITLE"
            )
        refs.append(
            f"priorityLiveEventEvidenceStatus:{priority_live_event_evidence_status}"
        )
        if self._normalize_text(priority_live_event_evidence.get("referenceId")):
            refs.append(
                f"priorityLiveEventReferenceId:{priority_live_event_evidence['referenceId']}"
            )
        if self._normalize_text(priority_live_event_evidence.get("evidenceId")):
            refs.append(
                f"priorityLiveEventEvidenceId:{priority_live_event_evidence['evidenceId']}"
            )
        if self._normalize_text(priority_live_event_evidence.get("source")):
            refs.append(
                f"priorityLiveEventEvidenceSource:{priority_live_event_evidence['source']}"
            )

        plan_result = state.get("plan_result") or {}
        if plan_result.get("planningMode"):
            refs.append(f"planningMode:{plan_result['planningMode']}")
        if plan_result.get("generationMode"):
            refs.append(f"planningGeneration:{plan_result['generationMode']}")
        if plan_result.get("llmFramework"):
            refs.append(f"planningFramework:{plan_result['llmFramework']}")
        if plan_result.get("modelName"):
            refs.append(f"planningModel:{plan_result['modelName']}")

        intent_result = state.get("intent_result") or {}
        for item in intent_result.get("focusDimensions") or []:
            refs.append(f"focus:{item}")
        if intent_result.get("reviewPressure"):
            refs.append(f"reviewPressure:{intent_result['reviewPressure']}")
        if intent_result.get("generationMode"):
            refs.append(f"intentGeneration:{intent_result['generationMode']}")
        if intent_result.get("llmFramework"):
            refs.append(f"intentFramework:{intent_result['llmFramework']}")
        if intent_result.get("modelName"):
            refs.append(f"intentModel:{intent_result['modelName']}")
        if self.model_client.is_enabled("report"):
            configured_model_name = self.model_client.model_name("report")
            if configured_model_name:
                refs.append(f"modelConfigured:{configured_model_name}")
        if self.langchain_report_service.is_enabled():
            refs.append(f"reportFramework:{self.langchain_report_service.framework_name()}")
        if plan_result.get("fallbackReason"):
            refs.append(f"planningFallback:{plan_result['fallbackReason']}")
        if intent_result.get("fallbackReason"):
            refs.append(f"intentFallback:{intent_result['fallbackReason']}")
        if financial_result.get("generationMode"):
            refs.append(f"financialGeneration:{financial_result['generationMode']}")
        if financial_result.get("llmFramework"):
            refs.append(f"financialFramework:{financial_result['llmFramework']}")
        if financial_result.get("modelName"):
            refs.append(f"financialModel:{financial_result['modelName']}")
        if risk_result.get("generationMode"):
            refs.append(f"riskGeneration:{risk_result['generationMode']}")
        if risk_result.get("llmFramework"):
            refs.append(f"riskFramework:{risk_result['llmFramework']}")
        if risk_result.get("modelName"):
            refs.append(f"riskModel:{risk_result['modelName']}")
        for item in evidence_items:
            reference_id = self._normalize_text(item.get("referenceId"))
            evidence_type = self._normalize_text(item.get("evidenceType"))
            source = self._normalize_text(item.get("source"))
            if evidence_type == "SOURCE_EVENT" and reference_id:
                refs.append(f"sourceEvent:{reference_id}")
            elif evidence_type == "MARKET_EVENT" and reference_id:
                refs.append(f"recentMarketEvent:{reference_id}")
            elif evidence_type == "LIVE_MARKET_EVENT" and reference_id:
                refs.append(f"liveMarketEvent:{reference_id}")
            elif evidence_type == "SOURCE_REPORT" and reference_id:
                refs.append(f"sourceTaskReport:{reference_id}")
            elif evidence_type == "LATEST_INSIGHT" and reference_id:
                refs.append(f"latestInsightReport:{reference_id}")
            elif evidence_type == "RISK_WARNING" and reference_id:
                refs.append(f"riskWarning:{reference_id}")
            elif evidence_type == "STRATEGY_SIGNAL" and reference_id:
                refs.append(f"strategySignal:{reference_id}")
            elif evidence_type == "MARKET_INTELLIGENCE" and reference_id:
                refs.append(f"marketIntelligence:{reference_id}")
            elif evidence_type == "RECENT_TASK" and reference_id:
                refs.append(f"sourceTask:{reference_id}")
            if source:
                refs.append(f"evidenceSource:{source}")

        deduplicated: list[str] = []
        seen = set()
        for item in refs:
            normalized = self._normalize_text(item)
            if normalized and normalized not in seen:
                seen.add(normalized)
                deduplicated.append(normalized)
        return deduplicated

    def _build_context_snapshot(
        self,
        *,
        task_context: dict[str, Any],
        source_task_context: dict[str, Any],
        market_context: dict[str, Any],
        source_context: dict[str, Any],
        plan_result: dict[str, Any],
        intent_result: dict[str, Any],
        generation_mode: str,
        model_name: str | None,
        llm_framework: str | None,
        generation_path: str | None,
        fallback_reason: str | None,
        live_event_summary_anchored: bool,
        live_event_summary_anchor: str,
        live_event_summary_anchor_status: str,
        summary_lead_anchors: list[str],
        summary_lead_anchors_covered: bool,
        summary_lead_coverage_status: str,
        live_event_highlight_anchored: bool,
        live_event_highlight_anchor: str,
        live_event_highlight_anchor_status: str,
        highlight_lead_anchors: list[str],
        highlight_lead_anchors_covered: bool,
        highlight_lead_coverage_status: str,
        evidence_items: list[dict[str, Any]],
    ) -> dict[str, Any]:
        source_report = source_task_context.get("report") or {}
        source_task_detail = source_task_context.get("taskDetail") or {}
        latest_live_event = self._extract_latest_live_event(market_context) or {}
        priority_live_event = self._extract_priority_live_event(market_context) or {}
        priority_live_event_evidence = self._find_live_event_evidence(
            priority_live_event,
            evidence_items,
        )
        priority_live_event_evidence_status = self._resolve_live_event_evidence_status(
            priority_live_event,
            priority_live_event_evidence,
        )
        live_event_stats = self._build_live_event_stats(market_context)
        high_impact_cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        priority_live_event_titles = self._extract_priority_live_event_titles(market_context)
        live_event_highlights = self._build_live_event_highlights(market_context)
        evidence_sources = sorted({
            self._normalize_text(item.get("source"))
            for item in evidence_items
            if self._normalize_text(item.get("source"))
        })
        return {
            "taskContextSource": task_context.get("contextSource"),
            "marketDataSource": market_context.get("dataSource"),
            "taskSummary": task_context.get("summary") or {},
            "sourceTaskId": source_task_detail.get("taskId"),
            "sourceReportId": source_report.get("reportId"),
            "sourceEventId": source_context.get("sourceEventId"),
            "sourceEventTitle": source_context.get("sourceEventTitle"),
            "sourceEventType": source_context.get("sourceEventType"),
            "sourceEventImpactLevel": source_context.get("sourceEventImpactLevel"),
            "sourceEventOccurredAt": source_context.get("sourceEventOccurredAt"),
            "latestInsightReportId": market_context.get("latestInsightReportId"),
            "reportCount": market_context.get("reportCount"),
            "taskCount": market_context.get("taskCount"),
            "pendingReviewCount": market_context.get("pendingReviewCount"),
            "liveMarketEventSourceCode": market_context.get("liveMarketEventSourceCode"),
            "liveMarketEventSourceName": market_context.get("liveMarketEventSourceName"),
            "liveEventCount": market_context.get("liveEventCount"),
            "latestLiveEventTitle": latest_live_event.get("title"),
            "latestLiveEventOccurredAt": latest_live_event.get("occurredAt"),
            "latestLiveEventImpactLevel": latest_live_event.get("impactLevel"),
            "latestLiveEventSourceUrl": latest_live_event.get("sourceUrl"),
            "priorityLiveEventTitle": priority_live_event.get("title"),
            "priorityLiveEventOccurredAt": priority_live_event.get("occurredAt"),
            "priorityLiveEventImpactLevel": priority_live_event.get("impactLevel"),
            "priorityLiveEventSourceUrl": priority_live_event.get("sourceUrl"),
            "priorityLiveEventEvidenceId": priority_live_event_evidence.get("evidenceId"),
            "priorityLiveEventReferenceId": priority_live_event_evidence.get("referenceId"),
            "priorityLiveEventEvidenceSource": priority_live_event_evidence.get("source"),
            "priorityLiveEventEvidenceStatus": priority_live_event_evidence_status,
            "priorityLiveEventEvidenceMatchRule": "URL_THEN_TITLE_TIME_THEN_TITLE",
            "priorityLiveEventTitles": priority_live_event_titles,
            "highImpactLiveEventCount": live_event_stats.get("highCount"),
            "mediumImpactLiveEventCount": live_event_stats.get("mediumCount"),
            "lowImpactLiveEventCount": live_event_stats.get("lowCount"),
            "highImpactLiveEventClusterDate": high_impact_cluster.get("date"),
            "highImpactLiveEventClusterCount": high_impact_cluster.get("count"),
            "highImpactLiveEventClusterTitles": high_impact_cluster.get("titles") or [],
            "liveEventPriorityRule": "HIGH_IMPACT_CLUSTER_FIRST_THEN_IMPACT_DESC_THEN_TITLE_DESC_THEN_TIME_DESC",
            "liveEventHighlights": live_event_highlights,
            "policyLiveEventCount": market_context.get("policyLiveEventCount"),
            "policyLiveEventHighlights": market_context.get("policyLiveEventHighlights") or [],
            "regulatoryRiskLiveEventCount": market_context.get("regulatoryRiskLiveEventCount"),
            "regulatoryRiskLiveEventHighlights": market_context.get("regulatoryRiskLiveEventHighlights") or [],
            "priorityExternalRiskEventSummary": market_context.get("priorityExternalRiskEventSummary"),
            "summaryLeadAnchors": summary_lead_anchors,
            "summaryLeadAnchorsCovered": summary_lead_anchors_covered,
            "summaryLeadCoverageStatus": summary_lead_coverage_status,
            "highlightLeadAnchors": highlight_lead_anchors,
            "highlightLeadAnchorsCovered": highlight_lead_anchors_covered,
            "highlightLeadCoverageStatus": highlight_lead_coverage_status,
            "liveEventSummaryAnchored": live_event_summary_anchored,
            "liveEventSummaryAnchor": live_event_summary_anchor,
            "liveEventSummaryAnchorStatus": live_event_summary_anchor_status,
            "liveEventHighlightAnchored": live_event_highlight_anchored,
            "liveEventHighlightAnchor": live_event_highlight_anchor,
            "liveEventHighlightAnchorStatus": live_event_highlight_anchor_status,
            "evidenceCount": len(evidence_items),
            "evidenceSources": evidence_sources,
            "planningMode": plan_result.get("planningMode"),
            "contextReady": plan_result.get("contextReady"),
            "planningLlmFramework": plan_result.get("llmFramework"),
            "planningModelName": plan_result.get("modelName"),
            "planningGenerationMode": plan_result.get("generationMode"),
            "planningFallbackReason": plan_result.get("fallbackReason"),
            "focusDimensions": intent_result.get("focusDimensions") or [],
            "reviewPressure": intent_result.get("reviewPressure"),
            "intentLlmFramework": intent_result.get("llmFramework"),
            "intentModelName": intent_result.get("modelName"),
            "intentGenerationMode": intent_result.get("generationMode"),
            "intentFallbackReason": intent_result.get("fallbackReason"),
            "generationMode": generation_mode,
            "modelName": model_name,
            "llmFramework": llm_framework,
            "reportGenerationPath": generation_path,
            "reportFallbackReason": fallback_reason,
        }

    def _generate_model_report(
        self,
        state: dict[str, Any],
        fallback_report: dict[str, Any],
    ) -> tuple[dict[str, Any] | None, str | None, str | None, str | None, str | None]:
        langchain_failure_reason: str | None = None
        if self.langchain_report_service.is_enabled():
            langchain_result = self.langchain_report_service.generate_report(
                state=state,
                fallback_report=fallback_report,
            )
            if isinstance(langchain_result, dict):
                return (
                    langchain_result,
                    self.langchain_report_service.framework_name(),
                    self.langchain_report_service.model_name(),
                    "LANGCHAIN_PRIMARY",
                    None,
                )
            langchain_failure_reason = "LANGCHAIN_NO_RESULT"
            log_info(state.get("trace_id", ""), f"[AI-ENGINE][LANGCHAIN] report fallback reason={langchain_failure_reason}")
        else:
            langchain_failure_reason = self.langchain_report_service.availability_reason() or "LANGCHAIN_DISABLED"
            log_info(state.get("trace_id", ""), f"[AI-ENGINE][LANGCHAIN] report fallback reason={langchain_failure_reason}")

        if not self.model_client.is_enabled("report"):
            model_reason = self.model_client.availability_reason("report") or "CUSTOM_HTTP_DISABLED"
            log_info(state.get("trace_id", ""), f"[AI-ENGINE][MODEL] report fallback to rule reason={model_reason}")
            return None, None, None, "RULE_FALLBACK", langchain_failure_reason or model_reason

        system_prompt, user_prompt = self.prompt_builder_service.build_report_prompts(
            state=state,
            fallback_report=fallback_report,
        )
        model_result = self.model_client.generate_report(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            trace_id=state.get("trace_id", ""),
        )
        if not isinstance(model_result, dict):
            log_info(state.get("trace_id", ""), "[AI-ENGINE][MODEL] report fallback to rule reason=CUSTOM_HTTP_NO_RESULT")
            return None, None, None, "RULE_FALLBACK", langchain_failure_reason or "CUSTOM_HTTP_NO_RESULT"

        if not self._normalize_text(model_result.get("summary")):
            log_error(state.get("trace_id", ""), "[AI-ENGINE][MODEL] summary missing, fallback applied")
            return None, None, None, "RULE_FALLBACK", langchain_failure_reason or "CUSTOM_HTTP_EMPTY_SUMMARY"
        log_info(
            state.get("trace_id", ""),
            f"[AI-ENGINE][MODEL] report fallback to custom-http reason={langchain_failure_reason or 'LANGCHAIN_SKIPPED'}",
        )
        return model_result, "custom-http", self.model_client.model_name("report"), "CUSTOM_HTTP_FALLBACK", langchain_failure_reason

    def _build_context_tail(
        self,
        step_count: Any,
        agent_count: Any,
        market_context: dict[str, Any],
    ) -> str:
        tail_parts: list[str] = []
        if isinstance(step_count, int) and step_count > 0:
            tail_parts.append(f"本次任务执行 {step_count} 个步骤")
        if isinstance(agent_count, int) and agent_count > 0:
            tail_parts.append(f"涉及 {agent_count} 个 Agent")
        if isinstance(market_context.get("reportCount"), int):
            tail_parts.append(f"参考了 {market_context['reportCount']} 份同标的报告记录")
        live_event_source_name = self._normalize_text(
            market_context.get("liveMarketEventSourceName")
        ) or self._normalize_text(market_context.get("liveMarketEventSourceCode"))
        live_event_count = market_context.get("liveEventCount")
        if isinstance(live_event_count, int) and live_event_count > 0:
            if live_event_source_name:
                tail_parts.append(f"同步了 {live_event_source_name} 的实时事件 {live_event_count} 条")
            else:
                tail_parts.append(f"同步了实时市场事件 {live_event_count} 条")
        high_impact_cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        cluster_count = high_impact_cluster.get("count")
        cluster_date = self._normalize_text(high_impact_cluster.get("date"))
        if isinstance(cluster_count, int) and cluster_count >= 2 and cluster_date:
            tail_parts.append(f"其中 {cluster_date} 高影响公告 {cluster_count} 份")
        priority_live_event_highlight = self._format_live_event_brief(
            self._extract_priority_live_event(market_context)
        )
        if priority_live_event_highlight:
            tail_parts.append(f"重点事件包括 {priority_live_event_highlight}")
        if not tail_parts:
            return ""
        return "。" + "，".join(tail_parts) + "。"

    def _ensure_live_event_summary(
        self,
        *,
        summary: str,
        market_context: dict[str, Any],
    ) -> tuple[str, bool, str, str]:
        normalized_summary = self._normalize_text(summary)
        if not normalized_summary:
            return normalized_summary, False, "", "NOT_APPLICABLE"
        if not self._extract_priority_live_event(market_context):
            return normalized_summary, False, "", "NOT_APPLICABLE"
        lead_summary = self._extract_summary_lead_segment(normalized_summary)
        cluster_anchor = self._build_live_event_cluster_summary_anchor(market_context)
        has_cluster_in_lead = self._mentions_live_event_cluster(lead_summary, market_context)
        has_priority_live_event_in_lead = self._mentions_live_event(lead_summary, market_context)
        if has_priority_live_event_in_lead and (not cluster_anchor or has_cluster_in_lead):
            return normalized_summary, False, self._build_live_event_summary_anchor(market_context), "MODEL_NATIVE"
        live_event_anchor = self._build_live_event_summary_anchor(market_context)
        prefix_parts: list[str] = []
        if cluster_anchor and not has_cluster_in_lead:
            prefix_parts.append(cluster_anchor)
        if not has_priority_live_event_in_lead and live_event_anchor:
            prefix_parts.append(live_event_anchor)
        if not prefix_parts:
            return normalized_summary, False, "", "NOT_APPLICABLE"
        anchor_text = "".join(prefix_parts)
        return f"{anchor_text}{normalized_summary}", True, anchor_text, "POST_PROCESS_ANCHORED"

    def _extract_summary_lead_segment(self, summary: str) -> str:
        normalized_summary = self._normalize_text(summary)
        if not normalized_summary:
            return ""
        lead_length = min(len(normalized_summary), max(40, len(normalized_summary) // 2))
        return normalized_summary[:lead_length]

    def _ensure_live_event_highlights(
        self,
        *,
        highlights: list[str],
        market_context: dict[str, Any],
    ) -> tuple[list[str], bool, str, str]:
        normalized_highlights = self._dedupe_text_list(self._normalize_text_list(highlights))
        if not self._extract_priority_live_event(market_context):
            return normalized_highlights[:6], False, "", "NOT_APPLICABLE"
        highlight_lead_anchors = self._build_live_event_highlight_lead_anchors(market_context)
        if highlight_lead_anchors:
            live_event_highlight = self._build_primary_live_event_highlight(market_context)
            cluster_highlight = self._build_live_event_cluster_highlight(market_context)
            desired_lead_highlights: list[str] = []
            if cluster_highlight:
                desired_lead_highlights.append(
                    next(
                        (
                            item
                            for item in normalized_highlights
                            if self._mentions_live_event_cluster(item, market_context)
                        ),
                        cluster_highlight,
                    )
                )
            if live_event_highlight:
                desired_lead_highlights.append(
                    next(
                        (
                            item
                            for item in normalized_highlights
                            if self._mentions_live_event(item, market_context)
                            and not self._mentions_live_event_cluster(item, market_context)
                        ),
                        live_event_highlight,
                    )
                )
            current_lead_highlights = normalized_highlights[:len(desired_lead_highlights)]
            if desired_lead_highlights and current_lead_highlights == desired_lead_highlights:
                return normalized_highlights[:6], False, "；".join(highlight_lead_anchors), "MODEL_NATIVE"
            remaining_highlights = [
                item
                for item in normalized_highlights
                if not self._mentions_live_event_cluster(item, market_context)
                and not (
                    self._mentions_live_event(item, market_context)
                    and not self._mentions_live_event_cluster(item, market_context)
                )
            ]
            return (
                self._dedupe_text_list([*desired_lead_highlights, *remaining_highlights])[:6],
                True,
                "；".join(desired_lead_highlights),
                "POST_PROCESS_ANCHORED",
            )
        changed = False
        anchor_highlights: list[str] = []
        cluster_highlight = self._build_live_event_cluster_highlight(market_context)
        if cluster_highlight and not any(
            self._mentions_live_event_cluster(item, market_context) for item in normalized_highlights
        ):
            normalized_highlights = [cluster_highlight, *normalized_highlights]
            changed = True
            anchor_highlights.append(cluster_highlight)
        live_event_highlight = self._build_primary_live_event_highlight(market_context)
        if not normalized_highlights:
            if live_event_highlight:
                return [live_event_highlight], True, live_event_highlight, "POST_PROCESS_ANCHORED"
            return normalized_highlights, False, "", "NOT_APPLICABLE"

        for index, item in enumerate(normalized_highlights):
            if not self._mentions_live_event(item, market_context):
                continue
            if index == 0:
                if changed:
                    return normalized_highlights[:6], True, "；".join(anchor_highlights), "POST_PROCESS_ANCHORED"
                return normalized_highlights[:6], False, item, "MODEL_NATIVE"
            prioritized = [
                normalized_highlights[index],
                *normalized_highlights[:index],
                *normalized_highlights[index + 1:],
            ]
            return self._dedupe_text_list(prioritized)[:6], True, item, "POST_PROCESS_ANCHORED"

        if not live_event_highlight:
            if changed:
                return normalized_highlights[:6], True, "；".join(anchor_highlights), "POST_PROCESS_ANCHORED"
            return normalized_highlights[:6], False, "", "NOT_APPLICABLE"
        anchor_highlights.append(live_event_highlight)
        return (
            self._dedupe_text_list([live_event_highlight, *normalized_highlights])[:6],
            True,
            "；".join(anchor_highlights),
            "POST_PROCESS_ANCHORED",
        )

    def _ensure_live_event_risk_points(
        self,
        *,
        risk_points: list[str],
        market_context: dict[str, Any],
    ) -> list[str]:
        normalized_risk_points = self._dedupe_text_list(self._normalize_text_list(risk_points))
        external_risk_points = self._dedupe_text_list([
            self._build_regulatory_risk_event_risk_point(market_context),
            self._build_policy_event_risk_point(market_context),
        ])
        if not self._extract_priority_live_event(market_context):
            return self._dedupe_text_list([*external_risk_points, *normalized_risk_points])[:5]

        missing_risk_points: list[str] = []
        for external_risk_point in external_risk_points:
            if external_risk_point and all(external_risk_point not in item for item in normalized_risk_points):
                missing_risk_points.append(external_risk_point)

        cluster_risk_point = self._build_live_event_cluster_risk_point(market_context)
        if cluster_risk_point and not any(
            self._mentions_live_event_cluster(item, market_context) for item in normalized_risk_points
        ):
            missing_risk_points.append(cluster_risk_point)

        priority_risk_point = self._build_priority_live_event_risk_point(market_context)
        if priority_risk_point and not any(
            self._mentions_live_event(item, market_context)
            and not self._mentions_live_event_cluster(item, market_context)
            for item in normalized_risk_points
        ):
            missing_risk_points.append(priority_risk_point)

        if not missing_risk_points:
            return normalized_risk_points[:5]
        return self._dedupe_text_list([*missing_risk_points, *normalized_risk_points])[:5]

    def _ensure_live_event_review_suggestion(
        self,
        *,
        review_suggestion: str,
        need_human_review: bool,
        market_context: dict[str, Any],
        evidence_items: list[dict[str, Any]],
    ) -> str:
        normalized_review_suggestion = self._normalize_text(review_suggestion)
        if not need_human_review:
            return normalized_review_suggestion

        priority_live_event = self._extract_priority_live_event(market_context) or {}
        if not priority_live_event:
            return normalized_review_suggestion

        priority_live_event_evidence = self._find_live_event_evidence(
            priority_live_event,
            evidence_items,
        )
        priority_live_event_evidence_status = self._resolve_live_event_evidence_status(
            priority_live_event,
            priority_live_event_evidence,
        )
        hints: list[str] = []
        if priority_live_event_evidence_status == "MISSING":
            hints.append("补充优先实时事件对应的官方公告原文或可追溯证据链")

        priority_title = self._normalize_text(priority_live_event.get("title"))
        if any(keyword in priority_title for keyword in ("上市公告书", "招股说明书", "募集说明书", "发行公告", "提示性公告")):
            hints.append("继续跟踪后续经营披露、定期报告和盈利兑现信息")
        elif any(keyword in priority_title for keyword in ("公司章程", "章程")):
            hints.append("继续关注后续财务与经营公告，避免将治理披露直接外推为经营改善")
        elif self._normalize_text(priority_live_event.get("impactLevel")).upper() == "HIGH":
            hints.append("补充高影响公告后续进展及其对经营的实际影响验证")

        if not hints:
            return normalized_review_suggestion

        parts: list[str] = []
        if normalized_review_suggestion:
            parts.append(normalized_review_suggestion.rstrip("。"))
        for hint in hints:
            normalized_hint = self._normalize_text(hint).rstrip("。")
            if normalized_hint and all(normalized_hint not in item for item in parts):
                parts.append(normalized_hint)
        if not parts:
            return ""
        return "；".join(parts) + "。"

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

    def _build_live_event_cluster_risk_point(self, market_context: dict[str, Any]) -> str:
        cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        cluster_count = cluster.get("count") or 0
        if cluster_count < 2:
            return ""
        cluster_date = self._normalize_text(cluster.get("date"))
        titles = cluster.get("titles") or []
        prefix = f"{cluster_date} 同步披露 {cluster_count} 份高影响公告"
        if titles:
            prefix = f"{prefix}，包括{self._join_title_list(titles)}"
        return f"{prefix}，短期信息流密集，仍需结合后续公告持续验证其对经营与盈利的实际影响。"

    def _build_priority_live_event_risk_point(self, market_context: dict[str, Any]) -> str:
        priority_live_event = self._extract_priority_live_event(market_context) or {}
        priority_title = self._normalize_text(priority_live_event.get("title"))
        if not priority_title:
            return ""
        if any(keyword in priority_title for keyword in ("上市公告书", "招股说明书", "募集说明书", "发行公告", "提示性公告")):
            return (
                f"当前重点公告《{priority_title}》更多反映发行或上市披露进度，"
                "经营细节与盈利兑现仍需后续公告和定期报告持续验证。"
            )
        if any(keyword in priority_title for keyword in ("公司章程", "章程")):
            return (
                f"当前重点公告《{priority_title}》偏向治理与制度安排，"
                "不能直接替代经营基本面判断，仍需继续跟踪后续财务与业务披露。"
            )
        if self._normalize_text(priority_live_event.get("impactLevel")).upper() == "HIGH":
            return (
                f"当前重点高影响公告《{priority_title}》已进入观察重点，"
                "其对经营和盈利层面的实际影响仍需结合后续披露继续验证。"
            )
        return ""

    def _build_regulatory_risk_event_risk_point(self, market_context: dict[str, Any]) -> str:
        highlight = self._extract_first_live_event_detail(
            market_context.get("regulatoryRiskLiveEvents"),
            market_context.get("regulatoryRiskLiveEventHighlights"),
        )
        if not highlight:
            return ""
        if "未命中该标的精确监管记录" in highlight:
            return f"监管风险背景事件显示：{highlight}，该信息不等同于标的专属处罚，但应作为监管环境变化跟踪项。"
        return f"监管风险事件显示：{highlight}，需核实是否触发标的风险重估、信息披露或合规复核要求。"

    def _build_policy_event_risk_point(self, market_context: dict[str, Any]) -> str:
        highlight = self._extract_first_live_event_detail(
            market_context.get("policyLiveEvents"),
            market_context.get("policyLiveEventHighlights"),
        )
        if not highlight:
            return ""
        return f"政策事件显示：{highlight}，需评估政策执行节奏及行业传导对经营假设的影响。"

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

    def _has_summary_lead_anchor_coverage(
        self,
        summary: str,
        market_context: dict[str, Any],
    ) -> bool:
        if not self._build_live_event_summary_lead_anchors(market_context):
            return False
        lead_summary = self._extract_summary_lead_segment(summary)
        if not lead_summary:
            return False
        cluster_anchor = self._build_live_event_cluster_summary_anchor(market_context)
        if cluster_anchor and not self._mentions_live_event_cluster(lead_summary, market_context):
            return False
        live_event_anchor = self._build_live_event_summary_anchor(market_context)
        if live_event_anchor and not self._mentions_live_event(lead_summary, market_context):
            return False
        return True

    def _has_highlight_lead_anchor_coverage(
        self,
        highlights: list[str],
        market_context: dict[str, Any],
    ) -> bool:
        if not self._build_live_event_highlight_lead_anchors(market_context):
            return False
        normalized_highlights = self._dedupe_text_list(self._normalize_text_list(highlights))
        lead_highlights = normalized_highlights[:2]
        if not lead_highlights:
            return False
        cluster_index: int | None = None
        direct_index: int | None = None
        for index, item in enumerate(lead_highlights):
            if cluster_index is None and self._mentions_live_event_cluster(item, market_context):
                cluster_index = index
            if direct_index is None and (
                self._mentions_live_event(item, market_context)
                and not self._mentions_live_event_cluster(item, market_context)
            ):
                direct_index = index
        live_event_highlight = self._build_primary_live_event_highlight(market_context)
        if live_event_highlight and direct_index is None:
            return False
        cluster_highlight = self._build_live_event_cluster_highlight(market_context)
        if cluster_highlight and cluster_index is None:
            return False
        if cluster_highlight and live_event_highlight and cluster_index > direct_index:
            return False
        return True

    def _resolve_live_event_lead_coverage_status(
        self,
        *,
        lead_anchors: list[str],
        covered: bool,
        anchor_status: str,
    ) -> str:
        if not lead_anchors:
            return "NOT_APPLICABLE"
        if not covered:
            return "COVERAGE_GAP"
        normalized_anchor_status = self._normalize_text(anchor_status).upper()
        if normalized_anchor_status == "POST_PROCESS_ANCHORED":
            return "POST_PROCESS_ANCHORED"
        return "MODEL_NATIVE"

    def _mentions_live_event(self, text: str, market_context: dict[str, Any]) -> bool:
        normalized_text = self._normalize_text(text).replace(" ", "")
        priority_live_event = self._extract_priority_live_event(market_context)
        if not normalized_text or not priority_live_event:
            return False

        for field in ("title", "summary"):
            for token in self._extract_live_event_match_tokens(priority_live_event.get(field)):
                if token and token in normalized_text:
                    return True

        if any(
            token in normalized_text
            for token in self._extract_live_event_date_match_tokens(priority_live_event.get("occurredAt"))
        ) and any(
            keyword in normalized_text for keyword in ("公告", "事件")
        ):
            return True

        impact_level = self._normalize_text(priority_live_event.get("impactLevel"))
        if impact_level and impact_level in normalized_text and any(
            keyword in normalized_text for keyword in ("公告", "事件", "影响")
        ):
            return True
        return False

    def _mentions_live_event_cluster(self, text: str, market_context: dict[str, Any]) -> bool:
        cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        cluster_count = cluster.get("count") or 0
        if cluster_count < 2:
            return False

        normalized_text = self._normalize_text(text).replace(" ", "")
        if not normalized_text:
            return False
        if (
            any(token in normalized_text for token in self._extract_live_event_cluster_count_tokens(cluster_count))
            or "多份高影响公告" in normalized_text
            or "多项高影响公告" in normalized_text
            or (
                any(
                    token in normalized_text
                    for token in self._extract_live_event_date_match_tokens(cluster.get("date"))
                )
                and "高影响公告" in normalized_text
            )
        ):
            return True

        matched_titles = 0
        for title in (cluster.get("titles") or []):
            for token in self._extract_live_event_cluster_match_tokens(title):
                if token and token in normalized_text:
                    matched_titles += 1
                    break
        return matched_titles >= 2

    def _build_live_event_highlights(self, market_context: dict[str, Any]) -> list[str]:
        highlights: list[str] = []
        for item in self._extract_live_event_briefs(market_context):
            highlight = self._format_live_event_brief(item)
            if highlight:
                highlights.append(highlight)
        return highlights

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

    def _extract_live_event_briefs(self, market_context: dict[str, Any], limit: int = 3) -> list[dict[str, str]]:
        live_events = market_context.get("liveMarketEvents") or []
        if not isinstance(live_events, list):
            return []

        cluster = self._build_high_impact_live_event_cluster(market_context) or {}
        cluster_date = self._normalize_text(cluster.get("date"))
        candidates: list[dict[str, Any]] = []
        for index, item in enumerate(live_events):
            if not isinstance(item, dict):
                continue
            title = self._normalize_text(item.get("eventTitle"))
            summary = self._normalize_text(item.get("eventSummary")) or title
            occurred_at = self._normalize_text(item.get("occurredAt"))
            impact_level = self._normalize_text(item.get("impactLevel"))
            source_url = self._normalize_text(item.get("sourceUrl"))
            if not title and not summary:
                continue
            candidates.append({
                "title": title,
                "summary": summary,
                "occurredAt": occurred_at,
                "impactLevel": impact_level,
                "sourceUrl": source_url,
                "_originalIndex": index,
            })
        candidates.sort(
            key=lambda item: (
                -self._live_event_cluster_rank(item, cluster_date),
                -self._live_event_priority_rank(item.get("impactLevel")),
                -self._live_event_title_rank(item.get("title")),
                -self._live_event_time_rank(item.get("occurredAt")),
                item.get("_originalIndex", 0),
            )
        )

        briefs: list[dict[str, str]] = []
        for item in candidates[:limit]:
            briefs.append({
                "title": self._normalize_text(item.get("title")),
                "summary": self._normalize_text(item.get("summary")),
                "occurredAt": self._normalize_text(item.get("occurredAt")),
                "impactLevel": self._normalize_text(item.get("impactLevel")),
                "sourceUrl": self._normalize_text(item.get("sourceUrl")),
            })
        return briefs

    def _extract_latest_live_event(self, market_context: dict[str, Any]) -> dict[str, str] | None:
        live_events = market_context.get("liveMarketEvents") or []
        if not isinstance(live_events, list):
            return None

        latest_event: dict[str, str] | None = None
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

    def _extract_priority_live_event(self, market_context: dict[str, Any]) -> dict[str, str] | None:
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
            occurred_at = self._normalize_text(item.get("occurredAt"))
            source_url = self._normalize_text(item.get("sourceUrl"))
            if not title and not summary:
                continue
            grouped.setdefault(date_label, []).append({
                "title": title,
                "summary": summary,
                "occurredAt": occurred_at,
                "sourceUrl": source_url,
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

    def _extract_live_event_match_tokens(self, value: Any) -> list[str]:
        normalized = self._normalize_text(value).replace(" ", "")
        if not normalized:
            return []

        token_lengths = [len(normalized)]
        if len(normalized) > 8:
            token_lengths.extend([8, 12])

        tokens: list[str] = []
        for length in token_lengths:
            if length <= 0 or length > len(normalized):
                continue
            tokens.append(normalized[:length])
            tokens.append(normalized[-length:])
        tokens.extend(self._extract_live_event_title_keywords(normalized))
        return self._dedupe_text_list(tokens)

    def _extract_live_event_cluster_match_tokens(self, value: Any) -> list[str]:
        normalized = self._normalize_text(value).replace(" ", "")
        if not normalized:
            return []

        token_lengths = [len(normalized)]
        if len(normalized) > 12:
            token_lengths.extend([12, 16])
        elif len(normalized) > 8:
            token_lengths.append(8)

        tokens: list[str] = []
        for length in token_lengths:
            if length <= 0 or length > len(normalized):
                continue
            tokens.append(normalized[-length:])
        tokens.extend(self._extract_live_event_title_keywords(normalized))
        return self._dedupe_text_list(tokens)

    def _extract_live_event_date_match_tokens(self, value: Any) -> list[str]:
        date_label = self._extract_live_event_date_label(value)
        if not date_label:
            return []

        year_text, month_text, day_text = date_label.split("-")
        month_value = int(month_text)
        day_value = int(day_text)
        return self._dedupe_text_list([
            date_label,
            f"{year_text}年{month_value}月{day_value}日",
            f"{month_value}月{day_value}日",
        ])

    def _extract_live_event_cluster_count_tokens(self, count: int) -> list[str]:
        if count <= 0:
            return []

        chinese_count_map = {
            1: "一",
            2: "两",
            3: "三",
            4: "四",
            5: "五",
            6: "六",
            7: "七",
            8: "八",
            9: "九",
            10: "十",
        }
        chinese_count = chinese_count_map.get(count, "")
        tokens = [
            f"{count}份高影响公告",
            f"{count}项高影响公告",
        ]
        if chinese_count:
            tokens.extend([
                f"{chinese_count}份高影响公告",
                f"{chinese_count}项高影响公告",
            ])
        return self._dedupe_text_list(tokens)

    def _extract_live_event_title_keywords(self, value: Any) -> list[str]:
        normalized = self._normalize_text(value).replace(" ", "")
        if not normalized:
            return []

        keywords: list[str] = []
        for _, keyword in self._ranked_live_event_title_keywords():
            normalized_keyword = self._normalize_text(keyword).replace(" ", "")
            if normalized_keyword and normalized_keyword in normalized:
                keywords.append(normalized_keyword)
        return self._dedupe_text_list(keywords)

    def _find_live_event_evidence(
        self,
        live_event: dict[str, Any],
        evidence_items: list[dict[str, Any]],
    ) -> dict[str, Any]:
        if not live_event:
            return {}

        live_event_title = self._normalize_text(live_event.get("title"))
        live_event_url = self._normalize_text(live_event.get("sourceUrl"))
        live_event_time = self._normalize_text(live_event.get("occurredAt"))
        title_time_match: dict[str, Any] = {}
        title_match: dict[str, Any] = {}
        for item in evidence_items:
            if self._normalize_text(item.get("evidenceType")) != "LIVE_MARKET_EVENT":
                continue
            evidence_url = self._normalize_text(item.get("url"))
            if live_event_url and evidence_url and live_event_url == evidence_url:
                return item
            evidence_title = self._normalize_text(item.get("title"))
            evidence_time = self._normalize_text(item.get("occurredAt"))
            if (
                live_event_title
                and live_event_time
                and live_event_title == evidence_title
                and live_event_time == evidence_time
            ):
                title_time_match = item
            if live_event_title and live_event_title == evidence_title and not title_match:
                title_match = item
        return title_time_match or title_match

    def _resolve_live_event_evidence_status(
        self,
        live_event: dict[str, Any],
        evidence_item: dict[str, Any],
    ) -> str:
        if not live_event:
            return "NOT_APPLICABLE"
        if self._normalize_text(evidence_item.get("evidenceId")):
            return "MATCHED"
        return "MISSING"

    def _extract_report_summary(self, report: Any) -> str:
        if not isinstance(report, dict):
            return ""
        for key in ("displaySummary", "summary", "originalSummary"):
            value = self._normalize_text(report.get(key))
            if value:
                return value
        return ""

    def _normalize_text_list(self, value: Any) -> list[str]:
        if not isinstance(value, list):
            return []
        normalized: list[str] = []
        for item in value:
            text = self._normalize_text(item)
            if text:
                normalized.append(text)
        return normalized

    def _extract_first_text(self, value: Any) -> str:
        if isinstance(value, list):
            for item in value:
                text = self._normalize_text(item)
                if text:
                    return text
        return self._normalize_text(value)

    def _extract_first_live_event_detail(self, events: Any, highlights: Any) -> str:
        if isinstance(events, list):
            for item in events:
                if not isinstance(item, dict):
                    continue
                occurred_at = self._normalize_text(item.get("occurredAt"))
                title = self._normalize_text(item.get("eventTitle") or item.get("title"))
                summary = self._normalize_text(item.get("eventSummary") or item.get("summary"))
                impact_level = self._normalize_text(item.get("impactLevel"))
                parts = [part for part in (occurred_at, title, summary, impact_level) if part]
                if parts:
                    return " / ".join(parts)
        return self._extract_first_text(highlights)

    def _normalize_text(self, value: Any) -> str:
        if value is None:
            return ""
        return str(value).strip()

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
        for score, keyword in self._ranked_live_event_title_keywords():
            if keyword in normalized:
                return score
        return 0

    def _ranked_live_event_title_keywords(self) -> list[tuple[int, str]]:
        return [
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

    def _join_title_list(self, titles: list[str]) -> str:
        normalized_titles = [self._normalize_text(title) for title in titles if self._normalize_text(title)]
        if not normalized_titles:
            return ""
        quoted_titles = [f"《{title}》" for title in normalized_titles]
        return "、".join(quoted_titles)

    def _normalize_evidence_items(self, value: Any) -> list[dict[str, Any]]:
        if not isinstance(value, list):
            return []
        normalized_items: list[dict[str, Any]] = []
        for item in value:
            if not isinstance(item, dict):
                continue
            normalized_item = {
                "evidenceId": self._normalize_text(item.get("evidenceId")),
                "evidenceType": self._normalize_text(item.get("evidenceType")),
                "source": self._normalize_text(item.get("source")),
                "title": self._normalize_text(item.get("title")),
                "summary": self._normalize_text(item.get("summary")),
                "url": self._normalize_text(item.get("url")),
                "occurredAt": self._normalize_text(item.get("occurredAt")),
                "referenceId": self._normalize_text(item.get("referenceId")),
                "relevance": self._normalize_text(item.get("relevance")),
            }
            if (
                normalized_item["evidenceId"]
                or normalized_item["title"]
                or normalized_item["summary"]
            ):
                normalized_items.append(normalized_item)
        return normalized_items

    def _resolve_text_list(self, preferred: Any, fallback: list[str]) -> list[str]:
        normalized = self._normalize_text_list(preferred)
        return normalized or fallback

    def _resolve_bool(self, preferred: Any, fallback: bool) -> bool:
        if isinstance(preferred, bool):
            return preferred
        if isinstance(preferred, str):
            normalized = preferred.strip().lower()
            if normalized in {"true", "1", "yes"}:
                return True
            if normalized in {"false", "0", "no"}:
                return False
        return fallback

    def _resolve_confidence_score(self, preferred: Any, fallback: float) -> float:
        if isinstance(preferred, (int, float)):
            return round(max(0.0, min(1.0, float(preferred))), 4)
        if isinstance(preferred, str):
            try:
                return round(max(0.0, min(1.0, float(preferred.strip()))), 4)
            except ValueError:
                return fallback
        return fallback
