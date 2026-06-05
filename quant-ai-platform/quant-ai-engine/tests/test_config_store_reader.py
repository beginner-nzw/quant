import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from app.services.agent_config_repository import AgentConfigRepository
from app.services.config_store_reader import ConfigStoreReader
from app.services.prompt_template_repository import PromptTemplateRepository


class ConfigStoreReaderTests(unittest.TestCase):
    def test_load_root_prefers_backend_authority(self):
        class Response:
            def raise_for_status(self):
                return None

            def json(self):
                return {
                    "success": True,
                    "data": {
                        "root": {
                            "agents": [
                                {"agentCode": "backend_agent", "enabled": True}
                            ]
                        }
                    },
                }

        with patch("app.services.config_store_reader.httpx.get", return_value=Response()):
            root = ConfigStoreReader("agent-configs", "agent-configs.json").load_root()

        self.assertEqual("backend_agent", root["agents"][0]["agentCode"])

    def test_load_root_falls_back_to_legacy_file_for_demo(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            legacy = Path(temp_dir) / "agent-configs.json"
            legacy.write_text(json.dumps({"agents": [{"agentCode": "legacy_agent"}]}), encoding="utf-8")
            reader = ConfigStoreReader("agent-configs", "agent-configs.json")
            reader._resolve_legacy_path = lambda: legacy

            with patch("app.services.config_store_reader.settings.services.config_store_fallback_enabled", True), \
                    patch("app.services.config_store_reader.httpx.get", side_effect=RuntimeError("backend down")):
                root = reader.load_root()

        self.assertEqual("legacy_agent", root["agents"][0]["agentCode"])

    def test_load_root_raises_when_backend_down_and_production_fallback_disabled(self):
        reader = ConfigStoreReader("agent-configs", "agent-configs.json")

        with patch("app.services.config_store_reader.settings.services.config_store_fallback_enabled", False), \
                patch("app.services.config_store_reader.httpx.get", side_effect=RuntimeError("backend down")):
            with self.assertRaises(RuntimeError):
                reader.load_root()

    def test_agent_repository_uses_stable_reader_api(self):
        repo = AgentConfigRepository()
        repo.config_store_reader.load_root = lambda: {"agents": [{"agentCode": "planner_agent", "enabled": False}]}

        self.assertFalse(repo.is_enabled("planner_agent", default=True))

    def test_prompt_repository_reads_backend_templates_before_fallback_prompt(self):
        repo = PromptTemplateRepository()
        repo.config_store_reader.load_root = lambda: {"templates": {"planner_agent_template": "backend prompt"}}

        self.assertEqual("backend prompt", repo.load_system_prompt("planner_agent_template", "fallback prompt"))


if __name__ == "__main__":
    unittest.main()
