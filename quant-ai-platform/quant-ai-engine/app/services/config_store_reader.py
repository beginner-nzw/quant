import json
from pathlib import Path
from typing import Any

import httpx

from app.config.settings import settings


class ConfigStoreReader:
    def __init__(self, store_code: str, legacy_file_name: str | None = None) -> None:
        self.store_code = store_code
        self.legacy_file_name = legacy_file_name

    def load_root(self) -> dict[str, Any]:
        try:
            backend_root = self._load_backend_root()
        except (httpx.HTTPError, RuntimeError) as exc:
            if not settings.services.config_store_fallback_enabled:
                raise RuntimeError(f"governed config store unavailable: {self.store_code}") from exc
            backend_root = {}
        if backend_root:
            return backend_root
        if not settings.services.config_store_fallback_enabled:
            raise RuntimeError(f"governed config store unavailable or empty: {self.store_code}")
        return self._load_legacy_file()

    def _load_backend_root(self) -> dict[str, Any]:
        response = httpx.get(
            f"{settings.services.ai_orchestrator_base_url.rstrip('/')}/api/tasks/config-store/{self.store_code}",
            headers={
                "X-User-Id": settings.services.user_id,
                "X-User-Role": settings.services.user_role,
            },
            timeout=settings.services.request_timeout_seconds,
        )
        response.raise_for_status()
        payload = response.json()
        if not payload.get("success"):
            raise RuntimeError(payload.get("message") or f"governed config store read failed: {self.store_code}")
        data = payload.get("data")
        if not isinstance(data, dict):
            return {}
        root = data.get("root")
        return root if isinstance(root, dict) else {}

    def _load_legacy_file(self) -> dict[str, Any]:
        if not self.legacy_file_name:
            return {}
        config_path = self._resolve_legacy_path()
        if not config_path.exists():
            return {}
        try:
            return json.loads(config_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise RuntimeError(f"legacy config fallback unreadable: {config_path}") from exc

    def _resolve_legacy_path(self) -> Path:
        current = Path(__file__).resolve()
        candidates = [
            current.parents[3] / "ai-config" / self.legacy_file_name,
            current.parents[4] / "ai-config" / self.legacy_file_name,
            Path.cwd() / "ai-config" / self.legacy_file_name,
            Path.cwd() / "quant-ai-platform" / "ai-config" / self.legacy_file_name,
        ]
        seen: set[Path] = set()
        for candidate in candidates:
            normalized = candidate.resolve()
            if normalized in seen:
                continue
            seen.add(normalized)
            if normalized.exists():
                return normalized
        return (current.parents[4] / "ai-config" / self.legacy_file_name).resolve()
