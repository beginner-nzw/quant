from pathlib import Path
from typing import Any
import yaml
from pydantic import BaseModel


class AppConfig(BaseModel):
    env: str
    host: str
    port: int
    workflow_timeout_seconds: int = 60
    workflow_timeout_buffer_seconds: int = 30
    model_node_timeout_buffer_seconds: int = 15


class KafkaTopicsConfig(BaseModel):
    dispatch: str
    status: str
    result: str
    audit: str


class KafkaConfig(BaseModel):
    bootstrap_servers: str
    consumer_group: str
    topics: KafkaTopicsConfig


class RedisConfig(BaseModel):
    host: str
    port: int
    db: int = 0


class ServicesConfig(BaseModel):
    ai_orchestrator_base_url: str
    request_timeout_seconds: float = 5.0
    user_id: str = "ai-engine"
    user_role: str = "ADMIN"


class ModelConfig(BaseModel):
    enabled: bool = False
    provider: str = "openai-compatible"
    base_url: str = ""
    api_key: str = ""
    model: str = ""
    request_timeout_seconds: float = 20.0
    temperature: float = 0.2
    max_tokens: int = 1200


class Settings(BaseModel):
    app: AppConfig
    kafka: KafkaConfig
    redis: RedisConfig
    services: ServicesConfig
    model: ModelConfig = ModelConfig()


def _deep_merge(base: dict[str, Any], override: dict[str, Any]) -> dict[str, Any]:
    merged = dict(base)
    for key, value in override.items():
        current = merged.get(key)
        if isinstance(current, dict) and isinstance(value, dict):
            merged[key] = _deep_merge(current, value)
        else:
            merged[key] = value
    return merged


def load_settings(profile: str = "local") -> Settings:
    base_dir = Path(__file__).resolve().parent
    config_file = base_dir / f"{profile}.yml"
    private_config_file = base_dir / f"{profile}.private.yml"

    if not config_file.exists():
        raise FileNotFoundError(f"config file not found: {config_file}")

    with open(config_file, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f)

    if private_config_file.exists():
        with open(private_config_file, "r", encoding="utf-8") as f:
            private_data = yaml.safe_load(f) or {}
        data = _deep_merge(data, private_data)

    return Settings(**data)


settings = load_settings("local")
