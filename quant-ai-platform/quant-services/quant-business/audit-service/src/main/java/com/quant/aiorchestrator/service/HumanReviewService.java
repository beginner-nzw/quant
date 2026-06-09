package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.domain.dto.HumanReviewQueueQueryDTO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueuePageVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueStatsVO;

public interface HumanReviewService {
    HumanReviewQueuePageVO pageQueue(HumanReviewQueueQueryDTO queryDTO);
    HumanReviewQueueStatsVO getStats();
    String decide(String queueId, HumanReviewDecisionDTO dto);
}
