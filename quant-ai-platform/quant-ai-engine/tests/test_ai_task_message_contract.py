import unittest

from app.messaging.message_models import (
    AgentAuditItem,
    AiTaskAuditMessage,
    AiTaskAuditPayload,
    AiTaskDispatchMessage,
    AiTaskDispatchPayload,
    AiTaskMessageEnvelope,
    AiTaskResultMessage,
    AiTaskResultPayload,
    AiTaskStatusMessage,
    AiTaskStatusPayload,
)


ENVELOPE_FIELDS = [
    'messageId',
    'traceId',
    'taskId',
    'eventId',
    'messageType',
    'sourceService',
    'targetService',
    'tenantId',
    'bizKey',
    'timestamp',
    'version',
    'retryCount',
]


class AiTaskMessageContractTests(unittest.TestCase):
    def test_envelope_fields_match_java_contract(self):
        self.assertEqual(ENVELOPE_FIELDS, list(AiTaskMessageEnvelope.model_fields))
        self.assertEqual(
            ENVELOPE_FIELDS + ['payload'],
            list(AiTaskDispatchMessage.model_fields),
        )
        self.assertEqual(
            ENVELOPE_FIELDS + ['payload'],
            list(AiTaskStatusMessage.model_fields),
        )
        self.assertEqual(
            ENVELOPE_FIELDS + ['payload'],
            list(AiTaskResultMessage.model_fields),
        )
        self.assertEqual(
            ENVELOPE_FIELDS + ['payload'],
            list(AiTaskAuditMessage.model_fields),
        )

    def test_dispatch_payload_fields_match_java_contract(self):
        self.assertEqual(
            [
                'taskType',
                'taskTitle',
                'targetType',
                'targetCode',
                'targetName',
                'priority',
                'sourceTaskId',
                'sourceReportId',
                'sourceEventId',
                'sourceDomain',
                'sourceReviewStatus',
                'analysisScope',
            ],
            list(AiTaskDispatchPayload.model_fields),
        )

    def test_status_payload_fields_match_java_contract(self):
        self.assertEqual(
            [
                'workflowInstanceId',
                'status',
                'currentStage',
                'currentNode',
                'progress',
            ],
            list(AiTaskStatusPayload.model_fields),
        )

    def test_result_payload_fields_match_java_contract(self):
        self.assertEqual(
            [
                'workflowInstanceId',
                'taskType',
                'taskTitle',
                'analysisScope',
                'targetType',
                'targetCode',
                'targetName',
                'priority',
                'sourceTaskId',
                'sourceReportId',
                'sourceEventId',
                'sourceDomain',
                'sourceReviewStatus',
                'finalStatus',
                'finalStage',
                'summary',
                'confidenceScore',
                'needHumanReview',
                'riskWarnings',
                'reportMeta',
                'resultRef',
            ],
            list(AiTaskResultPayload.model_fields),
        )

    def test_audit_payload_fields_match_java_contract(self):
        self.assertEqual(
            [
                'workflowInstanceId',
                'agents',
                'reviewSuggestion',
                'evidenceRefs',
            ],
            list(AiTaskAuditPayload.model_fields),
        )
        self.assertEqual(
            [
                'executionId',
                'agentCode',
                'agentName',
                'nodeCode',
                'status',
                'confidenceScore',
                'needHumanReview',
                'startTimestamp',
                'finishTimestamp',
                'durationMs',
            ],
            list(AgentAuditItem.model_fields),
        )

    def test_python_required_fields_are_explicitly_guarded(self):
        expected_required = {
            AiTaskDispatchPayload: {
                'taskType',
                'targetType',
                'targetCode',
                'targetName',
                'priority',
            },
            AiTaskStatusPayload: {
                'status',
                'currentStage',
                'currentNode',
                'progress',
            },
            AiTaskResultPayload: {
                'taskType',
                'finalStatus',
                'summary',
                'confidenceScore',
                'needHumanReview',
                'riskWarnings',
                'reportMeta',
                'resultRef',
            },
            AiTaskAuditPayload: {
                'agents',
                'reviewSuggestion',
                'evidenceRefs',
            },
            AgentAuditItem: {
                'executionId',
                'agentCode',
                'agentName',
                'nodeCode',
                'status',
            },
        }

        for model_type, required_fields in expected_required.items():
            with self.subTest(model_type=model_type.__name__):
                actual_required = {
                    name
                    for name, field in model_type.model_fields.items()
                    if field.is_required()
                }
                self.assertEqual(required_fields, actual_required)


if __name__ == '__main__':
    unittest.main()
