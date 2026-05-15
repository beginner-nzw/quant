import time
from typing import Any

from app.clients.backend_client import BackendApiClient
from app.utils.logger import log_error


class TaskLoaderService:
    def __init__(self):
        self.backend_client = BackendApiClient()

    def load_task_context(
        self,
        task_id: str,
        trace_id: str,
        fallback_detail: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        fallback_detail = fallback_detail or {}
        context = {
            "contextLoaded": False,
            "contextSource": "dispatch",
            "taskDetail": fallback_detail,
            "taskState": {},
            "summary": {},
            "report": {},
            "workflow": {},
            "sourceTask": {},
            "sourceEvent": {},
        }

        task_full_detail = self._load_task_full_detail(task_id, trace_id)
        if not task_full_detail:
            return context

        context.update(
            {
                "contextLoaded": True,
                "contextSource": "task-full-api",
                "taskDetail": task_full_detail.get("taskDetail") or fallback_detail,
                "taskState": task_full_detail.get("taskState") or {},
                "summary": task_full_detail.get("summary") or {},
                "report": task_full_detail.get("report") or {},
                "workflow": task_full_detail.get("workflow") or {},
            }
        )

        source_task_id = (context["taskDetail"] or {}).get("sourceTaskId")
        if source_task_id and source_task_id != task_id:
            source_task_detail = self._load_task_full_detail(source_task_id, trace_id)
            if source_task_detail:
                context["sourceTask"] = source_task_detail

        source_event_id = (context["taskDetail"] or {}).get("sourceEventId")
        if source_event_id:
            source_event_detail = self._load_market_event_detail(source_event_id, trace_id)
            if source_event_detail:
                context["sourceEvent"] = source_event_detail

        return context

    def _load_task_full_detail(self, task_id: str, trace_id: str) -> dict[str, Any]:
        last_error: Exception | None = None
        for attempt in range(2):
            try:
                return self.backend_client.get_task_full_detail(task_id, trace_id=trace_id)
            except Exception as exc:
                last_error = exc
                time.sleep(0.2)

        log_error(trace_id, f"[AI-ENGINE][TASK-CONTEXT] load failed task={task_id} err={last_error}")
        return {}

    def _load_market_event_detail(self, event_id: str, trace_id: str) -> dict[str, Any]:
        last_error: Exception | None = None
        for attempt in range(2):
            try:
                return self.backend_client.get_market_event_detail(event_id, trace_id=trace_id)
            except Exception as exc:
                last_error = exc
                time.sleep(0.2)

        log_error(trace_id, f"[AI-ENGINE][EVENT-CONTEXT] load failed event={event_id} err={last_error}")
        return {}
