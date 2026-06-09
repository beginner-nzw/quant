package com.quant.report;

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
import com.quant.aiorchestrator.manager.ReportReviewStatsManager;
import com.quant.aiorchestrator.report.ReportDomainOwnership;
import com.quant.aiorchestrator.report.ReportDomainService;
import com.quant.aiorchestrator.service.AiResultReportService;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.aiorchestrator.service.impl.AiResultReportServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceBoundaryTests {

    @Test
    void reportServiceOwnsReportPersistenceAndVersionRuntimeTypes() {
        assertEquals("report-service", ReportServiceBoundary.MODULE);
        assertEquals("com.quant.aiorchestrator.domain.entity", ResearchReportDO.class.getPackageName());
        assertEquals(ResearchReportDO.class.getPackageName(), ResearchReportVersionDO.class.getPackageName());
        assertEquals(ResearchReportDO.class.getPackageName(), ResearchReportSectionDO.class.getPackageName());
        assertEquals(ResearchReportDO.class.getPackageName(), ResearchReportReviewLogDO.class.getPackageName());
        assertEquals(ResearchReportDO.class.getPackageName(), ReportEvidenceRefDO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.mapper", ResearchReportMapper.class.getPackageName());
        assertEquals(ResearchReportMapper.class.getPackageName(), ResearchReportVersionMapper.class.getPackageName());
        assertEquals(ResearchReportMapper.class.getPackageName(), ResearchReportSectionMapper.class.getPackageName());
        assertEquals(ResearchReportMapper.class.getPackageName(), ResearchReportReviewLogMapper.class.getPackageName());
        assertEquals(ResearchReportMapper.class.getPackageName(), ReportEvidenceRefMapper.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", ReportReviewStatsManager.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", ReportVersionService.class.getPackageName());
        assertEquals(ReportVersionService.class.getPackageName(), AiResultReportService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", AiResultReportServiceImpl.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.report", ReportDomainService.class.getPackageName());
        assertEquals("report-service", ReportDomainOwnership.CURRENT_HOST);
    }
}
