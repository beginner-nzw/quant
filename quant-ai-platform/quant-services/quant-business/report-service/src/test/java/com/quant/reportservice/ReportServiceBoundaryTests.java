package com.quant.reportservice;

import com.quant.aiorchestrator.controller.ReportController;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportReviewLogDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportVersionDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchReportReviewLogMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchReportVersionMapper;
import com.quant.aiorchestrator.manager.AiResultReportProjectionManager;
import com.quant.aiorchestrator.manager.ReportGeneratedDomainEventManager;
import com.quant.aiorchestrator.manager.TaskReportContextHydrationManager;
import com.quant.aiorchestrator.manager.TaskReportDomainHydrationManager;
import com.quant.aiorchestrator.report.ReportDomainService;
import com.quant.aiorchestrator.report.ReportDomainServiceImpl;
import com.quant.aiorchestrator.report.ReportReviewCommand;
import com.quant.aiorchestrator.report.api.ReportDomainController;
import com.quant.aiorchestrator.service.ReportVersionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceBoundaryTests {

    @Test
    void reportServiceOwnsReportPersistenceContractsAndController() {
        assertEquals("com.quant.aiorchestrator.domain.entity", ResearchReportDO.class.getPackageName());
        assertEquals(ResearchReportDO.class.getPackageName(), ResearchReportSectionDO.class.getPackageName());
        assertEquals(ResearchReportDO.class.getPackageName(), ResearchReportVersionDO.class.getPackageName());
        assertEquals(ResearchReportDO.class.getPackageName(), ResearchReportReviewLogDO.class.getPackageName());
        assertEquals(ResearchReportDO.class.getPackageName(), ReportEvidenceRefDO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.mapper", ResearchReportMapper.class.getPackageName());
        assertEquals(ResearchReportMapper.class.getPackageName(), ResearchReportSectionMapper.class.getPackageName());
        assertEquals(ResearchReportMapper.class.getPackageName(), ResearchReportVersionMapper.class.getPackageName());
        assertEquals(ResearchReportMapper.class.getPackageName(), ResearchReportReviewLogMapper.class.getPackageName());
        assertEquals(ResearchReportMapper.class.getPackageName(), ReportEvidenceRefMapper.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.report", ReportDomainService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.report", ReportDomainServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.report", ReportReviewCommand.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.report.api", ReportDomainController.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", ReportVersionService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.controller", ReportController.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", AiResultReportProjectionManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", ReportGeneratedDomainEventManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskReportContextHydrationManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", TaskReportDomainHydrationManager.class.getPackageName());
    }
}
