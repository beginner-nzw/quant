package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportVersionDO;
import com.quant.aiorchestrator.mapper.ResearchReportVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportVersionStoreManager {

    private final ResearchReportVersionMapper researchReportVersionMapper;

    public ResearchReportVersionDO selectByReportAndVersion(String reportId, Integer versionNo) {
        return researchReportVersionMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportVersionDO>()
                        .eq(ResearchReportVersionDO::getReportId, reportId)
                        .eq(ResearchReportVersionDO::getVersionNo, versionNo)
                        .eq(ResearchReportVersionDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    public List<ResearchReportVersionDO> listByTaskId(String taskId) {
        return researchReportVersionMapper.selectList(
                new LambdaQueryWrapper<ResearchReportVersionDO>()
                        .eq(ResearchReportVersionDO::getTaskId, taskId)
                        .eq(ResearchReportVersionDO::getDeleted, 0)
                        .orderByDesc(ResearchReportVersionDO::getVersionNo)
                        .orderByDesc(ResearchReportVersionDO::getId)
        );
    }

    public ResearchReportVersionDO selectByTaskAndVersion(String taskId, Integer versionNo) {
        return researchReportVersionMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportVersionDO>()
                        .eq(ResearchReportVersionDO::getTaskId, taskId)
                        .eq(ResearchReportVersionDO::getVersionNo, versionNo)
                        .eq(ResearchReportVersionDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    public void insert(ResearchReportVersionDO entity) {
        researchReportVersionMapper.insert(entity);
    }
}
