package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;

public interface ResearchWorkbenchProjectionProvider {
    ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO);
}
