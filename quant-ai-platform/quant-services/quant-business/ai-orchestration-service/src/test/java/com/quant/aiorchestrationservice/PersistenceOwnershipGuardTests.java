package com.quant.aiorchestrationservice;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiPromptAuditDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.entity.HumanReviewRecordDO;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportReviewLogDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskStepDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.domain.entity.TaskMessageLogDO;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiPromptAuditMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import com.quant.aiorchestrator.mapper.MarketEventAnalysisMapper;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.MarketEventRelationMapper;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchReportReviewLogMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.mapper.TaskMessageLogMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceOwnershipGuardTests {

    @Test
    void orchestrationSideTablesRemainMappedToAiOrchestrationEntitiesAndMappers() {
        assertTableMapping("task_message_log", TaskMessageLogDO.class, TaskMessageLogMapper.class);
        assertTableMapping("research_task", ResearchTaskDO.class, ResearchTaskMapper.class);
        assertTableMapping("research_task_step", ResearchTaskStepDO.class, ResearchTaskStepMapper.class);
        assertTableMapping("research_task_retry_log", ResearchTaskRetryLogDO.class, ResearchTaskRetryLogMapper.class);

        assertTableMapping("ai_workflow_instance", AiWorkflowInstanceDO.class, AiWorkflowInstanceMapper.class);
        assertTableMapping("ai_agent_execution", AiAgentExecutionDO.class, AiAgentExecutionMapper.class);
        assertTableMapping("audit_record", AuditRecordDO.class, AuditRecordMapper.class);
        assertTableMapping("ai_prompt_audit", AiPromptAuditDO.class, AiPromptAuditMapper.class);
        assertTableMapping("human_review_record", HumanReviewRecordDO.class, HumanReviewRecordMapper.class);

        assertTableMapping("research_report", ResearchReportDO.class, ResearchReportMapper.class);
        assertTableMapping("research_report_section", ResearchReportSectionDO.class, ResearchReportSectionMapper.class);
        assertTableMapping("research_report_review_log", ResearchReportReviewLogDO.class, ResearchReportReviewLogMapper.class);
        assertTableMapping("report_evidence_ref", ReportEvidenceRefDO.class, ReportEvidenceRefMapper.class);

        assertTableMapping("market_event", MarketEventDO.class, MarketEventMapper.class);
        assertTableMapping("market_event_relation", MarketEventRelationDO.class, MarketEventRelationMapper.class);
        assertTableMapping("market_event_analysis", MarketEventAnalysisDO.class, MarketEventAnalysisMapper.class);

        assertTableMapping("risk_warning", RiskWarningDO.class, RiskWarningMapper.class);
        assertTableMapping("risk_warning_detail", RiskWarningDetailDO.class, RiskWarningDetailMapper.class);
        assertTableMapping("strategy_signal", StrategySignalDO.class, StrategySignalMapper.class);
        assertTableMapping("strategy_signal_factor", StrategySignalFactorDO.class, StrategySignalFactorMapper.class);
    }

    private void assertTableMapping(
            String tableName,
            Class<?> entityClass,
            Class<? extends BaseMapper<?>> mapperClass
    ) {
        TableName annotation = entityClass.getAnnotation(TableName.class);

        assertEquals(tableName, annotation.value());
        assertEquals(entityClass, baseMapperEntityClass(mapperClass));
    }

    private Class<?> baseMapperEntityClass(Class<? extends BaseMapper<?>> mapperClass) {
        return Arrays.stream(mapperClass.getGenericInterfaces())
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(type -> BaseMapper.class.equals(type.getRawType()))
                .map(type -> type.getActualTypeArguments()[0])
                .map(this::asClass)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No BaseMapper<T> generic found for " + mapperClass.getName()));
    }

    private Class<?> asClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        throw new AssertionError("BaseMapper<T> generic is not a class: " + type.getTypeName());
    }
}
