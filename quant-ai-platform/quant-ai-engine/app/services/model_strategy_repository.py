import json
from pathlib import Path
from typing import Any

from app.config.settings import settings


class ModelStrategyRepository:
    def __init__(self) -> None:
        self.config_file_name = "model-strategies.json"

    def load_strategy(self, scenario_code: str | None) -> dict[str, Any]:
        if not scenario_code:
            return {}

        data = self._load_file()
        strategies = data.get("strategies")
        if not isinstance(strategies, list):
            return {}

        for item in strategies:
            if not isinstance(item, dict):
                continue
            if str(item.get("scenarioCode") or "").strip() == scenario_code:
                return item
        return {}

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

    def build_runtime_config(self, scenario_code: str | None) -> dict[str, Any]:
        strategy = self.load_strategy(scenario_code)
        return {
            "enabled": bool(strategy.get("enabled", settings.model.enabled)),
            "provider": str(strategy.get("provider") or settings.model.provider),
            "base_url": str(strategy.get("baseUrl") or settings.model.base_url),
            "api_key": settings.model.api_key,
            "model": str(strategy.get("modelName") or settings.model.model),
            "request_timeout_seconds": float(strategy.get("requestTimeoutSeconds") or settings.model.request_timeout_seconds),
            "temperature": float(strategy.get("temperature") if strategy.get("temperature") is not None else settings.model.temperature),
            "max_tokens": int(strategy.get("maxTokens") or settings.model.max_tokens),
            "access_mode": str(strategy.get("accessMode") or "LANGCHAIN_OPENAI_COMPATIBLE"),
            "fallback_enabled": bool(strategy.get("fallbackEnabled", True)),
        }
