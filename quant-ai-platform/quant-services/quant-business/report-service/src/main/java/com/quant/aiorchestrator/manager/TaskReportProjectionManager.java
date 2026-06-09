package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.report.TaskReportProjectionProvider;
import com.quant.aiorchestrator.report.TaskReportRiskDetailProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjection;
import com.quant.aiorchestrator.report.TaskReportRiskProjectionProvider;
import com.quant.common.redis.RedisKeyBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class TaskReportProjectionManager implements TaskReportProjectionProvider {

    private final ReportTaskPageReadManager reportReadManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskReportDomainHydrationManager domainHydrationManager;
    private final TaskReportRiskProjectionProvider riskProjectionProvider;
    private final TaskReportItemAssembler itemAssembler;
    private final TaskReportContextHydrationManager contextHydrationManager;

    @Autowired
    public TaskReportProjectionManager(ReportTaskPageReadManager reportReadManager,
                                       StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       TaskReportDomainHydrationManager domainHydrationManager,
                                       TaskReportRiskProjectionProvider riskProjectionProvider) {
        this(
                reportReadManager,
                stringRedisTemplate,
                objectMapper,
                domainHydrationManager,
                riskProjectionProvider,
                new TaskReportItemAssembler(objectMapper),
                new TaskReportContextHydrationManager(objectMapper)
        );
    }

    public TaskReportProjectionManager(ReportTaskPageReadManager reportReadManager,
                                       StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       TaskReportDomainHydrationManager domainHydrationManager,
                                       TaskReportRiskProjectionProvider riskProjectionProvider,
                                       TaskReportItemAssembler itemAssembler,
                                       TaskReportContextHydrationManager contextHydrationManager) {
        this.reportReadManager = reportReadManager;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.domainHydrationManager = domainHydrationManager;
        this.riskProjectionProvider = riskProjectionProvider;
        this.itemAssembler = itemAssembler;
        this.contextHydrationManager = contextHydrationManager;
    }

    @Override
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

        ResearchReportDO report = reportReadManager.selectCurrentReportByTaskId(taskId);
        if (report == null) {
            return null;
        }

        TaskReportRiskProjection warning = riskProjectionProvider.loadLatestRiskWarningMapByTaskIds(Set.of(taskId)).get(taskId);
        List<TaskReportRiskDetailProjection> warningDetails = warning == null
                ? List.of()
                : riskProjectionProvider.loadRiskWarningDetailMapByWarningIds(Set.of(warning.warningId()))
                .getOrDefault(warning.warningId(), List.of());

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
                                        TaskReportRiskProjection warning,
                                        List<TaskReportRiskDetailProjection> warningDetails) {
        TaskReportVO vo = itemAssembler.toTaskReportVO(report, warning, warningDetails);
        contextHydrationManager.hydrateTaskReportContextFields(vo);
        domainHydrationManager.hydrateTaskReportDomainFields(vo);
        return vo;
    }

    private boolean isCurrentTaskReportCache(TaskReportVO cached) {
        return cached != null && cached.getReportId() != null && !cached.getReportId().isBlank();
    }
}
