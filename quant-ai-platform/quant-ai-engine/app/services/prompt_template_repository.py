from pathlib import Path


class PromptTemplateRepository:
    def __init__(self) -> None:
        self.template_dir = Path(__file__).resolve().parents[3] / "prompt-templates"

    def load_system_prompt(self, template_code: str, fallback_prompt: str) -> str:
        prompt_path = self.template_dir / f"{template_code}.txt"
        if prompt_path.exists():
            content = prompt_path.read_text(encoding="utf-8").strip()
            if content:
                return content
        return fallback_prompt.strip()
