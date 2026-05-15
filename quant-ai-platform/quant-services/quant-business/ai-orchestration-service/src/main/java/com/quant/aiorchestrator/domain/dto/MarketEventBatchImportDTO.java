package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class MarketEventBatchImportDTO {

    private List<MarketEventCreateDTO> events;
}
