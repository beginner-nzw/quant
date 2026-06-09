package com.quant.aiorchestrator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.aiorchestrator.domain.entity.MarketEventIngestRunDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MarketEventIngestRunMapper extends BaseMapper<MarketEventIngestRunDO> {
}
