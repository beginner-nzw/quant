import time
from typing import Any

from app.services.task_control_service import TaskControlService


class AuditComplianceAgent:
    def __init__(self):
        self.task_control_service = TaskControlService()

    def invoke(self, state):
        self.task_control_service.check_cancelled(state["task_id"])
        now = int(time.time() * 1000)

        evidence_items = self._normalize_evidence_items(state.get("evidence_items"))
        evidence_refs = self._normalize_text_list(state.get("evidence_refs"))
        risk_result = state.get("risk_result") or {}
        strategy_result = state.get("strategy_result") or {}

        policy_checks = self._build_policy_checks(
            evidence_items=evidence_items,
            evidence_refs=evidence_refs,
            risk_result=risk_result,
            strategy_result=strategy_result,
        )
        evidence_checks = self._build_evidence_checks(evidence_items, evidence_refs)
        report_review = self._build_report_review_support(policy_checks, evidence_checks, risk_result)
        review_suggestions = self._build_review_suggestions(policy_checks, evidence_checks, report_review)

        audit_result = {
            "supportType": "POLICY_EVIDENCE_REPORT_REVIEW_SUPPORT",
            "authority": "SUPPORT_ONLY_NO_BUSINESS_APPROVAL",
            "policyChecks": policy_checks,
            "evidenceChecks": evidence_checks,
            "reportReview": report_review,
            "reviewSuggestions": review_suggestions,
            "trace": {
                "taskId": state.get("task_id"),
                "traceId": state.get("trace_id"),
                "source": "audit_compliance_agent",
            },
        }

        state["current_stage"] = "AUDIT_COMPLIANCE"
        state["current_node"] = "audit_compliance_agent"
        state["progress"] = 92
        state["audit_result"] = audit_result
        state.setdefault("agent_audits", []).append(
            {
                "executionId": f"exec-{state['task_id']}-audit-compliance",
                "agentCode": "audit_compliance_agent",
                "agentName": "Audit Compliance Agent",
                "nodeCode": "audit_compliance_agent",
                "status": "SUCCESS",
                "confidenceScore": 0.86 if evidence_items else 0.72,
                "needHumanReview": False,
                "startTimestamp": now,
                "finishTimestamp": now,
                "durationMs": 0,
            }
        )
        return state

    def _build_policy_checks(
        self,
        *,
        evidence_items: list[dict[str, Any]],
        evidence_refs: list[str],
        risk_result: dict[str, Any],
        strategy_result: dict[str, Any],
    ) -> list[dict[str, Any]]:
        return [
            {
                "policyCode": "NO_DIRECT_APPROVAL",
                "policyName": "No direct business approval",
                "status": "PASS",
                "severity": "HIGH",
                "finding": "Audit agent provides support metadata only and does not set report review status.",
            },
            {
                "policyCode": "EVIDENCE_REQUIRED",
                "policyName": "Evidence presence",
                "status": "PASS" if evidence_items or evidence_refs else "REVIEW",
                "severity": "MEDIUM",
                "finding": f"Structured evidence={len(evidence_items)}, refs={len(evidence_refs)}.",
            },
            {
                "policyCode": "RISK_STRATEGY_BOUNDARY",
                "policyName": "Risk/strategy authority boundary",
                "status": "PASS",
                "severity": "HIGH",
                "finding": (
                    "Risk output remains risk context; strategy output remains a candidate until Java projection "
                    "persists strategy_signal and strategy_signal_factor."
                ),
                "strategyAuthority": strategy_result.get("trace", {}).get("authority"),
                "needHumanReview": bool(risk_result.get("needHumanReview")),
            },
        ]

    def _build_evidence_checks(
        self,
        evidence_items: list[dict[str, Any]],
        evidence_refs: list[str],
    ) -> list[dict[str, Any]]:
        checks = []
        missing_summary = [
            self._normalize_text(item.get("evidenceId")) or self._normalize_text(item.get("title")) or "unknown"
            for item in evidence_items
            if not self._normalize_text(item.get("summary"))
        ]
        checks.append({
            "checkCode": "STRUCTURED_EVIDENCE_SUMMARY",
            "status": "PASS" if not missing_summary else "REVIEW",
            "finding": "All structured evidence has summaries." if not missing_summary else "Some evidence lacks summaries.",
            "evidenceRefs": missing_summary[:5],
        })
        source_refs = [ref for ref in evidence_refs if ":" in ref]
        checks.append({
            "checkCode": "TRACEABLE_REFERENCE_FORMAT",
            "status": "PASS" if source_refs else "REVIEW",
            "finding": f"{len(source_refs)} traceable evidence refs use source:value format.",
            "evidenceRefs": source_refs[:8],
        })
        return checks

    def _build_report_review_support(
        self,
        policy_checks: list[dict[str, Any]],
        evidence_checks: list[dict[str, Any]],
        risk_result: dict[str, Any],
    ) -> dict[str, Any]:
        review_items = [
            item
            for item in [*policy_checks, *evidence_checks]
            if item.get("status") != "PASS"
        ]
        risk_points = self._normalize_text_list(risk_result.get("riskPoints"))
        return {
            "supportStatus": "REVIEW_SUGGESTED" if review_items or risk_points else "READY_FOR_HUMAN_REVIEW",
            "reviewItemCount": len(review_items),
            "riskPointCount": len(risk_points),
            "doesNotApproveReport": True,
        }

    def _build_review_suggestions(
        self,
        policy_checks: list[dict[str, Any]],
        evidence_checks: list[dict[str, Any]],
        report_review: dict[str, Any],
    ) -> list[str]:
        suggestions = []
        if report_review.get("reviewItemCount"):
            suggestions.append("Human reviewer should inspect non-pass policy/evidence checks before report approval.")
        if any(item.get("checkCode") == "STRUCTURED_EVIDENCE_SUMMARY" and item.get("status") != "PASS" for item in evidence_checks):
            suggestions.append("Add or confirm summaries for evidence items flagged by the audit support check.")
        if any(item.get("policyCode") == "EVIDENCE_REQUIRED" and item.get("status") != "PASS" for item in policy_checks):
            suggestions.append("Attach at least one traceable evidence item or reference before relying on the report.")
        if not suggestions:
            suggestions.append("Evidence and policy checks are available for human report review; no business approval is implied.")
        return suggestions

    def _normalize_evidence_items(self, value: Any) -> list[dict[str, Any]]:
        if not isinstance(value, list):
            return []
        return [item for item in value if isinstance(item, dict)]

    def _normalize_text_list(self, value: Any) -> list[str]:
        if not isinstance(value, list):
            return []
        result = []
        for item in value:
            text = self._normalize_text(item)
            if text:
                result.append(text)
        return result

    def _normalize_text(self, value: Any) -> str:
        if value is None:
            return ""
        return str(value).strip()
