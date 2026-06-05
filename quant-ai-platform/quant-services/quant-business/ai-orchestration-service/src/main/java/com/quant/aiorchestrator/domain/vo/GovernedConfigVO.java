package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GovernedConfigVO {
    private String storeCode;
    private String configPath;
    private Map<String, Object> root;
    private List<Map<String, Object>> audits;
}
