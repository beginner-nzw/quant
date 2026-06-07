package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.HumanReviewRecordDO;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.common.redis.RedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class TaskReportProjectionManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskReportDomainHydrationManager domainHydrationManager;
    private final TaskReportRiskReadManager riskReadManager;
    private final TaskReportItemAssembler itemAssembler;
    private final TaskReportContextHydrationManager contextHydrationManager;

    public TaskReportProjectionManager(ResearchTaskMapper researchTaskMapper,
                                       ResearchReportMapper researchReportMapper,
                                       StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       RiskWarningMapper riskWarningMapper,
                                       RiskWarningDetailMapper riskWarningDetailMapper,
                                       StrategySignalMapper strategySignalMapper,
                                       TaskReportDomainHydrationManager domainHydrationManager) {
        this(
                researchTaskMapper,
                researchReportMapper,
                stringRedisTemplate,
                objectMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                strategySignalMapper,
                domainHydrationManager,
                new TaskReportRiskReadManager(riskWarningMapper, riskWarningDetailMapper),
                new TaskReportItemAssembler(objectMapper),
                new TaskReportContextHydrationManager(objectMapper)
        );
    }

    public TaskReportProjectionManager(ResearchTaskMapper researchTaskMapper,
                                       ResearchReportMapper researchReportMapper,
                                       StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       RiskWarningMapper riskWarningMapper,
                                       RiskWarningDetailMapper riskWarningDetailMapper,
                                       StrategySignalMapper strategySignalMapper,
                                       TaskReportDomainHydrationManager domainHydrationManager,
                                       TaskReportRiskReadManager riskReadManager) {
        this(
                researchTaskMapper,
                researchReportMapper,
                stringRedisTemplate,
                objectMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                strategySignalMapper,
                domainHydrationManager,
                riskReadManager,
                new TaskReportItemAssembler(objectMapper),
                new TaskReportContextHydrationManager(objectMapper)
        );
    }

    public TaskReportProjectionManager(ResearchTaskMapper researchTaskMapper,
                                       ResearchReportMapper researchReportMapper,
                                       StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       RiskWarningMapper riskWarningMapper,
                                       RiskWarningDetailMapper riskWarningDetailMapper,
                                       StrategySignalMapper strategySignalMapper,
                                       TaskReportDomainHydrationManager domainHydrationManager,
                                       TaskReportRiskReadManager riskReadManager,
                                       TaskReportItemAssembler itemAssembler,
                                       TaskReportContextHydrationManager contextHydrationManager) {
        this.researchTaskMapper = researchTaskMapper;
        this.researchReportMapper = researchReportMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.domainHydrationManager = domainHydrationManager;
        this.riskReadManager = riskReadManager;
        this.itemAssembler = itemAssembler;
        this.contextHydrationManager = contextHydrationManager;
    }

    public TaskReportProjectionManager(ResearchTaskMapper researchTaskMapper,
                                       ResearchReportMapper researchReportMapper,
                                       StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       RiskWarningMapper riskWarningMapper,
                                       RiskWarningDetailMapper riskWarningDetailMapper,
                                       StrategySignalMapper strategySignalMapper,
                                       ReportEvidenceRefMapper reportEvidenceRefMapper,
                                       HumanReviewRecordMapper humanReviewRecordMapper,
                                       ResearchReportSectionMapper researchReportSectionMapper) {
        this(
                researchTaskMapper,
                researchReportMapper,
                stringRedisTemplate,
                objectMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                strategySignalMapper,
                new TaskReportDomainHydrationManager(
                        reportEvidenceRefMapper,
                        humanReviewRecordMapper,
                        researchReportSectionMapper,
                        objectMapper
                )
        );
    }

public TaskReportVO getTaskReportOnly(String taskId) {
        String cacheKey = RedisKeyBuilder.taskResult(taskId);
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                TaskReportVO cached = objectMapper.readValue(cache, TaskReportVO.class);
                boolean upgraded = contextHydrationManager.hydrateTaskReportContextFields(cached);
                upgraded = domainHydrationManager.hydrateTaskReportDomainFields(cached) || upgraded;
                if (isCurrentTaskReportCache(cached)) {
                    if (upgraded) {
                        try {
                            stringRedisTemplate.opsForValue().set(
                                    cacheKey,
                                    objectMapper.writeValueAsString(cached),
                                    java.time.Duration.ofHours(12)
                            );
                        } catch (Exception ignored) {
                        }
                    }
                    return cached;
                }
            } catch (Exception ignored) {
            }
        }

        ResearchReportDO report = researchReportMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getTaskId, taskId)
                        .eq(ResearchReportDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (report == null) {
            return null;
        }

        RiskWarningDO warning = riskReadManager.loadLatestRiskWarningMapByTaskIds(Set.of(taskId)).get(taskId);
        List<RiskWarningDetailDO> warningDetails = warning == null
                ? List.of()
                : riskReadManager.loadRiskWarningDetailMapByWarningIds(Set.of(warning.getWarningId()))
                .getOrDefault(warning.getWarningId(), List.of());

        TaskReportVO vo = toTaskReportVO(report, warning, warningDetails);
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    java.time.Duration.ofHours(12)
            );
        } catch (Exception ignored) {
        }
        return vo;
    }

    private TaskReportVO toTaskReportVO(ResearchReportDO report,
                                        RiskWarningDO warning,
                                        List<RiskWarningDetailDO> warningDetails) {
        TaskReportVO vo = itemAssembler.toTaskReportVO(report, warning, warningDetails);
        contextHydrationManager.hydrateTaskReportContextFields(vo);
        domainHydrationManager.hydrateTaskReportDomainFields(vo);
        return vo;
    }

    private boolean isCurrentTaskReportCache(TaskReportVO cached) {
        return cached != null && cached.getReportId() != null && !cached.getReportId().isBlank();
    }
}
