from pathlib import Path

from app.services.config_store_reader import ConfigStoreReader


class PromptTemplateRepository:
    def __init__(self) -> None:
        self.template_dir = Path(__file__).resolve().parents[3] / "prompt-templates"
        self.config_store_reader = ConfigStoreReader("prompt-templates")

    def load_system_prompt(self, template_code: str, fallback_prompt: str) -> str:
        root = self.config_store_reader.load_root()
        templates = root.get("templates")
        if isinstance(templates, dict):
            content = templates.get(template_code)
            if isinstance(content, str) and content.strip():
                return content.strip()

        prompt_path = self.template_dir / f"{template_code}.txt"
        if prompt_path.exists():
            content = prompt_path.read_text(encoding="utf-8").strip()
            if content:
                return content
        return fallback_prompt.strip()
