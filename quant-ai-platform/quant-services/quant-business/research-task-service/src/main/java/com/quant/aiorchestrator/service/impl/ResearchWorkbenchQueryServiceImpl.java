package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.service.ResearchWorkbenchProjectionProvider;
import com.quant.aiorchestrator.service.ResearchWorkbenchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResearchWorkbenchQueryServiceImpl implements ResearchWorkbenchQueryService {

    private final ResearchWorkbenchProjectionProvider researchWorkbenchProjectionProvider;

    @Override
    public ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO) {
        return researchWorkbenchProjectionProvider.getResearchWorkbench(queryDTO);
    }
}
