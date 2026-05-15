import json
from pathlib import Path
from typing import Any


class AgentConfigRepository:
    def __init__(self) -> None:
        self.config_file_name = "agent-configs.json"

    def load_agents(self) -> list[dict[str, Any]]:
        data = self._load_file()
        agents = data.get("agents")
        if not isinstance(agents, list):
            return []
        return [item for item in agents if isinstance(item, dict)]

    def load_agent(self, agent_code: str | None) -> dict[str, Any]:
        if not agent_code:
            return {}
        for item in self.load_agents():
            if str(item.get("agentCode") or "").strip() == agent_code:
                return item
        return {}

    def is_enabled(self, agent_code: str | None, default: bool = True) -> bool:
        config = self.load_agent(agent_code)
        if not config:
            return default
        return bool(config.get("enabled", default))

    def timeout_seconds(self, agent_code: str | None, default: int) -> int:
        config = self.load_agent(agent_code)
        if not config:
            return default
        raw_value = config.get("timeoutSeconds")
        if raw_value is None:
            return default
        try:
            timeout_seconds = int(raw_value)
        except (TypeError, ValueError):
            return default
        return timeout_seconds if timeout_seconds > 0 else default

    def _load_file(self) -> dict[str, Any]:
        config_path = self._resolve_config_path()
        if not config_path.exists():
            return {}
        try:
            return json.loads(config_path.read_text(encoding="utf-8"))
        except Exception:
            return {}

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
