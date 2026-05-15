from typing import Optional, Any

from pydantic import BaseModel


class AiTaskDispatchPayload(BaseModel):
    taskType: str
    taskTitle: Optional[str] = None
    targetType: str
    targetCode: str
    targetName: str
    priority: str
    sourceTaskId: Optional[str] = None
    sourceReportId: Optional[str] = None
    sourceEventId: Optional[str] = None
    sourceDomain: Optional[str] = None
    sourceReviewStatus: Optional[str] = None
    analysisScope: Optional[str] = None


class AiTaskDispatchMessage(BaseModel):
    messageId: str
    traceId: str
    taskId: str
    eventId: Optional[str] = None
    messageType: str
    sourceService: str
    targetService: Optional[str] = None
    tenantId: Optional[str] = None
    bizKey: Optional[str] = None
    timestamp: int
    version: str = '1.0'
    retryCount: int = 0
    payload: AiTaskDispatchPayload


class AiTaskMessageEnvelope(BaseModel):
    messageId: str
    traceId: str
    taskId: str
    eventId: Optional[str] = None
    messageType: str
    sourceService: str
    targetService: Optional[str] = None
    tenantId: Optional[str] = None
    bizKey: Optional[str] = None
    timestamp: int
    version: str = '1.0'
    retryCount: int = 0


class AiTaskStatusPayload(BaseModel):
    workflowInstanceId: Optional[str] = None
    status: str
    currentStage: str
    currentNode: str
    progress: int


class AiTaskStatusMessage(AiTaskMessageEnvelope):
    payload: AiTaskStatusPayload


class AiTaskResultPayload(BaseModel):
    workflowInstanceId: Optional[str] = None
    taskType: str
    taskTitle: Optional[str] = None
    analysisScope: Optional[str] = None
    targetType: Optional[str] = None
    targetCode: Optional[str] = None
    targetName: Optional[str] = None
    priority: Optional[str] = None
    sourceTaskId: Optional[str] = None
    sourceReportId: Optional[str] = None
    sourceEventId: Optional[str] = None
    sourceDomain: Optional[str] = None
    sourceReviewStatus: Optional[str] = None
    finalStatus: str
    finalStage: Optional[str] = None
    summary: str
    confidenceScore: float
    needHumanReview: bool
    riskWarnings: list[str]
    reportMeta: dict[str, Any]
    resultRef: str


class AiTaskResultMessage(AiTaskMessageEnvelope):
    payload: AiTaskResultPayload


class AgentAuditItem(BaseModel):
    executionId: str
    agentCode: str
    agentName: str
    nodeCode: str
    status: str
    confidenceScore: float | None = None
    needHumanReview: bool = False
    startTimestamp: int | None = None
    finishTimestamp: int | None = None
    durationMs: int | None = None


class AiTaskAuditPayload(BaseModel):
    workflowInstanceId: Optional[str] = None
    agents: list[AgentAuditItem | dict[str, Any]]
    reviewSuggestion: str
    evidenceRefs: list[str]


class AiTaskAuditMessage(AiTaskMessageEnvelope):
    payload: AiTaskAuditPayload
