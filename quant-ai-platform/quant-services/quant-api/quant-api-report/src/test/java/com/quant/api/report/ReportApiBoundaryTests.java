package com.quant.api.report;

import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.vo.ReportCenterPageVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.report.StableReportContract;
import com.quant.aiorchestrator.service.ReportQueryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportApiBoundaryTests {

    @Test
    void reportApiOwnsStableReportDtosAndVos() {
        assertEquals("quant-api-report", ReportApiBoundary.MODULE);
        assertEquals("/api/reports", StableReportContract.API_BASE);
        assertEquals(ReportCenterPageQueryDTO.class.getPackageName(), TaskReportReviewDTO.class.getPackageName());
        assertEquals(ReportCenterPageVO.class.getPackageName(), ReportVersionVO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", TaskReportVO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", ReportQueryService.class.getPackageName());
    }
}
