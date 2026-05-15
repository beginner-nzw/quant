from pydantic import BaseModel

from app.clients.langchain_llm_factory import LangChainLlmFactory
from app.services.prompt_builder_service import PromptBuilderService
from app.utils.logger import log_error, log_info

try:
    from langchain_core.output_parsers import PydanticOutputParser
    from langchain_core.prompts import ChatPromptTemplate
except ImportError:
    PydanticOutputParser = None
    ChatPromptTemplate = None


class FinancialAnalysisOutput(BaseModel):
    summary: str
    revenueTrend: str
    profitTrend: str
    cashflowSignal: str


class LangChainFinancialService:
    SCENARIO_CODE = "FINANCIAL_ANALYSIS"

    def __init__(self):
        self.factory = LangChainLlmFactory()
        self.prompt_builder_service = PromptBuilderService()
        self.parser = (
            PydanticOutputParser(pydantic_object=FinancialAnalysisOutput)
            if PydanticOutputParser is not None
            else None
        )

    def is_enabled(self) -> bool:
        return bool(
            self.factory.is_enabled(self.SCENARIO_CODE)
            and self.parser is not None
            and ChatPromptTemplate is not None
        )

    def availability_reason(self) -> str | None:
        if self.factory.availability_reason(self.SCENARIO_CODE) is not None:
            return self.factory.availability_reason(self.SCENARIO_CODE)
        if self.parser is None:
            return "LANGCHAIN_PARSER_MISSING"
        if ChatPromptTemplate is None:
            return "LANGCHAIN_PROMPT_TEMPLATE_MISSING"
        return None

    def generate_financial_result(
        self,
        *,
        state: dict,
        fallback_result: dict,
    ) -> dict | None:
        if not self.is_enabled():
            return None

        llm = self.factory.create_chat_model(self.SCENARIO_CODE)
        if llm is None:
            return None

        system_prompt, user_prompt = self.prompt_builder_service.build_financial_prompts(
            state=state,
            fallback_result=fallback_result,
            format_instructions=self.parser.get_format_instructions(),
        )
        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", "{system_prompt}"),
                ("human", "{user_prompt}"),
            ]
        )
        chain = prompt | llm | self.parser

        try:
            result = chain.invoke(
                {
                    "system_prompt": system_prompt,
                    "user_prompt": user_prompt,
                }
            )
        except Exception as exc:
            log_error(
                state.get("trace_id", ""),
                f"[AI-ENGINE][LANGCHAIN] financial generation failed err={exc}",
            )
            return None

        if not result.summary.strip():
            log_error(
                state.get("trace_id", ""),
                "[AI-ENGINE][LANGCHAIN] financial generation returned empty summary",
            )
            return None

        log_info(
            state.get("trace_id", ""),
            f"[AI-ENGINE][LANGCHAIN] financial generated model={self.factory.model_name(self.SCENARIO_CODE)}",
        )
        return result.model_dump()

    def framework_name(self) -> str:
        return self.factory.framework_name()

    def model_name(self) -> str | None:
        return self.factory.model_name(self.SCENARIO_CODE)
