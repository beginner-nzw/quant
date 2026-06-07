package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.TaskPageVO;
import com.quant.aiorchestrator.util.CacheKeyUtil;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TaskPageProjectionManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final TaskQueryReadManager taskQueryReadManager;
    private final TaskQueryItemAssembler itemAssembler;

    public TaskPageProjectionManager(StringRedisTemplate stringRedisTemplate,
                                     ObjectMapper objectMapper,
                                     TaskCacheVersionManager taskCacheVersionManager,
                                     TaskQueryReadManager taskQueryReadManager,
                                     TaskQueryItemAssembler itemAssembler) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.taskCacheVersionManager = taskCacheVersionManager;
        this.taskQueryReadManager = taskQueryReadManager;
        this.itemAssembler = itemAssembler;
    }

    public TaskPageVO pageTasks(TaskPageQueryDTO queryDTO) {
        int pageNum = queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1 ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : queryDTO.getPageSize();

        String queryFingerprint = String.format(
                "pageNum=%d&pageSize=%d&taskType=%s&status=%s&targetCode=%s&targetName=%s&onlyFailed=%s&hasRetry=%s&onlyPendingReview=%s&reportReviewStatus=%s&reportReviewedBy=%s",
                pageNum,
                pageSize,
                queryDTO.getTaskType(),
                queryDTO.getStatus(),
                queryDTO.getTargetCode(),
                queryDTO.getTargetName(),
                queryDTO.getOnlyFailed(),
                queryDTO.getHasRetry(),
                queryDTO.getOnlyPendingReview(),
                queryDTO.getReportReviewStatus(),
                queryDTO.getReportReviewedBy()
        );

        String cacheKey = RedisKeyConstants.TASK_LIST_CACHE_PREFIX + taskCacheVersionManager.currentVersion() + ":" +
                CacheKeyUtil.md5(queryFingerprint);
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                return objectMapper.readValue(cache, TaskPageVO.class);
            } catch (Exception ignored) {
            }
        }

        LambdaQueryWrapper<ResearchReportDO> reportWrapper = new LambdaQueryWrapper<ResearchReportDO>()
                .eq(ResearchReportDO::getDeleted, 0);

        boolean needReportFilter = false;

        String targetReviewStatus;
        if (Boolean.TRUE.equals(queryDTO.getOnlyPendingReview())) {
            targetReviewStatus = ReportReviewStatusEnum.PENDING.name();
        } else if (queryDTO.getReportReviewStatus() != null && !queryDTO.getReportReviewStatus().isBlank()) {
            targetReviewStatus = queryDTO.getReportReviewStatus();
        } else {
            targetReviewStatus = null;
        }

        if (targetReviewStatus != null) {
            if (ReportReviewStatusEnum.PENDING.name().equals(targetReviewStatus)) {
                reportWrapper.and(wrapper -> wrapper
                        .isNull(ResearchReportDO::getReviewStatus)
                        .or()
                        .eq(ResearchReportDO::getReviewStatus, targetReviewStatus));
            } else {
                reportWrapper.eq(ResearchReportDO::getReviewStatus, targetReviewStatus);
            }
            needReportFilter = true;
        }

        if (queryDTO.getReportReviewedBy() != null && !queryDTO.getReportReviewedBy().isBlank()) {
            reportWrapper.like(ResearchReportDO::getReviewedBy, queryDTO.getReportReviewedBy());
            needReportFilter = true;
        }

        Set<String> reportFilteredTaskIds = null;
        if (needReportFilter) {
            var reports = taskQueryReadManager.listReports(reportWrapper);

            reportFilteredTaskIds = reports.stream()
                    .map(ResearchReportDO::getTaskId)
                    .collect(Collectors.toSet());

            if (reportFilteredTaskIds.isEmpty()) {
                TaskPageVO empty = new TaskPageVO();
                empty.setTotal(0L);
                empty.setPageNum((long) pageNum);
                empty.setPageSize((long) pageSize);
                empty.setRecords(List.of());
                return empty;
            }
        }

        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0)
                .orderByDesc(ResearchTaskDO::getId);
        if (reportFilteredTaskIds != null) {
            wrapper.in(ResearchTaskDO::getTaskId, reportFilteredTaskIds);
        }
        if (queryDTO.getTaskType() != null && !queryDTO.getTaskType().isBlank()) {
            wrapper.eq(ResearchTaskDO::getTaskType, queryDTO.getTaskType());
        }
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isBlank()) {
            wrapper.eq(ResearchTaskDO::getStatus, queryDTO.getStatus());
        }
        if (Boolean.TRUE.equals(queryDTO.getOnlyFailed())) {
            wrapper.eq(ResearchTaskDO::getStatus, TaskStatusEnum.FAILED.name());
        }
        if (queryDTO.getTargetCode() != null && !queryDTO.getTargetCode().isBlank()) {
            wrapper.eq(ResearchTaskDO::getTargetCode, queryDTO.getTargetCode());
        }
        if (queryDTO.getTargetName() != null && !queryDTO.getTargetName().isBlank()) {
            wrapper.like(ResearchTaskDO::getTargetName, queryDTO.getTargetName());
        }
        if (Boolean.TRUE.equals(queryDTO.getHasRetry())) {
            wrapper.gt(ResearchTaskDO::getRetryCount, 0);
        }

        Page<ResearchTaskDO> page =
                new Page<>(pageNum, pageSize);

        Page<ResearchTaskDO> result =
                taskQueryReadManager.pageTasks(page, wrapper);

        TaskPageVO vo = new TaskPageVO();
        vo.setTotal(result.getTotal());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        Map<String, ResearchReportDO> reportMap = Collections.emptyMap();
        if (!result.getRecords().isEmpty()) {
            var taskIds = result.getRecords().stream().map(ResearchTaskDO::getTaskId).toList();
            var reports = taskQueryReadManager.listReports(
                    new LambdaQueryWrapper<ResearchReportDO>()
                            .eq(ResearchReportDO::getDeleted, 0)
                            .in(ResearchReportDO::getTaskId, taskIds)
            );

            reportMap = reports.stream().collect(
                    java.util.stream.Collectors.toMap(
                            ResearchReportDO::getTaskId,
                            item -> item,
                            (a, b) -> a
                    )
            );
        }

        Map<String, ResearchReportDO> finalReportMap = reportMap;
        vo.setRecords(result.getRecords().stream()
                .map(task -> itemAssembler.toTaskListItemVO(task, finalReportMap.get(task.getTaskId())))
                .toList());

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    Duration.ofSeconds(20)
            );
        } catch (Exception ignored) {
        }

        return vo;
    }
}
