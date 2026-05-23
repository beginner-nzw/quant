package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.service.ResearchWorkbenchQueryService;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class ResearchWorkbenchController {

    private final ResearchWorkbenchQueryService researchWorkbenchQueryService;

    @GetMapping("/research-workbench")
    public Result<ResearchWorkbenchVO> getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO) {
        return Result.success(researchWorkbenchQueryService.getResearchWorkbench(queryDTO));
    }
}
