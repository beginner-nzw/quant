package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;

/**
 * Display-only aggregation contract for the research workbench.
 *
 * <p>This surface composes existing read models for UI display and must not be
 * consumed by backend commands or projection paths as task, report, risk,
 * strategy, market, audit, or config authority.</p>
 */
public interface ResearchWorkbenchQueryService {
    ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO);
}
