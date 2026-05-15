package com.quant.task.service;

import com.quant.task.domain.dto.CreateResearchTaskDTO;

public interface ResearchTaskService {
    String createTask(CreateResearchTaskDTO dto);
}