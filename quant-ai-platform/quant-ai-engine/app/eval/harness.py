from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class EvalCase:
    name: str
    report: dict[str, Any]
    evidence_items: list[dict[str, Any]]
    fallback_expected: bool = False


class AiEvalHarness:
    def evaluate(self, case: EvalCase) -> dict[str, Any]:
        hallucination = self._score_hallucination(case.report, case.evidence_items)
        grounding = self._score_grounding(case.report, case.evidence_items)
        fallback = self._score_fallback(case.report, case.fallback_expected)
        quality = self._score_report_quality(case.report)
        overall = round((hallucination + grounding + fallback + quality) / 4, 4)
        return {
            "caseName": case.name,
            "overallScore": overall,
            "passed": overall >= 0.75 and min(hallucination, grounding, fallback, quality) >= 0.6,
            "scores": {
                "hallucination": hallucination,
                "evidenceGrounding": grounding,
                "fallback": fallback,
                "reportQuality": quality,
            },
        }

    def _score_hallucination(self, report: dict[str, Any], evidence_items: list[dict[str, Any]]) -> float:
        text = self._report_text(report)
        unsupported_red_flags = [
            "guaranteed",
            "risk-free",
            "100%",
            "definitely",
            "no downside",
        ]
        penalty = sum(0.18 for token in unsupported_red_flags if token.lower() in text.lower())
        evidence_terms = self._evidence_terms(evidence_items)
        if evidence_terms and not any(term.lower() in text.lower() for term in evidence_terms):
            penalty += 0.25
        return round(max(0.0, 1.0 - penalty), 4)

    def _score_grounding(self, report: dict[str, Any], evidence_items: list[dict[str, Any]]) -> float:
        evidence_refs = report.get("evidenceRefs") or []
        if not evidence_items and not evidence_refs:
            return 0.0
        evidence_ids = {
            str(item.get("evidenceId") or item.get("referenceId") or "").strip()
            for item in evidence_items
        }
        evidence_ids = {item for item in evidence_ids if item}
        traceable_refs = [ref for ref in evidence_refs if isinstance(ref, str) and ":" in ref]
        matched_refs = [
            ref for ref in evidence_refs
            if any(evidence_id and evidence_id in str(ref) for evidence_id in evidence_ids)
        ]
        score = 0.35 if evidence_items else 0.0
        if traceable_refs:
            score += 0.3
        if evidence_ids and matched_refs:
            score += 0.25
        if report.get("contextSnapshot"):
            score += 0.1
        return round(min(1.0, score), 4)

    def _score_fallback(self, report: dict[str, Any], fallback_expected: bool) -> float:
        snapshot = report.get("contextSnapshot") or {}
        path = str(snapshot.get("reportGenerationPath") or report.get("generationPath") or "").upper()
        reason = str(snapshot.get("reportFallbackReason") or report.get("fallbackReason") or "").strip()
        if fallback_expected:
            return 1.0 if "FALLBACK" in path and reason else 0.4
        return 1.0 if "FALLBACK" not in path else 0.7

    def _score_report_quality(self, report: dict[str, Any]) -> float:
        summary = str(report.get("summary") or "").strip()
        highlights = report.get("highlights") if isinstance(report.get("highlights"), list) else []
        risk_points = report.get("riskPoints") if isinstance(report.get("riskPoints"), list) else []
        confidence = report.get("confidenceScore")
        score = 0.0
        if len(summary) >= 40:
            score += 0.35
        if len(highlights) >= 2:
            score += 0.25
        if risk_points:
            score += 0.2
        if isinstance(confidence, (int, float)) and 0 <= confidence <= 1:
            score += 0.1
        if report.get("reviewSuggestion"):
            score += 0.1
        return round(min(1.0, score), 4)

    def _report_text(self, report: dict[str, Any]) -> str:
        values: list[str] = []
        for key in ("summary", "reviewSuggestion"):
            if report.get(key):
                values.append(str(report[key]))
        for key in ("highlights", "riskPoints", "riskWarnings", "evidenceRefs"):
            if isinstance(report.get(key), list):
                values.extend(str(item) for item in report[key])
        return "\n".join(values)

    def _evidence_terms(self, evidence_items: list[dict[str, Any]]) -> list[str]:
        terms: list[str] = []
        for item in evidence_items:
            for key in ("title", "summary", "source", "referenceId"):
                value = str(item.get(key) or "").strip()
                if len(value) >= 4:
                    terms.append(value)
        return terms
