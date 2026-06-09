package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.domain.dto.HumanReviewQueueQueryDTO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueuePageVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueStatsVO;
import com.quant.aiorchestrator.service.HumanReviewDecisionProvider;
import com.quant.aiorchestrator.service.HumanReviewQueueProvider;
import com.quant.aiorchestrator.service.HumanReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HumanReviewServiceImpl implements HumanReviewService {

    private final HumanReviewQueueProvider humanReviewQueueProvider;
    private final HumanReviewDecisionProvider humanReviewDecisionProvider;

    @Override
    public HumanReviewQueuePageVO pageQueue(HumanReviewQueueQueryDTO queryDTO) {
        return humanReviewQueueProvider.pageQueue(queryDTO);
    }

    @Override
    public HumanReviewQueueStatsVO getStats() {
        return humanReviewQueueProvider.getStats();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String decide(String queueId, HumanReviewDecisionDTO dto) {
        return humanReviewDecisionProvider.decide(queueId, dto);
    }
}
