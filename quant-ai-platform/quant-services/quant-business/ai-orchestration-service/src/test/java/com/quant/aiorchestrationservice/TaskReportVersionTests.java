package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportReviewLogDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchReportReviewLogMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.aiorchestrator.service.impl.TaskReportServiceImpl;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskReportVersionTests {

    @Test
    void reviewReportIncrementsVersionAndCreatesSnapshot() {
        TestDeps deps = new TestDeps();
        ResearchReportDO report = buildReport(1);
        ResearchReportSectionDO section = buildSection();
        when(deps.reportMapper.selectOne(any())).thenReturn(report);
        when(deps.sectionMapper.selectList(any())).thenReturn(List.of(section));

        newService(deps).reviewReport("task-1", buildReviewDTO(ReportReviewStatusEnum.APPROVED.name()));

        ArgumentCaptor<ResearchReportDO> reportCaptor = ArgumentCaptor.forClass(ResearchReportDO.class);
        verify(deps.reportMapper).updateById(reportCaptor.capture());
        assertEquals(2, reportCaptor.getValue().getVersionNo());
        assertEquals("APPROVED", reportCaptor.getValue().getReviewStatus());

        ArgumentCaptor<ResearchReportReviewLogDO> logCaptor = ArgumentCaptor.forClass(ResearchReportReviewLogDO.class);
        verify(deps.reviewLogMapper).insert(logCaptor.capture());
        assertEquals(2, logCaptor.getValue().getVersionNo());

        ArgumentCaptor<ResearchReportSectionDO> sectionCaptor = ArgumentCaptor.forClass(ResearchReportSectionDO.class);
        verify(deps.sectionMapper).updateById(sectionCaptor.capture());
        assertEquals(2, sectionCaptor.getValue().getVersionNo());
        assertEquals("review summary", sectionCaptor.getValue().getRevisedContent());
        verify(deps.reportVersionService).createSnapshot(report, "REPORT_REVIEW");
    }

    @Test
    void invalidReviewStatusDoesNotCreateSnapshot() {
        TestDeps deps = new TestDeps();
        when(deps.reportMapper.selectOne(any())).thenReturn(buildReport(1));

        assertThrows(BizException.class, () -> newService(deps).reviewReport("task-1", buildReviewDTO("INVALID")));

        verify(deps.reportVersionService, never()).createSnapshot(any(), any());
        verify(deps.reportMapper, never()).updateById(any(ResearchReportDO.class));
    }

    private static TaskReportServiceImpl newService(TestDeps deps) {
        return new TaskReportServiceImpl(
                deps.reportMapper,
                new ObjectMapper().findAndRegisterModules(),
                deps.reviewLogMapper,
                deps.humanReviewRecordMapper,
                deps.sectionMapper,
                deps.redisTemplate,
                deps.versionManager,
                deps.reportVersionService
        );
    }

    private static ResearchReportDO buildReport(int versionNo) {
        ResearchReportDO report = new ResearchReportDO();
        report.setReportId("report-1");
        report.setTaskId("task-1");
        report.setVersionNo(versionNo);
        report.setReviewStatus("PENDING");
        report.setHighlights("[\"old highlight\"]");
        report.setRiskPoints("[\"old risk\"]");
        return report;
    }

    private static ResearchReportSectionDO buildSection() {
        ResearchReportSectionDO section = new ResearchReportSectionDO();
        section.setSectionId("section-1");
        section.setReportId("report-1");
        section.setTaskId("task-1");
        section.setVersionNo(1);
        section.setSectionCode("SUMMARY");
        section.setDeleted(0);
        return section;
    }

    private static TaskReportReviewDTO buildReviewDTO(String status) {
        TaskReportReviewDTO dto = new TaskReportReviewDTO();
        dto.setReviewStatus(status);
        dto.setReviewedBy("analyst-1");
        dto.setRevisedSummary("review summary");
        dto.setRevisedHighlights(List.of("review highlight"));
        dto.setRevisedRiskPoints(List.of("review risk"));
        dto.setReviewComment("review comment");
        return dto;
    }

    private static final class TestDeps {
        private final ResearchReportMapper reportMapper = mock(ResearchReportMapper.class);
        private final ResearchReportReviewLogMapper reviewLogMapper = mock(ResearchReportReviewLogMapper.class);
        private final HumanReviewRecordMapper humanReviewRecordMapper = mock(HumanReviewRecordMapper.class);
        private final ResearchReportSectionMapper sectionMapper = mock(ResearchReportSectionMapper.class);
        private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        private final TaskCacheVersionManager versionManager = mock(TaskCacheVersionManager.class);
        private final ReportVersionService reportVersionService = mock(ReportVersionService.class);
    }
}
