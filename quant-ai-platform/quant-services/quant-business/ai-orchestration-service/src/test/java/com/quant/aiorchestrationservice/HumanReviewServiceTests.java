package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.domain.dto.HumanReviewQueueQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.domain.entity.HumanReviewRecordDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueuePageVO;
import com.quant.aiorchestrator.manager.HumanReviewCommandManager;
import com.quant.aiorchestrator.manager.HumanReviewDecisionManager;
import com.quant.aiorchestrator.manager.HumanReviewQueueManager;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.service.TaskControlService;
import com.quant.aiorchestrator.service.TaskReportService;
import com.quant.aiorchestrator.service.impl.HumanReviewServiceImpl;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanReviewServiceTests {

    @Test
    void queueProjectsReportRiskAndComplianceReviewItems() {
        Dependencies deps = new Dependencies();
        deps.tasks.add(buildTask());
        deps.reports.add(buildReport());
        deps.risks.add(buildRisk());

        HumanReviewQueuePageVO page = deps.service.pageQueue(new HumanReviewQueueQueryDTO());

        assertEquals(3, page.getTotal());
        assertTrue(page.getRecords().stream().anyMatch(item -> "REPORT:task-1".equals(item.getQueueId())));
        assertTrue(page.getRecords().stream().anyMatch(item -> "RISK:task-1".equals(item.getQueueId())));
        assertTrue(page.getRecords().stream().anyMatch(item -> "COMPLIANCE:task-1".equals(item.getQueueId())));
    }

    @Test
    void reportDecisionDelegatesToReportReviewAndCanRequestRerun() {
        Dependencies deps = new Dependencies();
        deps.tasks.add(buildTask());
        deps.reports.add(buildReport());

        HumanReviewDecisionDTO dto = new HumanReviewDecisionDTO();
        dto.setDecision(ReportReviewStatusEnum.REJECTED.name());
        dto.setReviewedBy("reviewer-1");
        dto.setReviewComment("revise evidence grounding");
        dto.setRevisedSummary("revised summary");
        dto.setRerunWorkflow(true);
        dto.setRerunNodeName("report_generation_agent");

        assertEquals("task-1", deps.service.decide("REPORT:task-1", dto));

        assertNotNull(deps.taskReportService.lastReview);
        assertEquals(ReportReviewStatusEnum.REJECTED.name(), deps.taskReportService.lastReview.getReviewStatus());
        assertEquals("revised summary", deps.taskReportService.lastReview.getRevisedSummary());
        assertEquals("task-1", deps.taskControlService.rerunTaskId);
        assertEquals("report_generation_agent", deps.taskControlService.rerunDto.getNodeName());
    }

    @Test
    void riskDecisionUpdatesRiskAndWritesHumanReviewRecord() {
        Dependencies deps = new Dependencies();
        deps.tasks.add(buildTask());
        deps.risks.add(buildRisk());

        HumanReviewDecisionDTO dto = new HumanReviewDecisionDTO();
        dto.setDecision(ReportReviewStatusEnum.APPROVED.name());
        dto.setReviewedBy("risk-reviewer");
        dto.setReviewComment("accepted after manual review");

        assertEquals("task-1", deps.service.decide("RISK:task-1", dto));

        assertEquals(ReportReviewStatusEnum.APPROVED.name(), deps.risks.get(0).getReviewStatus());
        assertEquals("risk-reviewer", deps.risks.get(0).getReviewerId());
        assertEquals(1, deps.insertedReviews.size());
        HumanReviewRecordDO record = deps.insertedReviews.get(0);
        assertEquals("RISK_WARNING", record.getRelatedObjectType());
        assertEquals("warning-1", record.getRelatedObjectId());
        assertEquals(ReportReviewStatusEnum.APPROVED.name(), record.getReviewResult());
    }

    private static ResearchTaskDO buildTask() {
        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId("task-1");
        task.setTaskTitle("Review task");
        task.setTaskType("EQUITY_RESEARCH");
        task.setTargetCode("000001");
        task.setTargetName("Ping An Bank");
        task.setPriority("HIGH");
        task.setDeleted(0);
        return task;
    }

    private static ResearchReportDO buildReport() {
        ResearchReportDO report = new ResearchReportDO();
        report.setReportId("report-1");
        report.setTaskId("task-1");
        report.setTaskType("EQUITY_RESEARCH");
        report.setReportType("EQUITY_RESEARCH");
        report.setSummary("summary requiring human review");
        report.setRiskWarnings("[\"risk warning\"]");
        report.setRiskPoints("[\"risk point\"]");
        report.setNeedHumanReview(1);
        report.setReviewStatus(ReportReviewStatusEnum.PENDING.name());
        report.setCreatedAt(LocalDateTime.now());
        report.setDeleted(0);
        return report;
    }

    private static RiskWarningDO buildRisk() {
        RiskWarningDO risk = new RiskWarningDO();
        risk.setWarningId("warning-1");
        risk.setTaskId("task-1");
        risk.setWarningLevel("HIGH");
        risk.setWarningSummary("risk summary");
        risk.setWarningReason("risk point");
        risk.setSuggestAction("NEED_HUMAN_REVIEW");
        risk.setReviewStatus(ReportReviewStatusEnum.PENDING.name());
        risk.setTenantId("tenant-1");
        risk.setCreatedAt(LocalDateTime.now());
        risk.setDeleted(0);
        return risk;
    }

    private static class Dependencies {
        private final List<ResearchTaskDO> tasks = new ArrayList<>();
        private final List<ResearchReportDO> reports = new ArrayList<>();
        private final List<RiskWarningDO> risks = new ArrayList<>();
        private final List<HumanReviewRecordDO> insertedReviews = new ArrayList<>();
        private final FakeTaskReportService taskReportService = new FakeTaskReportService();
        private final FakeTaskControlService taskControlService = new FakeTaskControlService();
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final ResearchTaskMapper researchTaskMapper = mapperProxy(ResearchTaskMapper.class, tasks);
        private final ResearchReportMapper researchReportMapper = mapperProxy(ResearchReportMapper.class, reports);
        private final RiskWarningMapper riskWarningMapper = mapperProxy(RiskWarningMapper.class, risks);
        private final HumanReviewQueueManager queueManager = new HumanReviewQueueManager(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                objectMapper
        );
        private final HumanReviewDecisionManager decisionManager = new HumanReviewDecisionManager(
                riskWarningMapper,
                humanReviewMapperProxy(insertedReviews),
                taskReportService,
                taskControlService,
                new FakeStringRedisTemplate(),
                objectMapper
        );
        private final HumanReviewCommandManager commandManager = new HumanReviewCommandManager(decisionManager);
        private final HumanReviewServiceImpl service = new HumanReviewServiceImpl(
                queueManager,
                commandManager
        );
    }

    private static class FakeStringRedisTemplate extends StringRedisTemplate {
        @Override
        public Boolean delete(String key) {
            return true;
        }
    }

    private static class FakeTaskReportService implements TaskReportService {
        private TaskReportReviewDTO lastReview;

        @Override
        public String reviewReport(String taskId, TaskReportReviewDTO dto) {
            lastReview = dto;
            return taskId;
        }

        @Override
        public List<com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO> listReviewLogs(String taskId) {
            return List.of();
        }
    }

    private static class FakeTaskControlService implements TaskControlService {
        private String rerunTaskId;
        private TaskWorkflowControlDTO rerunDto;

        @Override
        public String cancelTask(String taskId, com.quant.aiorchestrator.domain.dto.TaskCancelDTO dto) {
            return taskId;
        }

        @Override
        public String resumeTask(String taskId, TaskWorkflowControlDTO dto) {
            return taskId;
        }

        @Override
        public String rerunNode(String taskId, TaskWorkflowControlDTO dto) {
            rerunTaskId = taskId;
            rerunDto = dto;
            return taskId;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapperProxy(Class<T> mapperType, List<?> rows) {
        return (T) Proxy.newProxyInstance(
                mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                (proxy, method, args) -> {
                    if (method.getName().equals("selectList")) {
                        return rows;
                    }
                    if (method.getName().equals("selectOne")) {
                        return rows.isEmpty() ? null : rows.get(0);
                    }
                    if (method.getName().equals("updateById")) {
                        return 1;
                    }
                    return defaultReturn(method.getReturnType());
                }
        );
    }

    private static HumanReviewRecordMapper humanReviewMapperProxy(List<HumanReviewRecordDO> inserted) {
        return (HumanReviewRecordMapper) Proxy.newProxyInstance(
                HumanReviewRecordMapper.class.getClassLoader(),
                new Class<?>[]{HumanReviewRecordMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("insert")) {
                        inserted.add((HumanReviewRecordDO) args[0]);
                        return 1;
                    }
                    return defaultReturn(method.getReturnType());
                }
        );
    }

    private static Object defaultReturn(Class<?> returnType) {
        if (returnType.equals(int.class) || returnType.equals(Integer.class)) {
            return 0;
        }
        if (returnType.equals(long.class) || returnType.equals(Long.class)) {
            return 0L;
        }
        if (returnType.equals(boolean.class) || returnType.equals(Boolean.class)) {
            return false;
        }
        return null;
    }
}
