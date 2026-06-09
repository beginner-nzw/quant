package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class EventSourceConfigVO {

    private String configPath;
    private List<EventSourceConfigItemVO> sources;
}
