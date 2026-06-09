package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;

public interface HumanReviewDecisionProvider {
    String decide(String queueId, HumanReviewDecisionDTO dto);
}
