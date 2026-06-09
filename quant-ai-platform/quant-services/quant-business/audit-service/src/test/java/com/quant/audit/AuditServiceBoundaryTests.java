package com.quant.audit;

import com.quant.aiorchestrator.controller.AuditComplianceController;
import com.quant.aiorchestrator.controller.HumanReviewController;
import com.quant.aiorchestrator.consumer.AiTaskAuditConsumer;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.domain.dto.HumanReviewQueueQueryDTO;
import com.quant.aiorchestrator.domain.entity.AiPromptAuditDO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.entity.HumanReviewRecordDO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceListItemVO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueItemVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueuePageVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueStatsVO;
import com.quant.aiorchestrator.mapper.AiPromptAuditMapper;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import com.quant.aiorchestrator.manager.AiTaskAuditRecordWriteManager;
import com.quant.aiorchestrator.manager.TaskControlAuditRecordWriteManager;
import com.quant.aiorchestrator.service.AuditComplianceQueryService;
import com.quant.aiorchestrator.service.HumanReviewService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditServiceBoundaryTests {

    @Test
    void auditServiceOwnsAuditAndHumanReviewPersistenceTypes() {
        assertEquals("com.quant.aiorchestrator.domain.entity", AuditRecordDO.class.getPackageName());
        assertEquals(AuditRecordDO.class.getPackageName(), AiPromptAuditDO.class.getPackageName());
        assertEquals(AuditRecordDO.class.getPackageName(), HumanReviewRecordDO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.mapper", AuditRecordMapper.class.getPackageName());
        assertEquals(AuditRecordMapper.class.getPackageName(), AiPromptAuditMapper.class.getPackageName());
        assertEquals(AuditRecordMapper.class.getPackageName(), HumanReviewRecordMapper.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", AiTaskAuditRecordWriteManager.class.getPackageName());
        assertEquals(AiTaskAuditRecordWriteManager.class.getPackageName(), TaskControlAuditRecordWriteManager.class.getPackageName());
    }

    @Test
    void auditServiceOwnsAiTaskAuditConsumer() {
        assertEquals("com.quant.aiorchestrator.consumer", AiTaskAuditConsumer.class.getPackageName());
    }

    @Test
    void auditServiceOwnsAuditAndHumanReviewApiModels() {
        assertEquals("com.quant.aiorchestrator.domain.dto", AuditCompliancePageQueryDTO.class.getPackageName());
        assertEquals(AuditCompliancePageQueryDTO.class.getPackageName(), HumanReviewDecisionDTO.class.getPackageName());
        assertEquals(AuditCompliancePageQueryDTO.class.getPackageName(), HumanReviewQueueQueryDTO.class.getPackageName());

        assertEquals("com.quant.aiorchestrator.domain.vo", AuditComplianceListItemVO.class.getPackageName());
        assertEquals(AuditComplianceListItemVO.class.getPackageName(), AuditCompliancePageVO.class.getPackageName());
        assertEquals(AuditComplianceListItemVO.class.getPackageName(), AuditComplianceStatsVO.class.getPackageName());
        assertEquals(AuditComplianceListItemVO.class.getPackageName(), HumanReviewQueueItemVO.class.getPackageName());
        assertEquals(AuditComplianceListItemVO.class.getPackageName(), HumanReviewQueuePageVO.class.getPackageName());
        assertEquals(AuditComplianceListItemVO.class.getPackageName(), HumanReviewQueueStatsVO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", AuditComplianceQueryService.class.getPackageName());
        assertEquals(AuditComplianceQueryService.class.getPackageName(), HumanReviewService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.controller", AuditComplianceController.class.getPackageName());
        assertEquals(AuditComplianceController.class.getPackageName(), HumanReviewController.class.getPackageName());
    }
}
