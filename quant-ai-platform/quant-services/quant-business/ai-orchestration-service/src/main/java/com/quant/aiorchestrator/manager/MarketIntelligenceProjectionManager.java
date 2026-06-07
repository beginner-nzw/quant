package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.common.model.enums.MarketIntelligenceTypeEnum;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MarketIntelligenceProjectionManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final FollowUpTaskSummaryManager followUpManager;
    private final MarketIntelligenceReadManager readManager;
    private final MarketIntelligenceItemAssembler itemAssembler;

    @Autowired
    public MarketIntelligenceProjectionManager(ResearchTaskMapper researchTaskMapper,
                                               ResearchReportMapper researchReportMapper,
                                               RiskWarningMapper riskWarningMapper,
                                               RiskWarningDetailMapper riskWarningDetailMapper,
                                               StrategySignalMapper strategySignalMapper,
                                               FollowUpTaskSummaryManager followUpManager,
                                               StrategySignalRuleManager ruleManager,
                                               MarketIntelligenceReadManager readManager) {
        this(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                strategySignalMapper,
                followUpManager,
                ruleManager,
                readManager,
                new MarketIntelligenceItemAssembler(ruleManager)
        );
    }

    public MarketIntelligenceProjectionManager(ResearchTaskMapper researchTaskMapper,
                                               ResearchReportMapper researchReportMapper,
                                               RiskWarningMapper riskWarningMapper,
                                               RiskWarningDetailMapper riskWarningDetailMapper,
                                               StrategySignalMapper strategySignalMapper,
                                               FollowUpTaskSummaryManager followUpManager,
                                               StrategySignalRuleManager ruleManager,
                                               MarketIntelligenceReadManager readManager,
                                               MarketIntelligenceItemAssembler itemAssembler) {
        this.researchTaskMapper = researchTaskMapper;
        this.researchReportMapper = researchReportMapper;
        this.followUpManager = followUpManager;
        this.readManager = readManager;
        this.itemAssembler = itemAssembler;
    }

    public MarketIntelligenceProjectionManager(ResearchTaskMapper researchTaskMapper,
                                               ResearchReportMapper researchReportMapper,
                                               RiskWarningMapper riskWarningMapper,
                                               RiskWarningDetailMapper riskWarningDetailMapper,
                                               StrategySignalMapper strategySignalMapper,
                                               FollowUpTaskSummaryManager followUpManager,
                                               StrategySignalRuleManager ruleManager) {
        this(
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                strategySignalMapper,
                followUpManager,
                ruleManager,
                new MarketIntelligenceReadManager(riskWarningMapper, riskWarningDetailMapper, strategySignalMapper)
        );
    }

    public MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO) {
        MarketIntelligencePageQueryDTO safeQuery = queryDTO == null ? new MarketIntelligencePageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<MarketIntelligenceListItemVO> matchedRecords = listMarketIntelligenceRecords(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        MarketIntelligencePageVO vo = new MarketIntelligencePageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }
    public MarketIntelligenceStatsVO getMarketIntelligenceStats() {
        List<MarketIntelligenceListItemVO> records = listMarketIntelligenceRecords(new MarketIntelligencePageQueryDTO());
        MarketIntelligenceStatsVO vo = new MarketIntelligenceStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setRiskAlertCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.RISK_ALERT.name().equals(item.getIntelligenceType())).count());
        vo.setStrategySignalCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.STRATEGY_SIGNAL.name().equals(item.getIntelligenceType())).count());
        vo.setReportInsightCount(records.stream().filter(item -> MarketIntelligenceTypeEnum.REPORT_INSIGHT.name().equals(item.getIntelligenceType())).count());
        vo.setHighPriorityCount(records.stream().filter(item -> "HIGH".equalsIgnoreCase(item.getPriority())).count());
        vo.setPendingReviewCount(records.stream().filter(item -> ReportReviewStatusEnum.PENDING.name().equals(item.getReviewStatus())).count());
        return vo;
    }

    private List<MarketIntelligenceListItemVO> listMarketIntelligenceRecords(MarketIntelligencePageQueryDTO queryDTO) {
        List<ResearchReportDO> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        );

        if (reports.isEmpty()) {
            return List.of();
        }

        Set<String> taskIds = reports.stream()
                .map(ResearchReportDO::getTaskId)
                .filter(taskId -> taskId != null && !taskId.isBlank())
                .collect(Collectors.toSet());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        Map<String, ResearchTaskDO> taskMap = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
        Map<String, RiskWarningDO> riskWarningMap = readManager.loadLatestRiskWarningMapByTaskIds(taskIds);
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = readManager.loadRiskWarningDetailMapByWarningIds(
                riskWarningMap.values().stream()
                        .map(RiskWarningDO::getWarningId)
                        .filter(warningId -> warningId != null && !warningId.isBlank())
                        .collect(Collectors.toSet())
        );
        Map<String, StrategySignalDO> strategySignalMap = readManager.loadLatestStrategySignalMapByTaskIds(taskIds);

        List<ResearchTaskDO> followUpTasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "MARKET_INTELLIGENCE")
        );

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId = followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));

        Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId = followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));

        return reports.stream()
                .map(report -> {
                    RiskWarningDO warning = riskWarningMap.get(report.getTaskId());
                    StrategySignalDO signal = strategySignalMap.get(report.getTaskId());
                    return itemAssembler.toMarketIntelligenceItem(
                            report,
                            taskMap.get(report.getTaskId()),
                            resolveMarketIntelligenceFollowUpSummary(
                                    taskMap.get(report.getTaskId()),
                                    report,
                                    followUpTaskMapBySourceTaskId,
                                    followUpTaskMapBySourceReportId
                            ),
                            warning,
                            warning == null ? List.of() : riskWarningDetailMap.getOrDefault(warning.getWarningId(), List.of()),
                            signal
                    );
                })
                .filter(Objects::nonNull)
                .filter(item -> itemAssembler.matchesMarketIntelligenceQuery(item, queryDTO))
                .sorted(Comparator
                        .comparing(MarketIntelligenceListItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MarketIntelligenceListItemVO::getConfidenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private FollowUpTaskSummaryManager.FollowUpSummary resolveMarketIntelligenceFollowUpSummary(ResearchTaskDO sourceTask,
                                                                                                ResearchReportDO sourceReport,
                                                                                                Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId,
                                                                                                Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId) {
        return followUpManager.resolveSummary(
                sourceTask,
                sourceReport,
                followUpTaskMapBySourceTaskId,
                followUpTaskMapBySourceReportId
        );
    }
}
