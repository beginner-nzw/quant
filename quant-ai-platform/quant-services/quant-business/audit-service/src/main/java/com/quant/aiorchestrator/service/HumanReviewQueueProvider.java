package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.HumanReviewQueueQueryDTO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueuePageVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueStatsVO;

public interface HumanReviewQueueProvider {
    HumanReviewQueuePageVO pageQueue(HumanReviewQueueQueryDTO queryDTO);

    HumanReviewQueueStatsVO getStats();
}
