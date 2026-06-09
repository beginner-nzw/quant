package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StrategySignalProjectionManager {

    private final StrategySignalRuleManager ruleManager;
    private final StrategySignalRecordManager recordManager;

    @Autowired
    public StrategySignalProjectionManager(StrategySignalRecordManager recordManager,
                                           StrategySignalRuleManager ruleManager) {
        this.ruleManager = ruleManager;
        this.recordManager = recordManager;
    }

    public StrategySignalProjectionManager(StrategySignalReadManager readManager,
                                           StrategySignalFollowUpSummaryManager followUpManager,
                                           StrategySignalRuleManager ruleManager,
                                           StrategySignalItemAssembler itemAssembler) {
        this(
                new StrategySignalRecordManager(
                        readManager,
                        followUpManager,
                        ruleManager,
                        itemAssembler
                ),
                ruleManager
        );
    }

    public StrategySignalPageVO pageStrategySignals(StrategySignalPageQueryDTO queryDTO) {
        StrategySignalPageQueryDTO safeQuery = queryDTO == null ? new StrategySignalPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<StrategySignalListItemVO> matchedRecords = recordManager.listStrategySignalRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        StrategySignalPageVO vo = new StrategySignalPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    public StrategySignalStatsVO getStrategySignalStats() {
        List<StrategySignalListItemVO> records = recordManager.listStrategySignalRecords(new StrategySignalPageQueryDTO());
        StrategySignalStatsVO vo = new StrategySignalStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setPositiveCount(records.stream().filter(item -> SignalDirectionEnum.POSITIVE.name().equals(item.getSignalDirection())).count());
        vo.setNeutralCount(records.stream().filter(item -> SignalDirectionEnum.NEUTRAL.name().equals(item.getSignalDirection())).count());
        vo.setNegativeCount(records.stream().filter(item -> SignalDirectionEnum.NEGATIVE.name().equals(item.getSignalDirection())).count());
        vo.setHighConfidenceCount(records.stream().filter(item -> ruleManager.isHighConfidence(item.getConfidenceScore())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReportReviewStatus())).count());
        return vo;
    }

}


