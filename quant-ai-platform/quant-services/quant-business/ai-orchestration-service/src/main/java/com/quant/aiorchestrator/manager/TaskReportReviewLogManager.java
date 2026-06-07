package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportReviewLogDO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.mapper.ResearchReportReviewLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskReportReviewLogManager {

    private final ObjectMapper objectMapper;
    private final ResearchReportReviewLogMapper researchReportReviewLogMapper;

    public List<TaskReportReviewLogVO> listReviewLogs(String taskId) {
        return researchReportReviewLogMapper.selectList(
                new LambdaQueryWrapper<ResearchReportReviewLogDO>()
                        .eq(ResearchReportReviewLogDO::getTaskId, taskId)
                        .eq(ResearchReportReviewLogDO::getDeleted, 0)
                        .orderByDesc(ResearchReportReviewLogDO::getId)
        ).stream().map(this::toReviewLogVO).toList();
    }

    private TaskReportReviewLogVO toReviewLogVO(ResearchReportReviewLogDO item) {
        TaskReportReviewLogVO vo = new TaskReportReviewLogVO();
        vo.setReviewLogId(item.getReviewLogId());
        vo.setReportId(item.getReportId());
        vo.setTaskId(item.getTaskId());
        vo.setVersionNo(defaultVersionNo(item.getVersionNo()));
        vo.setReviewStatus(item.getReviewStatus());
        vo.setReviewedBy(item.getReviewedBy());
        vo.setReviewComment(item.getReviewComment());
        vo.setRevisedSummary(item.getRevisedSummary());
        vo.setCreatedAt(item.getCreatedAt() == null ? null : item.getCreatedAt().toString());
        vo.setRevisedHighlights(readStringList(item.getRevisedHighlights()));
        vo.setRevisedRiskPoints(readStringList(item.getRevisedRiskPoints()));
        return vo;
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }
}
