from typing import Any

from app.clients.backend_client import BackendApiClient
from app.utils.logger import log_error


class MarketDataService:
    def __init__(self):
        self.backend_client = BackendApiClient()

    def load_financial_data(
        self,
        target_code: str,
        target_name: str = "",
        target_type: str = "",
        trace_id: str = "",
    ) -> dict[str, Any]:
        fallback = self._build_fallback_snapshot(target_code, target_name)
        live_event_preview = self._load_live_market_event_preview(
            target_code=target_code,
            target_name=target_name,
            target_type=target_type,
            trace_id=trace_id,
        )
        live_event_focus = self._build_live_event_focus(
            live_event_preview.get("liveMarketEvents") or []
        )
        fallback.update(live_event_preview)
        fallback.update(live_event_focus)
        recent_market_events = self._load_recent_market_events(
            target_code=target_code,
            target_name=target_name,
            trace_id=trace_id,
        )
        risk_warnings = self._load_risk_warnings(
            target_code=target_code,
            target_name=target_name,
            trace_id=trace_id,
        )
        strategy_signals = self._load_strategy_signals(
            target_code=target_code,
            target_name=target_name,
            trace_id=trace_id,
        )
        market_intelligence = self._load_market_intelligence(
            target_code=target_code,
            target_name=target_name,
            trace_id=trace_id,
        )
        fallback["recentMarketEvents"] = recent_market_events
        fallback["recentEventCount"] = len(recent_market_events)
        fallback["riskWarnings"] = risk_warnings
        fallback["riskWarningCount"] = len(risk_warnings)
        fallback["latestRiskWarningSummary"] = self._extract_first_text(
            risk_warnings,
            "summary",
        )
        fallback["strategySignals"] = strategy_signals
        fallback["strategySignalCount"] = len(strategy_signals)
        fallback["latestStrategySignalSummary"] = self._extract_first_text(
            strategy_signals,
            "strategySummary",
            "backtestSummary",
        )
        fallback["marketIntelligence"] = market_intelligence
        fallback["marketIntelligenceCount"] = len(market_intelligence)
        fallback["latestMarketIntelligenceSummary"] = self._extract_first_text(
            market_intelligence,
            "summary",
        )
        if not target_code:
            return fallback

        try:
            workbench = self.backend_client.get_research_workbench(
                target_code=target_code,
                target_name=target_name or None,
                trace_id=trace_id or None,
            )
        except Exception as exc:
            log_error(trace_id, f"[AI-ENGINE][MARKET-DATA] load failed target={target_code} err={exc}")
            return fallback

        if not workbench:
            return fallback

        latest_insight = workbench.get("latestInsight") or {}
        signal_direction = str(latest_insight.get("signalDirection") or "").upper()
        risk_level = str(latest_insight.get("riskLevel") or "").upper()
        success_task_count = int(workbench.get("successTaskCount") or 0)
        failed_task_count = int(workbench.get("failedTaskCount") or 0)
        pending_review_count = int(workbench.get("pendingReviewCount") or 0)

        return {
            "targetCode": target_code,
            "targetName": workbench.get("targetName") or target_name,
            "revenueTrend": self._resolve_revenue_trend(signal_direction, success_task_count, failed_task_count),
            "profitTrend": self._resolve_profit_trend(signal_direction, latest_insight.get("confidenceScore")),
            "cashflowSignal": self._resolve_cashflow_signal(risk_level, pending_review_count, failed_task_count),
            "dataSource": "research-workbench",
            "reportCount": int(workbench.get("reportCount") or 0),
            "taskCount": int(workbench.get("taskCount") or 0),
            "activeTaskCount": int(workbench.get("activeTaskCount") or 0),
            "successTaskCount": success_task_count,
            "failedTaskCount": failed_task_count,
            "pendingReviewCount": pending_review_count,
            "latestInsightReportId": latest_insight.get("reportId") or "",
            "latestInsightReportType": latest_insight.get("reportType") or "",
            "latestInsightReviewStatus": latest_insight.get("reviewStatus") or "",
            "latestInsightConfidenceScore": latest_insight.get("confidenceScore"),
            "latestInsightSummary": latest_insight.get("summary") or "",
            "latestHighlights": latest_insight.get("highlights") or [],
            "latestRiskPoints": latest_insight.get("riskPoints") or [],
            "recentTasks": workbench.get("recentTasks") or [],
            "recentMarketEvents": recent_market_events,
            "recentEventCount": len(recent_market_events),
            "riskWarnings": risk_warnings,
            "riskWarningCount": len(risk_warnings),
            "latestRiskWarningSummary": self._extract_first_text(
                risk_warnings,
                "summary",
            ),
            "strategySignals": strategy_signals,
            "strategySignalCount": len(strategy_signals),
            "latestStrategySignalSummary": self._extract_first_text(
                strategy_signals,
                "strategySummary",
                "backtestSummary",
            ),
            "marketIntelligence": market_intelligence,
            "marketIntelligenceCount": len(market_intelligence),
            "latestMarketIntelligenceSummary": self._extract_first_text(
                market_intelligence,
                "summary",
            ),
            "liveMarketEventSourceCode": live_event_preview.get("liveMarketEventSourceCode", ""),
            "liveMarketEventSourceName": live_event_preview.get("liveMarketEventSourceName", ""),
            "liveMarketEventSourceCodes": live_event_preview.get("liveMarketEventSourceCodes") or [],
            "liveMarketEventSourceNames": live_event_preview.get("liveMarketEventSourceNames") or [],
            "liveMarketEventSources": live_event_preview.get("liveMarketEventSources") or [],
            "liveMarketEvents": live_event_preview.get("liveMarketEvents") or [],
            "liveEventCount": int(live_event_preview.get("liveEventCount") or 0),
            "policyLiveEvents": live_event_focus.get("policyLiveEvents") or [],
            "policyLiveEventCount": int(live_event_focus.get("policyLiveEventCount") or 0),
            "policyLiveEventHighlights": live_event_focus.get("policyLiveEventHighlights") or [],
            "regulatoryRiskLiveEvents": live_event_focus.get("regulatoryRiskLiveEvents") or [],
            "regulatoryRiskLiveEventCount": int(live_event_focus.get("regulatoryRiskLiveEventCount") or 0),
            "regulatoryRiskLiveEventHighlights": live_event_focus.get("regulatoryRiskLiveEventHighlights") or [],
            "priorityExternalRiskEventSummary": live_event_focus.get("priorityExternalRiskEventSummary") or "",
        }

    def _build_fallback_snapshot(self, target_code: str, target_name: str) -> dict[str, Any]:
        return {
            "targetCode": target_code,
            "targetName": target_name,
            "revenueTrend": "STABLE",
            "profitTrend": "STABLE",
            "cashflowSignal": "NORMAL",
            "dataSource": "fallback",
            "reportCount": 0,
            "taskCount": 0,
            "activeTaskCount": 0,
            "successTaskCount": 0,
            "failedTaskCount": 0,
            "pendingReviewCount": 0,
            "latestInsightReportId": "",
            "latestInsightReportType": "",
            "latestInsightReviewStatus": "",
            "latestInsightConfidenceScore": None,
            "latestInsightSummary": "",
            "latestHighlights": [],
            "latestRiskPoints": [],
            "recentTasks": [],
            "recentMarketEvents": [],
            "recentEventCount": 0,
            "riskWarnings": [],
            "riskWarningCount": 0,
            "latestRiskWarningSummary": "",
            "strategySignals": [],
            "strategySignalCount": 0,
            "latestStrategySignalSummary": "",
            "marketIntelligence": [],
            "marketIntelligenceCount": 0,
            "latestMarketIntelligenceSummary": "",
            "liveMarketEventSourceCode": "",
            "liveMarketEventSourceName": "",
            "liveMarketEventSourceCodes": [],
            "liveMarketEventSourceNames": [],
            "liveMarketEventSources": [],
            "liveMarketEvents": [],
            "liveEventCount": 0,
            "policyLiveEvents": [],
            "policyLiveEventCount": 0,
            "policyLiveEventHighlights": [],
            "regulatoryRiskLiveEvents": [],
            "regulatoryRiskLiveEventCount": 0,
            "regulatoryRiskLiveEventHighlights": [],
            "priorityExternalRiskEventSummary": "",
        }

    def _load_recent_market_events(
        self,
        target_code: str,
        target_name: str,
        trace_id: str,
    ) -> list[dict[str, Any]]:
        if not target_code:
            return []
        try:
            return self.backend_client.list_market_events(
                target_code=target_code,
                target_name=target_name or None,
                page_size=3,
                trace_id=trace_id or None,
            )
        except Exception as exc:
            log_error(trace_id, f"[AI-ENGINE][MARKET-EVENTS] load failed target={target_code} err={exc}")
            return []

    def _load_risk_warnings(
        self,
        *,
        target_code: str,
        target_name: str,
        trace_id: str,
    ) -> list[dict[str, Any]]:
        if not target_code:
            return []
        try:
            return self.backend_client.list_risk_warnings(
                target_code=target_code,
                target_name=target_name or None,
                page_size=3,
                trace_id=trace_id or None,
            )
        except Exception as exc:
            log_error(trace_id, f"[AI-ENGINE][RISK-WARNINGS] load failed target={target_code} err={exc}")
            return []

    def _load_strategy_signals(
        self,
        *,
        target_code: str,
        target_name: str,
        trace_id: str,
    ) -> list[dict[str, Any]]:
        if not target_code:
            return []
        try:
            return self.backend_client.list_strategy_signals(
                target_code=target_code,
                target_name=target_name or None,
                page_size=3,
                trace_id=trace_id or None,
            )
        except Exception as exc:
            log_error(trace_id, f"[AI-ENGINE][STRATEGY-SIGNALS] load failed target={target_code} err={exc}")
            return []

    def _load_market_intelligence(
        self,
        *,
        target_code: str,
        target_name: str,
        trace_id: str,
    ) -> list[dict[str, Any]]:
        if not target_code:
            return []
        try:
            return self.backend_client.list_market_intelligence(
                target_code=target_code,
                target_name=target_name or None,
                page_size=3,
                trace_id=trace_id or None,
            )
        except Exception as exc:
            log_error(trace_id, f"[AI-ENGINE][MARKET-INTELLIGENCE] load failed target={target_code} err={exc}")
            return []

    def _load_live_market_event_preview(
        self,
        *,
        target_code: str,
        target_name: str,
        target_type: str,
        trace_id: str,
    ) -> dict[str, Any]:
        empty_result = {
            "liveMarketEventSourceCode": "",
            "liveMarketEventSourceName": "",
            "liveMarketEventSourceCodes": [],
            "liveMarketEventSourceNames": [],
            "liveMarketEventSources": [],
            "liveMarketEvents": [],
            "liveEventCount": 0,
        }
        if not target_code:
            return empty_result

        source_configs = self._resolve_live_event_source_configs(trace_id=trace_id)
        if not source_configs:
            return empty_result

        source_infos: list[dict[str, Any]] = []
        live_items: list[dict[str, Any]] = []
        for source_config in source_configs:
            source_code = self._normalize_text(source_config.get("sourceCode"))
            source_name = self._normalize_text(source_config.get("sourceName"))
            source_category = self._normalize_text(source_config.get("sourceCategory"))
            source_channel = self._normalize_text(source_config.get("sourceChannel"))
            try:
                preview = self.backend_client.preview_market_event_source(
                    source_code=source_code,
                    target_code=target_code,
                    target_name=self._normalize_text(target_name) or target_code,
                    target_type=self._normalize_text(target_type) or None,
                    item_count=3,
                    trace_id=trace_id or None,
                )
            except Exception as exc:
                log_error(trace_id, f"[AI-ENGINE][LIVE-MARKET-EVENTS] preview failed source={source_code} target={target_code} err={exc}")
                source_infos.append({
                    "sourceCode": source_code,
                    "sourceName": source_name,
                    "sourceCategory": source_category,
                    "sourceChannel": source_channel,
                    "itemCount": 0,
                })
                continue

            preview_items = preview.get("items")
            if not isinstance(preview_items, list):
                preview_items = []

            enriched_items: list[dict[str, Any]] = []
            for item in preview_items:
                if not isinstance(item, dict):
                    continue
                enriched_item = dict(item)
                enriched_item["sourceCode"] = self._normalize_text(
                    enriched_item.get("sourceCode")
                ) or self._normalize_text(preview.get("sourceCode")) or source_code
                enriched_item["sourceName"] = self._normalize_text(
                    enriched_item.get("sourceName")
                ) or self._normalize_text(preview.get("sourceName")) or source_name
                enriched_item["sourceCategory"] = self._normalize_text(
                    enriched_item.get("sourceCategory")
                ) or source_category
                enriched_item["sourceChannel"] = self._normalize_text(
                    enriched_item.get("sourceChannel")
                ) or source_channel
                enriched_items.append(enriched_item)

            source_infos.append({
                "sourceCode": self._normalize_text(preview.get("sourceCode")) or source_code,
                "sourceName": self._normalize_text(preview.get("sourceName")) or source_name,
                "sourceCategory": source_category,
                "sourceChannel": source_channel,
                "itemCount": len(enriched_items),
            })
            live_items.extend(enriched_items)

        deduplicated_live_items = self._dedupe_live_market_events(live_items)
        primary_source = next(
            (item for item in source_infos if int(item.get("itemCount") or 0) > 0),
            source_infos[0] if source_infos else {},
        )
        return {
            "liveMarketEventSourceCode": self._normalize_text(primary_source.get("sourceCode")),
            "liveMarketEventSourceName": self._normalize_text(primary_source.get("sourceName")),
            "liveMarketEventSourceCodes": [
                self._normalize_text(item.get("sourceCode"))
                for item in source_infos
                if self._normalize_text(item.get("sourceCode"))
            ],
            "liveMarketEventSourceNames": [
                self._normalize_text(item.get("sourceName"))
                for item in source_infos
                if self._normalize_text(item.get("sourceName"))
            ],
            "liveMarketEventSources": source_infos,
            "liveMarketEvents": deduplicated_live_items,
            "liveEventCount": len(deduplicated_live_items),
        }

    def _resolve_live_event_source_configs(self, *, trace_id: str) -> list[dict[str, Any]]:
        try:
            source_configs = self.backend_client.list_market_event_source_configs(trace_id=trace_id or None)
        except Exception as exc:
            log_error(trace_id, f"[AI-ENGINE][LIVE-MARKET-EVENTS] source config load failed err={exc}")
            return []

        candidates: list[tuple[int, str, dict[str, Any]]] = []
        for item in source_configs:
            if not isinstance(item, dict) or not bool(item.get("enabled")):
                continue

            ingest_mode = self._normalize_text(item.get("ingestMode")).upper()
            source_code = self._normalize_text(item.get("sourceCode"))
            endpoint_url = self._normalize_text(item.get("endpointUrl"))
            upstream_url = self._normalize_text(item.get("upstreamUrl"))
            if ingest_mode == "MOCK":
                continue
            if ingest_mode == "CNINFO_PUBLIC_CRAWLER":
                if not endpoint_url:
                    continue
                candidates.append((0, source_code, item))
                continue
            if ingest_mode == "CNINFO_PROXY":
                if not endpoint_url or not upstream_url:
                    continue
                candidates.append((1, source_code, item))
                continue
            if ingest_mode == "HTTP_JSON":
                if not endpoint_url:
                    continue
                candidates.append((2, source_code, item))
                continue
            if ingest_mode == "RSS_XML":
                if not endpoint_url:
                    continue
                candidates.append((2, source_code, item))
                continue
            if ingest_mode == "GOV_CN_POLICY_HTML":
                if not endpoint_url:
                    continue
                candidates.append((3, source_code, item))
                continue
            if ingest_mode == "CSRC_RISK_HTML":
                if not endpoint_url:
                    continue
                candidates.append((4, source_code, item))
                continue
            if not endpoint_url and not upstream_url:
                continue
            candidates.append((9, source_code, item))

        if not candidates:
            return []

        candidates.sort(key=lambda entry: (entry[0], entry[1]))
        return [entry[2] for entry in candidates]

    def _resolve_revenue_trend(self, signal_direction: str, success_task_count: int, failed_task_count: int) -> str:
        if signal_direction == "POSITIVE":
            return "UP"
        if signal_direction == "NEGATIVE":
            return "DOWN"
        if success_task_count > failed_task_count:
            return "STABLE"
        if failed_task_count > success_task_count:
            return "PRESSURED"
        return "STABLE"

    def _resolve_profit_trend(self, signal_direction: str, confidence_score: Any) -> str:
        try:
            score = float(confidence_score)
        except (TypeError, ValueError):
            score = 0.0

        if signal_direction == "POSITIVE" and score >= 0.8:
            return "UP"
        if signal_direction == "NEGATIVE" and score >= 0.8:
            return "DOWN"
        return "STABLE"

    def _resolve_cashflow_signal(self, risk_level: str, pending_review_count: int, failed_task_count: int) -> str:
        if risk_level == "HIGH":
            return "PRESSURED"
        if pending_review_count > 0 or failed_task_count > 0:
            return "WATCH"
        return "NORMAL"

    def _normalize_text(self, value: Any) -> str:
        if value is None:
            return ""
        return str(value).strip()

    def _extract_first_text(self, items: list[dict[str, Any]], *keys: str) -> str:
        for item in items:
            if not isinstance(item, dict):
                continue
            for key in keys:
                value = self._normalize_text(item.get(key))
                if value:
                    return value
        return ""

    def _dedupe_live_market_events(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        deduplicated: list[dict[str, Any]] = []
        seen: set[str] = set()
        for item in items:
            if not isinstance(item, dict):
                continue
            source_url = self._normalize_text(item.get("sourceUrl")).lower()
            event_title = self._normalize_text(item.get("eventTitle")).lower()
            occurred_at = self._normalize_text(item.get("occurredAt")).lower()
            event_summary = self._normalize_text(item.get("eventSummary")).lower()
            key = source_url or "|".join([event_title, occurred_at, event_summary])
            if key in seen:
                continue
            seen.add(key)
            deduplicated.append(item)
        return deduplicated

    def _build_live_event_focus(self, live_events: list[dict[str, Any]]) -> dict[str, Any]:
        policy_events: list[dict[str, Any]] = []
        regulatory_risk_events: list[dict[str, Any]] = []
        if not isinstance(live_events, list):
            live_events = []

        for item in live_events:
            if not isinstance(item, dict):
                continue
            if self._is_policy_live_event(item):
                policy_events.append(item)
            if self._is_regulatory_risk_live_event(item):
                regulatory_risk_events.append(item)

        policy_highlights = self._build_live_event_highlights(policy_events)
        regulatory_risk_highlights = self._build_live_event_highlights(regulatory_risk_events)
        return {
            "policyLiveEvents": policy_events[:5],
            "policyLiveEventCount": len(policy_events),
            "policyLiveEventHighlights": policy_highlights,
            "regulatoryRiskLiveEvents": regulatory_risk_events[:5],
            "regulatoryRiskLiveEventCount": len(regulatory_risk_events),
            "regulatoryRiskLiveEventHighlights": regulatory_risk_highlights,
            "priorityExternalRiskEventSummary": self._extract_priority_external_event_summary(
                regulatory_risk_events,
                regulatory_risk_highlights,
                policy_events,
                policy_highlights,
            ),
        }

    def _extract_priority_external_event_summary(
        self,
        regulatory_risk_events: list[dict[str, Any]],
        regulatory_risk_highlights: list[str],
        policy_events: list[dict[str, Any]],
        policy_highlights: list[str],
    ) -> str:
        for events in (regulatory_risk_events, policy_events):
            if not isinstance(events, list):
                continue
            for item in events:
                if not isinstance(item, dict):
                    continue
                occurred_at = self._normalize_text(item.get("occurredAt"))
                title = self._normalize_text(item.get("eventTitle"))
                summary = self._normalize_text(item.get("eventSummary"))
                impact_level = self._normalize_text(item.get("impactLevel"))
                parts = [part for part in (occurred_at, title, summary, impact_level) if part]
                if parts:
                    return " / ".join(parts)
        for highlights in (regulatory_risk_highlights, policy_highlights):
            if not isinstance(highlights, list):
                continue
            for item in highlights:
                text = self._normalize_text(item)
                if text:
                    return text
        return ""

    def _is_policy_live_event(self, item: dict[str, Any]) -> bool:
        source_code = self._normalize_text(item.get("sourceCode")).upper()
        source_category = self._normalize_text(item.get("sourceCategory")).upper()
        source_channel = self._normalize_text(item.get("sourceChannel")).upper()
        event_type = self._normalize_text(item.get("eventType")).upper()
        title = self._normalize_text(item.get("eventTitle"))
        summary = self._normalize_text(item.get("eventSummary"))
        combined_text = f"{title} {summary}"
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
        title = self._normalize_text(item.get("eventTitle"))
        summary = self._normalize_text(item.get("eventSummary"))
        combined_text = f"{title} {summary}"
        return (
            source_code == "RISK_MONITOR"
            or source_category == "RISK"
            or source_channel == "RISK_MONITOR"
            or event_type == "RISK_ALERT"
            or "证监会" in combined_text
            or "监管风险" in combined_text
            or "行政处罚" in combined_text
            or "市场禁入" in combined_text
            or "监管措施" in combined_text
        )

    def _build_live_event_highlights(self, items: list[dict[str, Any]]) -> list[str]:
        highlights: list[str] = []
        for item in items[:3]:
            if not isinstance(item, dict):
                continue
            occurred_at = self._normalize_text(item.get("occurredAt"))
            title = self._normalize_text(item.get("eventTitle"))
            summary = self._normalize_text(item.get("eventSummary"))
            impact_level = self._normalize_text(item.get("impactLevel"))
            parts = [
                part
                for part in (occurred_at, title or summary, impact_level)
                if part
            ]
            if parts:
                highlights.append(" / ".join(parts))
        return highlights
