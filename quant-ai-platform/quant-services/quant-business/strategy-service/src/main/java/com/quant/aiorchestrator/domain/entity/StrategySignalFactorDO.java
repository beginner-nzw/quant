package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("strategy_signal_factor")
public class StrategySignalFactorDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String factorId;
    private String signalId;
    private String factorCode;
    private String factorName;
    private String factorValue;
    private BigDecimal factorWeight;
    private String factorConclusion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
