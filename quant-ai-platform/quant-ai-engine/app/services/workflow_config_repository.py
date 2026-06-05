import json
from pathlib import Path
from typing import Any

from app.services.config_store_reader import ConfigStoreReader


class WorkflowConfigRepository:
    def __init__(self) -> None:
        self.config_file_name = "workflow-configs.json"
        self.config_store_reader = ConfigStoreReader("workflow-configs", self.config_file_name)

    def load_workflows(self) -> list[dict[str, Any]]:
        data = self._load_file()
        workflows = data.get("workflows")
        if not isinstance(workflows, list):
            return []
        return [item for item in workflows if isinstance(item, dict)]

    def resolve_workflow(self, task_type: str | None) -> dict[str, Any]:
        normalized_task_type = self._normalize(task_type)
        enabled_workflows = [
            item for item in self.load_workflows()
            if item.get("enabled") is not False
        ]
        if not enabled_workflows:
            return {}

        if normalized_task_type:
            for item in enabled_workflows:
                if normalized_task_type in self._normalize_list(item.get("taskTypes")):
                    return item

        for item in enabled_workflows:
            if item.get("defaultSelected") is True:
                return item

        return enabled_workflows[0]

    def _load_file(self) -> dict[str, Any]:
        return self.config_store_reader.load_root()

    def _resolve_config_path(self) -> Path:
        current = Path(__file__).resolve()
        candidates = [
            current.parents[3] / "ai-config" / self.config_file_name,
            current.parents[4] / "ai-config" / self.config_file_name,
            Path.cwd() / "ai-config" / self.config_file_name,
            Path.cwd() / "quant-ai-platform" / "ai-config" / self.config_file_name,
        ]

        seen: set[Path] = set()
        for candidate in candidates:
            normalized = candidate.resolve()
            if normalized in seen:
                continue
            seen.add(normalized)
            if normalized.exists():
                return normalized

        return (current.parents[4] / "ai-config" / self.config_file_name).resolve()

    def _normalize_list(self, values: Any) -> list[str]:
        if not isinstance(values, list):
            return []
        result: list[str] = []
        for value in values:
            normalized = self._normalize(value)
            if normalized:
                result.append(normalized)
        return result

    def _normalize(self, value: Any) -> str | None:
        if value is None:
            return None
        normalized = str(value).strip()
        return normalized or None
