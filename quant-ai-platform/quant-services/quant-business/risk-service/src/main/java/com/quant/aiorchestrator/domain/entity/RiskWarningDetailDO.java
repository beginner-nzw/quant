package com.quant.aiorchestrator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_warning_detail")
public class RiskWarningDetailDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String detailId;
    private String warningId;
    private String indicatorCode;
    private String indicatorName;
    private String indicatorValue;
    private String thresholdValue;
    private String comparisonResult;
    private String detailDesc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
