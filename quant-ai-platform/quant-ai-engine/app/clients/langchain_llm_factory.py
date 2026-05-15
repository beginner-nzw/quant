from typing import Any

from app.config.settings import settings
from app.services.model_strategy_repository import ModelStrategyRepository
from app.utils.logger import log_error

try:
    from langchain_openai import ChatOpenAI
except ImportError:
    ChatOpenAI = None


class LangChainLlmFactory:
    def __init__(self):
        self.strategy_repository = ModelStrategyRepository()

    def is_enabled(self, scenario_code: str | None = None) -> bool:
        return self.availability_reason(scenario_code) is None

    def availability_reason(self, scenario_code: str | None = None) -> str | None:
        config = self._runtime_config(scenario_code)
        if ChatOpenAI is None:
            return "LANGCHAIN_CHAT_MODEL_MISSING"
        if not config["enabled"]:
            return "MODEL_CONFIG_DISABLED"
        if not str(config["base_url"]).strip():
            return "MODEL_BASE_URL_MISSING"
        if not str(config["model"]).strip():
            return "MODEL_NAME_MISSING"
        return None

    def create_chat_model(self, scenario_code: str | None = None, **overrides: Any):
        if not self.is_enabled(scenario_code):
            return None

        config = self._runtime_config(scenario_code)

        model_kwargs = {
            "response_format": {
                "type": "json_object",
            }
        }
        if "model_kwargs" in overrides and isinstance(overrides["model_kwargs"], dict):
            model_kwargs.update(overrides.pop("model_kwargs"))

        try:
            return ChatOpenAI(
                model=overrides.pop("model", config["model"]),
                api_key=overrides.pop("api_key", config["api_key"]),
                base_url=overrides.pop("base_url", config["base_url"]),
                temperature=overrides.pop("temperature", config["temperature"]),
                timeout=overrides.pop("timeout", config["request_timeout_seconds"]),
                max_tokens=overrides.pop("max_tokens", config["max_tokens"]),
                model_kwargs=model_kwargs,
                **overrides,
            )
        except Exception as exc:
            log_error("", f"[AI-ENGINE][LANGCHAIN] create chat model failed err={exc}")
            return None

    def framework_name(self) -> str:
        return "langchain"

    def model_name(self, scenario_code: str | None = None) -> str | None:
        config = self._runtime_config(scenario_code)
        if not str(config["model"]).strip():
            return None
        return str(config["model"])

    def _runtime_config(self, scenario_code: str | None = None) -> dict[str, Any]:
        if scenario_code:
            return self.strategy_repository.build_runtime_config(scenario_code)
        return {
            "enabled": settings.model.enabled,
            "provider": settings.model.provider,
            "base_url": settings.model.base_url,
            "api_key": settings.model.api_key,
            "model": settings.model.model,
            "request_timeout_seconds": settings.model.request_timeout_seconds,
            "temperature": settings.model.temperature,
            "max_tokens": settings.model.max_tokens,
        }
