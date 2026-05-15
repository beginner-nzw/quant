import json
from typing import Any

import httpx

from app.config.settings import settings
from app.services.model_strategy_repository import ModelStrategyRepository
from app.utils.logger import log_error, log_info


class ModelClient:
    def __init__(self):
        self.strategy_repository = ModelStrategyRepository()

    def is_enabled(self, scene: str | None = None) -> bool:
        return self.availability_reason(scene) is None

    def availability_reason(self, scene: str | None = None) -> str | None:
        config = self._runtime_config(scene)
        if not config["enabled"]:
            return "MODEL_CONFIG_DISABLED"
        if not str(config["base_url"]).strip():
            return "MODEL_BASE_URL_MISSING"
        if not str(config["model"]).strip():
            return "MODEL_NAME_MISSING"
        return None

    def model_name(self, scene: str | None = None) -> str | None:
        config = self._runtime_config(scene)
        model_name = str(config["model"]).strip()
        return model_name or None

    def generate_json_object(
        self,
        *,
        scene: str,
        system_prompt: str,
        user_prompt: str,
        trace_id: str,
    ) -> dict[str, Any] | None:
        if not self.is_enabled(scene):
            return None

        config = self._runtime_config(scene)

        endpoint = self._build_endpoint(config)
        headers = {
            "Content-Type": "application/json",
        }
        if str(config["api_key"]).strip():
            headers["Authorization"] = f"Bearer {str(config['api_key']).strip()}"

        payload = {
            "model": config["model"],
            "temperature": config["temperature"],
            "max_tokens": config["max_tokens"],
            "response_format": {
                "type": "json_object",
            },
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        }

        try:
            with httpx.Client(timeout=config["request_timeout_seconds"]) as client:
                response = client.post(endpoint, headers=headers, json=payload)
                response.raise_for_status()
                data = response.json()
        except Exception as exc:
            log_error(trace_id, f"[AI-ENGINE][MODEL] request failed scene={scene} provider={config['provider']} err={exc}")
            return None

        content = self._extract_message_content(data)
        if not content:
            log_error(trace_id, f"[AI-ENGINE][MODEL] empty response content scene={scene}")
            return None

        parsed = self._parse_json_content(content)
        if not isinstance(parsed, dict):
            log_error(trace_id, f"[AI-ENGINE][MODEL] failed to parse JSON response scene={scene}")
            return None

        log_info(trace_id, f"[AI-ENGINE][MODEL] generated scene={scene} model={config['model']}")
        return parsed

    def generate_report(
        self,
        *,
        system_prompt: str,
        user_prompt: str,
        trace_id: str,
    ) -> dict[str, Any] | None:
        return self.generate_json_object(
            scene="report",
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            trace_id=trace_id,
        )

    def _build_endpoint(self, config: dict[str, Any]) -> str:
        base_url = str(config["base_url"]).strip().rstrip("/")
        if base_url.endswith("/chat/completions"):
            return base_url
        return f"{base_url}/chat/completions"

    def _runtime_config(self, scene: str | None = None) -> dict[str, Any]:
        scenario_code = self._resolve_scenario_code(scene)
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

    def _resolve_scenario_code(self, scene: str | None) -> str | None:
        mapping = {
            "planner": "TASK_PLANNING",
            "intent": "TASK_INTENT",
            "financial": "FINANCIAL_ANALYSIS",
            "risk": "RISK_REVIEW",
            "report": "REPORT_GENERATION",
        }
        return mapping.get(str(scene or "").strip().lower())

    def _extract_message_content(self, response_json: dict[str, Any]) -> str:
        choices = response_json.get("choices")
        if not isinstance(choices, list) or not choices:
            return ""

        message = choices[0].get("message")
        if not isinstance(message, dict):
            return ""

        content = message.get("content")
        if isinstance(content, str):
            return content.strip()

        if isinstance(content, list):
            parts: list[str] = []
            for item in content:
                if isinstance(item, dict) and item.get("type") == "text":
                    text = item.get("text")
                    if isinstance(text, str) and text.strip():
                        parts.append(text.strip())
            return "\n".join(parts).strip()

        return ""

    def _parse_json_content(self, content: str) -> dict[str, Any] | None:
        normalized = content.strip()
        if normalized.startswith("```"):
            normalized = normalized.strip("`")
            if normalized.startswith("json"):
                normalized = normalized[4:].strip()

        try:
            return json.loads(normalized)
        except json.JSONDecodeError:
            pass

        start = normalized.find("{")
        end = normalized.rfind("}")
        if start == -1 or end == -1 or end <= start:
            return None

        try:
            return json.loads(normalized[start:end + 1])
        except json.JSONDecodeError:
            return None
