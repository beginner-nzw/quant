import time
from typing import Any

from app.services.market_data_service import MarketDataService
from app.services.task_control_service import TaskControlService


class EvidenceCollectionAgent:
    def __init__(self):
        self.market_data_service = MarketDataService()
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        state["current_stage"] = "EVIDENCE_COLLECTION"
        state["current_node"] = "evidence_collection_agent"
        state["progress"] = 55

        market_context = self._ensure_market_context(state)
        evidence_items = self._build_evidence_items(
            source_context=state.get("source_context") or {},
            task_context=state.get("task_context") or {},
            source_task_context=state.get("source_task_context") or {},
            market_context=market_context,
        )
        evidence_refs = self._build_evidence_refs(evidence_items, market_context)

        state["market_context"] = market_context
        state["evidence_items"] = evidence_items
        state["evidence_refs"] = evidence_refs
        state.setdefault("agent_audits", []).append(
            {
                "executionId": f"exec-{state['task_id']}-evidence",
                "agentCode": "evidence_collection_agent",
                "agentName": "Evidence Collection Agent",
                "nodeCode": "evidence_collection_agent",
                "status": "SUCCESS",
                "confidenceScore": 0.9 if evidence_items else 0.82,
                "needHumanReview": False,
                "startTimestamp": now,
                "finishTimestamp": now,
                "durationMs": 0,
            }
        )
        return state

    def _ensure_market_context(self, state: dict[str, Any]) -> dict[str, Any]:
        market_context = state.get("market_context") or {}
        if market_context.get("dataSource"):
            return market_context

        return self.market_data_service.load_financial_data(
            target_code=state.get("target_code") or "",
            target_name=state.get("target_name") or "",
            target_type=state.get("target_type") or "",
            trace_id=state.get("trace_id", ""),
        )

    def _build_evidence_items(
        self,
        *,
        source_context: dict[str, Any],
        task_context: dict[str, Any],
        source_task_context: dict[str, Any],
        market_context: dict[str, Any],
    ) -> list[dict[str, Any]]:
        items: list[dict[str, Any]] = []

        source_event = task_context.get("sourceEvent") or {}
        source_event_id = self._normalize_text(
            source_event.get("eventId") or source_context.get("sourceEventId")
        )
        source_event_title = self._normalize_text(
            source_event.get("eventTitle") or source_context.get("sourceEventTitle")
        )
        source_event_summary = self._normalize_text(
            source_event.get("eventSummary") or source_context.get("sourceEventSummary")
        )
        if source_event_id or source_event_title or source_event_summary:
            items.append(
                self._build_item(
                    evidence_id=f"source-event:{source_event_id or 'unknown'}",
                    evidence_type="SOURCE_EVENT",
                    source=self._normalize_text(
                        source_event.get("sourceChannel")
                        or source_context.get("sourceEventSourceChannel")
                        or "market-event"
                    ),
                    title=source_event_title or "来源市场事件",
                    summary=source_event_summary or "当前任务关联了来源市场事件。",
                    url=self._normalize_text(source_event.get("sourceUrl")),
                    occurred_at=self._normalize_text(
                        source_event.get("occurredAt") or source_context.get("sourceEventOccurredAt")
                    ),
                    reference_id=source_event_id,
                    relevance="HIGH",
                )
            )

        for index, market_event in enumerate((market_context.get("recentMarketEvents") or [])[:3], start=1):
            if not isinstance(market_event, dict):
                continue
            event_id = self._normalize_text(market_event.get("eventId"))
            event_title = self._normalize_text(market_event.get("eventTitle"))
            event_summary = self._normalize_text(market_event.get("eventSummary"))
            if not event_id and not event_title and not event_summary:
                continue
            items.append(
                self._build_item(
                    evidence_id=f"market-event:{event_id or index}",
                    evidence_type="MARKET_EVENT",
                    source=self._first_text(
                        market_event.get("sourceChannel"),
                        "market-event-sync",
                    ),
                    title=event_title or f"近期市场事件 {index}",
                    summary=event_summary or event_title or "已同步到平台的真实市场事件。",
                    url=self._normalize_text(market_event.get("sourceUrl")),
                    occurred_at=self._first_text(
                        market_event.get("occurredAt"),
                        market_event.get("createdAt"),
                    ),
                    reference_id=event_id,
                    relevance=self._resolve_event_relevance(market_event.get("impactLevel")),
                )
            )

        live_source_code = self._normalize_text(market_context.get("liveMarketEventSourceCode"))
        live_source_name = self._normalize_text(market_context.get("liveMarketEventSourceName"))
        for index, market_event in enumerate(self._rank_live_market_events(market_context), start=1):
            if not isinstance(market_event, dict):
                continue
            event_title = self._normalize_text(market_event.get("eventTitle"))
            event_summary = self._normalize_text(market_event.get("eventSummary"))
            if not event_title and not event_summary:
                continue
            original_index = market_event.get("_originalIndex")
            fallback_index = original_index + 1 if isinstance(original_index, int) else index
            event_source_code = self._first_text(
                market_event.get("sourceCode"),
                live_source_code,
            )
            event_source_name = self._first_text(
                market_event.get("sourceName"),
                market_event.get("sourceChannel"),
                live_source_name,
            )
            reference_id = self._first_text(
                self._normalize_text(market_event.get("eventId")),
                f"{event_source_code or 'preview'}-{fallback_index}",
            )
            evidence_type = self._resolve_live_event_evidence_type(market_event)
            evidence_id_prefix = self._resolve_live_event_evidence_id_prefix(evidence_type)
            items.append(
                self._build_item(
                    evidence_id=f"{evidence_id_prefix}:{reference_id}",
                    evidence_type=evidence_type,
                    source=self._first_text(
                        market_event.get("sourceChannel"),
                        event_source_name,
                        event_source_code,
                        "live-event-preview",
                    ),
                    title=event_title or f"实时市场事件 {index}",
                    summary=event_summary or event_title or "实时事件源预览返回的市场事件。",
                    url=self._normalize_text(market_event.get("sourceUrl")),
                    occurred_at=self._normalize_text(market_event.get("occurredAt")),
                    reference_id=reference_id,
                    relevance=self._resolve_event_relevance(market_event.get("impactLevel")),
                )
            )

        source_task_detail = source_task_context.get("taskDetail") or {}
        source_report = source_task_context.get("report") or {}
        source_report_id = self._normalize_text(
            source_report.get("reportId") or source_context.get("sourceReportId")
        )
        source_report_summary = self._first_text(
            source_report.get("displaySummary"),
            source_report.get("summary"),
            source_report.get("originalSummary"),
        )
        if source_report_id or source_report_summary:
            items.append(
                self._build_item(
                    evidence_id=f"source-report:{source_report_id or 'unknown'}",
                    evidence_type="SOURCE_REPORT",
                    source="task-full-api",
                    title=self._first_text(
                        source_task_detail.get("taskTitle"),
                        source_report.get("reportType"),
                        "来源任务报告",
                    ),
                    summary=source_report_summary or "当前任务引用了来源任务报告。",
                    url="",
                    occurred_at=self._first_text(
                        source_task_detail.get("finishTime"),
                        source_task_detail.get("createdAt"),
                    ),
                    reference_id=source_report_id,
                    relevance="HIGH",
                )
            )

        latest_insight_report_id = self._normalize_text(market_context.get("latestInsightReportId"))
        latest_insight_summary = self._normalize_text(market_context.get("latestInsightSummary"))
        if latest_insight_report_id or latest_insight_summary:
            items.append(
                self._build_item(
                    evidence_id=f"latest-insight:{latest_insight_report_id or 'unknown'}",
                    evidence_type="LATEST_INSIGHT",
                    source=self._normalize_text(market_context.get("dataSource")) or "research-workbench",
                    title="最新洞察报告",
                    summary=latest_insight_summary or "研究工作台存在同标的最新洞察报告。",
                    url="",
                    occurred_at="",
                    reference_id=latest_insight_report_id,
                    relevance="MEDIUM",
                )
            )

        for index, risk_warning in enumerate((market_context.get("riskWarnings") or [])[:3], start=1):
            if not isinstance(risk_warning, dict):
                continue
            task_id = self._normalize_text(risk_warning.get("taskId"))
            summary = self._first_text(
                risk_warning.get("summary"),
                self._join_list(risk_warning.get("riskReasons")),
            )
            if not task_id and not summary:
                continue
            items.append(
                self._build_item(
                    evidence_id=f"risk-warning:{task_id or index}",
                    evidence_type="RISK_WARNING",
                    source="risk-warning-center",
                    title=self._first_text(
                        risk_warning.get("taskTitle"),
                        f"风险预警 {index}",
                    ),
                    summary=summary or "平台存在同标的风险预警记录。",
                    url="",
                    occurred_at=self._first_text(
                        risk_warning.get("createdAt"),
                        risk_warning.get("reportReviewedAt"),
                    ),
                    reference_id=task_id,
                    relevance=self._resolve_event_relevance(risk_warning.get("riskLevel")),
                )
            )

        for index, strategy_signal in enumerate((market_context.get("strategySignals") or [])[:3], start=1):
            if not isinstance(strategy_signal, dict):
                continue
            task_id = self._normalize_text(strategy_signal.get("taskId"))
            summary = self._first_text(
                strategy_signal.get("strategySummary"),
                strategy_signal.get("backtestSummary"),
                self._join_list(strategy_signal.get("signalSources")),
            )
            if not task_id and not summary:
                continue
            items.append(
                self._build_item(
                    evidence_id=f"strategy-signal:{task_id or index}",
                    evidence_type="STRATEGY_SIGNAL",
                    source="strategy-signal-center",
                    title=self._first_text(
                        strategy_signal.get("taskTitle"),
                        f"策略信号 {index}",
                    ),
                    summary=summary or "平台存在同标的策略信号记录。",
                    url="",
                    occurred_at=self._first_text(
                        strategy_signal.get("createdAt"),
                        strategy_signal.get("reportReviewedAt"),
                    ),
                    reference_id=task_id,
                    relevance=self._resolve_signal_relevance(strategy_signal.get("signalStrength")),
                )
            )

        for index, intelligence in enumerate((market_context.get("marketIntelligence") or [])[:3], start=1):
            if not isinstance(intelligence, dict):
                continue
            task_id = self._normalize_text(intelligence.get("taskId"))
            summary = self._first_text(
                intelligence.get("summary"),
                self._join_list(intelligence.get("intelligenceSourceTags")),
            )
            if not task_id and not summary:
                continue
            items.append(
                self._build_item(
                    evidence_id=f"market-intelligence:{task_id or index}",
                    evidence_type="MARKET_INTELLIGENCE",
                    source=self._first_text(
                        intelligence.get("sourceChannel"),
                        "market-intelligence-center",
                    ),
                    title=self._first_text(
                        intelligence.get("taskTitle"),
                        intelligence.get("intelligenceType"),
                        f"市场情报 {index}",
                    ),
                    summary=summary or "平台存在同标的市场情报记录。",
                    url="",
                    occurred_at=self._first_text(
                        intelligence.get("createdAt"),
                        intelligence.get("reviewedAt"),
                    ),
                    reference_id=task_id,
                    relevance=self._resolve_event_relevance(intelligence.get("riskLevel")),
                )
            )

        for index, recent_task in enumerate((market_context.get("recentTasks") or [])[:3], start=1):
            if not isinstance(recent_task, dict):
                continue
            task_id = self._normalize_text(recent_task.get("taskId"))
            task_summary = self._join_parts(
                recent_task.get("taskType"),
                recent_task.get("status"),
                recent_task.get("summary"),
            )
            if not task_id and not task_summary:
                continue
            items.append(
                self._build_item(
                    evidence_id=f"recent-task:{task_id or index}",
                    evidence_type="RECENT_TASK",
                    source=self._normalize_text(market_context.get("dataSource")) or "research-workbench",
                    title=self._first_text(
                        recent_task.get("taskTitle"),
                        recent_task.get("taskType"),
                        f"近期任务 {index}",
                    ),
                    summary=task_summary or "研究工作台存在近期同标的任务。",
                    url="",
                    occurred_at=self._first_text(
                        recent_task.get("finishTime"),
                        recent_task.get("createdAt"),
                    ),
                    reference_id=task_id,
                    relevance="LOW",
                )
            )

        deduplicated: list[dict[str, Any]] = []
        seen_ids: set[str] = set()
        seen_content: set[str] = set()
        for item in items:
            evidence_id = self._normalize_text(item.get("evidenceId"))
            content_key = self._join_parts(
                item.get("evidenceType"),
                item.get("source"),
                item.get("title"),
                item.get("summary"),
                item.get("url"),
                item.get("occurredAt"),
            ).lower()
            if evidence_id and evidence_id in seen_ids:
                continue
            if content_key and content_key in seen_content:
                continue
            if evidence_id:
                seen_ids.add(evidence_id)
            if content_key:
                seen_content.add(content_key)
            deduplicated.append(item)
        return deduplicated

    def _build_evidence_refs(
        self,
        evidence_items: list[dict[str, Any]],
        market_context: dict[str, Any],
    ) -> list[str]:
        refs: list[str] = []

        if market_context.get("dataSource"):
            refs.append(f"marketData:{market_context['dataSource']}")
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
        if market_context.get("policyLiveEventCount") is not None:
            refs.append(f"policyLiveEventCount:{market_context['policyLiveEventCount']}")
        if market_context.get("regulatoryRiskLiveEventCount") is not None:
            refs.append(f"regulatoryRiskLiveEventCount:{market_context['regulatoryRiskLiveEventCount']}")

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
            elif evidence_type == "POLICY_LIVE_EVENT" and reference_id:
                refs.append(f"policyLiveEvent:{reference_id}")
            elif evidence_type == "REGULATORY_RISK_LIVE_EVENT" and reference_id:
                refs.append(f"regulatoryRiskLiveEvent:{reference_id}")
            elif evidence_type == "SOURCE_REPORT" and reference_id:
                refs.append(f"sourceTaskReport:{reference_id}")
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
        seen: set[str] = set()
        for ref in refs:
            normalized = self._normalize_text(ref)
            if not normalized or normalized in seen:
                continue
            seen.add(normalized)
            deduplicated.append(normalized)
        return deduplicated

    def _build_item(
        self,
        *,
        evidence_id: str,
        evidence_type: str,
        source: str,
        title: str,
        summary: str,
        url: str,
        occurred_at: str,
        reference_id: str,
        relevance: str,
    ) -> dict[str, Any]:
        return {
            "evidenceId": evidence_id,
            "evidenceType": evidence_type,
            "source": source,
            "title": title,
            "summary": summary,
            "url": url,
            "occurredAt": occurred_at,
            "referenceId": reference_id,
            "relevance": relevance,
        }

    def _first_text(self, *values: Any) -> str:
        for value in values:
            text = self._normalize_text(value)
            if text:
                return text
        return ""

    def _join_parts(self, *values: Any) -> str:
        parts = [self._normalize_text(value) for value in values]
        return " / ".join(part for part in parts if part)

    def _join_list(self, value: Any) -> str:
        if not isinstance(value, list):
            return ""
        parts = [self._normalize_text(item) for item in value]
        return "；".join(part for part in parts if part)

    def _resolve_event_relevance(self, impact_level: Any) -> str:
        normalized = self._normalize_text(impact_level).upper()
        if normalized == "HIGH":
            return "HIGH"
        if normalized == "LOW":
            return "LOW"
        return "MEDIUM"

    def _resolve_signal_relevance(self, signal_strength: Any) -> str:
        normalized = self._normalize_text(signal_strength).upper()
        if normalized in {"STRONG", "HIGH"}:
            return "HIGH"
        if normalized in {"WEAK", "LOW"}:
            return "LOW"
        return "MEDIUM"

    def _rank_live_market_events(
        self,
        market_context: dict[str, Any],
        limit: int = 3,
    ) -> list[dict[str, Any]]:
        live_events = market_context.get("liveMarketEvents") or []
        if not isinstance(live_events, list):
            return []

        cluster_date = self._build_high_impact_live_event_cluster_date(market_context)
        candidates: list[dict[str, Any]] = []
        for index, item in enumerate(live_events):
            if not isinstance(item, dict):
                continue
            title = self._normalize_text(item.get("eventTitle"))
            summary = self._normalize_text(item.get("eventSummary"))
            if not title and not summary:
                continue
            candidate = dict(item)
            candidate["_originalIndex"] = index
            candidates.append(candidate)

        candidates.sort(
            key=lambda item: (
                -self._live_event_focus_rank(item),
                -self._live_event_cluster_rank(item, cluster_date),
                -self._live_event_priority_rank(item.get("impactLevel")),
                -self._live_event_title_rank(item.get("eventTitle")),
                -self._live_event_time_rank(item.get("occurredAt")),
                item.get("_originalIndex", 0),
            )
        )
        return candidates[:limit]

    def _resolve_live_event_evidence_type(self, live_event: dict[str, Any]) -> str:
        if self._is_regulatory_risk_live_event(live_event):
            return "REGULATORY_RISK_LIVE_EVENT"
        if self._is_policy_live_event(live_event):
            return "POLICY_LIVE_EVENT"
        return "LIVE_MARKET_EVENT"

    def _resolve_live_event_evidence_id_prefix(self, evidence_type: str) -> str:
        if evidence_type == "REGULATORY_RISK_LIVE_EVENT":
            return "regulatory-risk-live-event"
        if evidence_type == "POLICY_LIVE_EVENT":
            return "policy-live-event"
        return "live-market-event"

    def _live_event_focus_rank(self, live_event: dict[str, Any]) -> int:
        if self._is_regulatory_risk_live_event(live_event):
            return 3
        if self._is_policy_live_event(live_event):
            return 2
        return 0

    def _build_high_impact_live_event_cluster_date(self, market_context: dict[str, Any]) -> str:
        live_events = market_context.get("liveMarketEvents") or []
        if not isinstance(live_events, list):
            return ""

        grouped: dict[str, int] = {}
        for item in live_events:
            if not isinstance(item, dict):
                continue
            if self._normalize_text(item.get("impactLevel")).upper() != "HIGH":
                continue
            date_label = self._extract_live_event_date_label(item.get("occurredAt"))
            if not date_label:
                continue
            title = self._normalize_text(item.get("eventTitle"))
            summary = self._normalize_text(item.get("eventSummary"))
            if not title and not summary:
                continue
            grouped[date_label] = grouped.get(date_label, 0) + 1

        best_date = ""
        best_count = 0
        for date_label, count in grouped.items():
            if count < 2:
                continue
            if count > best_count or (count == best_count and date_label > best_date):
                best_date = date_label
                best_count = count
        return best_date

    def _live_event_cluster_rank(self, live_event: dict[str, Any], cluster_date: str) -> int:
        if not cluster_date:
            return 0
        if self._normalize_text(live_event.get("impactLevel")).upper() != "HIGH":
            return 0
        if self._extract_live_event_date_label(live_event.get("occurredAt")) != cluster_date:
            return 0
        return 1

    def _live_event_priority_rank(self, impact_level: Any) -> int:
        normalized = self._normalize_text(impact_level).upper()
        if normalized == "HIGH":
            return 3
        if normalized == "MEDIUM":
            return 2
        if normalized == "LOW":
            return 1
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

    def _live_event_time_rank(self, occurred_at: Any) -> int:
        digits = "".join(char for char in self._normalize_text(occurred_at) if char.isdigit())
        if not digits:
            return 0
        try:
            return int(digits)
        except ValueError:
            return 0

    def _extract_live_event_date_label(self, occurred_at: Any) -> str:
        digits = "".join(char for char in self._normalize_text(occurred_at) if char.isdigit())
        if len(digits) >= 8:
            return f"{digits[:4]}-{digits[4:6]}-{digits[6:8]}"
        return ""

    def _is_policy_live_event(self, live_event: dict[str, Any]) -> bool:
        source_code = self._normalize_text(live_event.get("sourceCode")).upper()
        source_category = self._normalize_text(live_event.get("sourceCategory")).upper()
        source_channel = self._normalize_text(live_event.get("sourceChannel")).upper()
        event_type = self._normalize_text(live_event.get("eventType")).upper()
        if source_code == "POLICY_TRACKER":
            return True
        if "POLICY" in {source_category, source_channel, event_type}:
            return True

        searchable_text = self._join_parts(
            live_event.get("eventTitle"),
            live_event.get("eventSummary"),
            live_event.get("sourceName"),
            live_event.get("sourceChannel"),
        )
        policy_keywords = [
            "政策",
            "国务院",
            "中国政府网",
            "发改委",
            "财政部",
            "人民银行",
        ]
        return any(keyword in searchable_text for keyword in policy_keywords)

    def _is_regulatory_risk_live_event(self, live_event: dict[str, Any]) -> bool:
        source_code = self._normalize_text(live_event.get("sourceCode")).upper()
        source_category = self._normalize_text(live_event.get("sourceCategory")).upper()
        source_channel = self._normalize_text(live_event.get("sourceChannel")).upper()
        event_type = self._normalize_text(live_event.get("eventType")).upper()
        if source_code == "RISK_MONITOR":
            return True
        if source_category == "RISK" or source_channel == "RISK_MONITOR":
            return True
        if event_type in {"RISK_ALERT", "REGULATORY_RISK"}:
            return True

        searchable_text = self._join_parts(
            live_event.get("eventTitle"),
            live_event.get("eventSummary"),
            live_event.get("sourceName"),
            live_event.get("sourceChannel"),
        )
        risk_keywords = [
            "证监会",
            "监管风险",
            "行政处罚",
            "市场禁入",
            "立案调查",
            "监管措施",
            "纪律处分",
            "警示函",
            "责令改正",
        ]
        return any(keyword in searchable_text for keyword in risk_keywords)

    def _normalize_text(self, value: Any) -> str:
        if value is None:
            return ""
        return str(value).strip()
