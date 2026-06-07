package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.vo.ReportReviewStatsVO;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportReviewStatsManager {

    private final ResearchReportMapper researchReportMapper;

    public ReportReviewStatsVO getReportReviewStats() {
        ReportReviewStatsVO vo = new ReportReviewStatsVO();

        vo.setPendingCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .and(wrapper -> wrapper
                                .isNull(ResearchReportDO::getReviewStatus)
                                .or()
                                .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.PENDING.name()))
        ));

        vo.setApprovedCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.APPROVED.name())
        ));

        vo.setRejectedCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .eq(ResearchReportDO::getReviewStatus, ReportReviewStatusEnum.REJECTED.name())
        ));

        vo.setTotalReportCount(researchReportMapper.selectCount(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
        ));

        return vo;
    }
}
