package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.audit.HumanReviewQueueReportProjection;
import com.quant.aiorchestrator.audit.HumanReviewQueueReportProvider;
import com.quant.aiorchestrator.audit.HumanReviewQueueRiskProjection;
import com.quant.aiorchestrator.audit.HumanReviewQueueRiskProvider;
import com.quant.aiorchestrator.audit.HumanReviewQueueTaskProjection;
import com.quant.aiorchestrator.audit.HumanReviewQueueTaskProvider;
import com.quant.aiorchestrator.domain.dto.HumanReviewQueueQueryDTO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueItemVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueuePageVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueStatsVO;
import com.quant.aiorchestrator.service.HumanReviewQueueProvider;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HumanReviewQueueManager implements HumanReviewQueueProvider {

    public static final String DOMAIN_REPORT = "REPORT";
    public static final String DOMAIN_RISK = "RISK";
    public static final String DOMAIN_COMPLIANCE = "COMPLIANCE";

    private final HumanReviewQueueTaskProvider taskProvider;
    private final HumanReviewQueueReportProvider reportProvider;
    private final HumanReviewQueueRiskProvider riskProvider;
    private final ObjectMapper objectMapper;

    public HumanReviewQueueManager(HumanReviewQueueTaskProvider taskProvider,
                                   HumanReviewQueueReportProvider reportProvider,
                                   HumanReviewQueueRiskProvider riskProvider,
                                   ObjectMapper objectMapper) {
        this.taskProvider = taskProvider;
        this.reportProvider = reportProvider;
        this.riskProvider = riskProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public HumanReviewQueuePageVO pageQueue(HumanReviewQueueQueryDTO queryDTO) {
        HumanReviewQueueQueryDTO safeQuery = queryDTO == null ? new HumanReviewQueueQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<HumanReviewQueueItemVO> records = listQueueItems(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, records.size());
        int toIndex = Math.min(fromIndex + pageSize, records.size());

        HumanReviewQueuePageVO vo = new HumanReviewQueuePageVO();
        vo.setTotal((long) records.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : records.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public HumanReviewQueueStatsVO getStats() {
        List<HumanReviewQueueItemVO> records = listQueueItems(new HumanReviewQueueQueryDTO());
        HumanReviewQueueStatsVO vo = new HumanReviewQueueStatsVO();
        vo.setTotalCount((long) records.size());
        vo.setPendingCount(records.stream().filter(item -> isPending(item.getReviewStatus())).count());
        vo.setApprovedCount(records.stream().filter(item -> ReportReviewStatusEnum.APPROVED.name().equals(item.getReviewStatus())).count());
        vo.setRejectedCount(records.stream().filter(item -> ReportReviewStatusEnum.REJECTED.name().equals(item.getReviewStatus())).count());
        vo.setReportCount(records.stream().filter(item -> DOMAIN_REPORT.equals(item.getDomain())).count());
        vo.setRiskCount(records.stream().filter(item -> DOMAIN_RISK.equals(item.getDomain())).count());
        vo.setComplianceCount(records.stream().filter(item -> DOMAIN_COMPLIANCE.equals(item.getDomain())).count());
        return vo;
    }

    private List<HumanReviewQueueItemVO> listQueueItems(HumanReviewQueueQueryDTO queryDTO) {
        List<HumanReviewQueueReportProjection> reports = reportProvider.listHumanReviewQueueReports();
        List<HumanReviewQueueRiskProjection> risks = riskProvider.listHumanReviewQueueRisks();

        List<String> taskIds = new ArrayList<>();
        reports.stream().map(HumanReviewQueueReportProjection::taskId).filter(this::hasText).forEach(taskIds::add);
        risks.stream().map(HumanReviewQueueRiskProjection::taskId).filter(this::hasText).forEach(taskIds::add);
        Map<String, HumanReviewQueueTaskProjection> taskMap = loadTaskMap(taskIds);

        List<HumanReviewQueueItemVO> items = new ArrayList<>();
        for (HumanReviewQueueReportProjection report : reports) {
            HumanReviewQueueTaskProjection task = taskMap.get(report.taskId());
            if (task == null) {
                continue;
            }
            items.add(toReportQueueItem(report, task, DOMAIN_REPORT));
            if (needsComplianceReview(report)) {
                items.add(toReportQueueItem(report, task, DOMAIN_COMPLIANCE));
            }
        }
        for (HumanReviewQueueRiskProjection risk : risks) {
            HumanReviewQueueTaskProjection task = taskMap.get(risk.taskId());
            if (task != null) {
                items.add(toRiskQueueItem(risk, task));
            }
        }
        return items.stream()
                .filter(item -> matches(item, queryDTO))
                .sorted(Comparator
                        .comparing(HumanReviewQueueItemVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HumanReviewQueueItemVO::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private HumanReviewQueueItemVO toReportQueueItem(HumanReviewQueueReportProjection report,
                                                     HumanReviewQueueTaskProjection task,
                                                     String domain) {
        HumanReviewQueueItemVO vo = new HumanReviewQueueItemVO();
        vo.setQueueId(domain + ":" + task.taskId());
        vo.setDomain(domain);
        hydrateTaskFields(vo, task);
        vo.setReportId(report.reportId());
        vo.setReportType(firstText(report.reportType(), report.taskType(), task.taskType()));
        vo.setRelatedObjectType("REPORT");
        vo.setRelatedObjectId(report.reportId());
        vo.setReviewStatus(resolveReviewStatus(report.reviewStatus()).name());
        vo.setNeedHumanReview(report.needHumanReview() != null && report.needHumanReview() == 1);
        vo.setRevised(isReportRevised(report));
        vo.setRerunnable(true);
        vo.setCurrentNode("report_generation_agent");
        vo.setSummary(firstText(report.revisedSummary(), report.summary()));
        vo.setRiskPoints(readTextList(firstText(report.revisedRiskPoints(), report.riskPoints())));
        vo.setReviewComment(report.reviewComment());
        vo.setReviewedBy(report.reviewedBy());
        vo.setReviewedAt(report.reviewedAt());
        vo.setCreatedAt(report.createdAt());
        return vo;
    }

    private HumanReviewQueueItemVO toRiskQueueItem(HumanReviewQueueRiskProjection risk,
                                                   HumanReviewQueueTaskProjection task) {
        HumanReviewQueueItemVO vo = new HumanReviewQueueItemVO();
        vo.setQueueId(DOMAIN_RISK + ":" + task.taskId());
        vo.setDomain(DOMAIN_RISK);
        hydrateTaskFields(vo, task);
        vo.setRelatedObjectType("RISK_WARNING");
        vo.setRelatedObjectId(risk.warningId());
        vo.setReviewStatus(resolveReviewStatus(risk.reviewStatus()).name());
        vo.setRiskLevel(risk.warningLevel());
        vo.setNeedHumanReview(isHighRisk(risk) || "NEED_HUMAN_REVIEW".equalsIgnoreCase(risk.suggestAction()));
        vo.setRevised(false);
        vo.setRerunnable(true);
        vo.setCurrentNode("risk_review_agent");
        vo.setSummary(firstText(risk.warningSummary(), risk.warningReason()));
        vo.setRiskPoints(readLines(risk.warningReason()));
        vo.setReviewComment(risk.suggestAction());
        vo.setReviewedBy(risk.reviewerId());
        vo.setReviewedAt(risk.reviewTime());
        vo.setCreatedAt(risk.createdAt());
        return vo;
    }

    private void hydrateTaskFields(HumanReviewQueueItemVO vo, HumanReviewQueueTaskProjection task) {
        vo.setTaskId(task.taskId());
        vo.setTaskTitle(task.taskTitle());
        vo.setTaskType(task.taskType());
        vo.setTargetCode(task.targetCode());
        vo.setTargetName(task.targetName());
        vo.setPriority(task.priority());
    }

    private boolean matches(HumanReviewQueueItemVO item, HumanReviewQueueQueryDTO query) {
        if (query == null) {
            return true;
        }
        if (hasText(query.getDomain()) && !query.getDomain().equalsIgnoreCase(item.getDomain())) {
            return false;
        }
        if (hasText(query.getReviewStatus()) && !query.getReviewStatus().equalsIgnoreCase(item.getReviewStatus())) {
            return false;
        }
        if (Boolean.TRUE.equals(query.getOnlyPending()) && !isPending(item.getReviewStatus())) {
            return false;
        }
        if (hasText(query.getTargetCode()) && !query.getTargetCode().equalsIgnoreCase(item.getTargetCode())) {
            return false;
        }
        return !hasText(query.getTargetName()) || containsIgnoreCase(item.getTargetName(), query.getTargetName());
    }

    private Map<String, HumanReviewQueueTaskProjection> loadTaskMap(List<String> taskIds) {
        Set<String> uniqueIds = taskIds.stream().filter(this::hasText).collect(Collectors.toSet());
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return taskProvider.loadTaskMapByTaskIds(uniqueIds);
    }

    private boolean needsComplianceReview(HumanReviewQueueReportProjection report) {
        return isPending(report.reviewStatus()) && (
                report.needHumanReview() != null && report.needHumanReview() == 1
                        || !readTextList(report.riskWarnings()).isEmpty()
        );
    }

    private boolean isReportRevised(HumanReviewQueueReportProjection report) {
        return hasText(report.revisedSummary())
                || hasText(report.revisedRiskPoints());
    }

    private boolean isPending(String reviewStatus) {
        return ReportReviewStatusEnum.PENDING == resolveReviewStatus(reviewStatus);
    }

    private ReportReviewStatusEnum resolveReviewStatus(String reviewStatus) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(reviewStatus);
        return resolved == null ? ReportReviewStatusEnum.PENDING : resolved;
    }

    private boolean isHighRisk(HumanReviewQueueRiskProjection risk) {
        return risk != null && "HIGH".equalsIgnoreCase(risk.warningLevel());
    }

    private List<String> readTextList(String rawJson) {
        if (!hasText(rawJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {})
                    .stream()
                    .filter(this::hasText)
                    .map(String::trim)
                    .toList();
        } catch (Exception ignored) {
            return readLines(rawJson);
        }
    }

    private List<String> readLines(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        return text.lines()
                .map(String::trim)
                .filter(this::hasText)
                .toList();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null && source.toLowerCase().contains(target.toLowerCase());
    }
}
