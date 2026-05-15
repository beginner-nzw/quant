from typing import Any

import httpx

from app.config.settings import settings


class BackendApiClient:
    def __init__(self):
        self.base_url = settings.services.ai_orchestrator_base_url.rstrip("/")
        self.timeout = settings.services.request_timeout_seconds
        self.default_headers = {
            "X-User-Id": settings.services.user_id,
            "X-User-Role": settings.services.user_role,
        }

    def get_task_full_detail(self, task_id: str, trace_id: str | None = None) -> dict[str, Any]:
        return self._get_json(f"/api/tasks/{task_id}/full", trace_id=trace_id)

    def get_research_workbench(
        self,
        target_code: str,
        target_name: str | None = None,
        trace_id: str | None = None,
    ) -> dict[str, Any]:
        params = {"targetCode": target_code}
        if target_name:
            params["targetName"] = target_name
        return self._get_json("/api/tasks/research-workbench", params=params, trace_id=trace_id)

    def get_market_event_detail(self, event_id: str, trace_id: str | None = None) -> dict[str, Any]:
        return self._get_json(f"/api/tasks/market-events/{event_id}", trace_id=trace_id)

    def list_market_events(
        self,
        target_code: str,
        target_name: str | None = None,
        page_size: int = 3,
        trace_id: str | None = None,
    ) -> list[dict[str, Any]]:
        return self._list_page_records(
            "/api/tasks/market-events",
            target_code=target_code,
            target_name=target_name,
            page_size=page_size,
            trace_id=trace_id,
        )

    def list_risk_warnings(
        self,
        target_code: str,
        target_name: str | None = None,
        page_size: int = 3,
        trace_id: str | None = None,
    ) -> list[dict[str, Any]]:
        return self._list_page_records(
            "/api/tasks/risk-warnings",
            target_code=target_code,
            target_name=target_name,
            page_size=page_size,
            trace_id=trace_id,
        )

    def list_strategy_signals(
        self,
        target_code: str,
        target_name: str | None = None,
        page_size: int = 3,
        trace_id: str | None = None,
    ) -> list[dict[str, Any]]:
        return self._list_page_records(
            "/api/tasks/strategy-signals",
            target_code=target_code,
            target_name=target_name,
            page_size=page_size,
            trace_id=trace_id,
        )

    def list_market_intelligence(
        self,
        target_code: str,
        target_name: str | None = None,
        page_size: int = 3,
        trace_id: str | None = None,
    ) -> list[dict[str, Any]]:
        return self._list_page_records(
            "/api/tasks/market-intelligence",
            target_code=target_code,
            target_name=target_name,
            page_size=page_size,
            trace_id=trace_id,
        )

    def list_market_event_source_configs(self, trace_id: str | None = None) -> list[dict[str, Any]]:
        data = self._get_data("/api/tasks/market-event-source-configs", trace_id=trace_id)
        if not isinstance(data, list):
            return []
        return [item for item in data if isinstance(item, dict)]

    def preview_market_event_source(
        self,
        source_code: str,
        target_code: str,
        target_name: str | None = None,
        target_type: str | None = None,
        item_count: int = 3,
        trace_id: str | None = None,
    ) -> dict[str, Any]:
        payload = {
            "targetCode": target_code,
            "itemCount": max(1, int(item_count or 1)),
        }
        if target_name:
            payload["targetName"] = target_name
        if target_type:
            payload["targetType"] = target_type
        data = self._post_data(
            f"/api/tasks/market-events/source-preview/{source_code}",
            json_body=payload,
            trace_id=trace_id,
        )
        return data if isinstance(data, dict) else {}

    def _get_json(
        self,
        path: str,
        params: dict[str, Any] | None = None,
        trace_id: str | None = None,
    ) -> dict[str, Any]:
        data = self._get_data(path, params=params, trace_id=trace_id)
        return data if isinstance(data, dict) else {}

    def _get_data(
        self,
        path: str,
        params: dict[str, Any] | None = None,
        trace_id: str | None = None,
    ) -> Any:
        return self._request_data("GET", path, params=params, trace_id=trace_id)

    def _post_data(
        self,
        path: str,
        json_body: dict[str, Any] | None = None,
        trace_id: str | None = None,
    ) -> Any:
        return self._request_data("POST", path, json_body=json_body, trace_id=trace_id)

    def _request_data(
        self,
        method: str,
        path: str,
        params: dict[str, Any] | None = None,
        json_body: dict[str, Any] | None = None,
        trace_id: str | None = None,
    ) -> Any:
        headers = dict(self.default_headers)
        if trace_id:
            headers["X-Trace-Id"] = trace_id

        response = httpx.request(
            method,
            f"{self.base_url}{path}",
            params=params,
            json=json_body,
            headers=headers,
            timeout=self.timeout,
        )
        response.raise_for_status()

        payload = response.json()
        if not payload.get("success"):
            code = payload.get("code") or "UNKNOWN"
            message = payload.get("message") or "backend api call failed"
            raise RuntimeError(f"{code}: {message}")

        return payload.get("data")

    def _list_page_records(
        self,
        path: str,
        *,
        target_code: str,
        target_name: str | None,
        page_size: int,
        trace_id: str | None,
    ) -> list[dict[str, Any]]:
        params = {
            "pageNum": 1,
            "pageSize": max(1, int(page_size or 1)),
            "targetCode": target_code,
        }
        if target_name:
            params["targetName"] = target_name
        data = self._get_json(path, params=params, trace_id=trace_id)
        records = data.get("records")
        if not isinstance(records, list):
            return []
        return [item for item in records if isinstance(item, dict)]
