package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("market_event_relation")
public class MarketEventRelationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventId;
    private String relationType;
    private String relationCode;
    private String relationName;
    private BigDecimal relationWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
