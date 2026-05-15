package com.quant.aiorchestrator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiWorkflowInstanceMapper extends BaseMapper<AiWorkflowInstanceDO> {
}