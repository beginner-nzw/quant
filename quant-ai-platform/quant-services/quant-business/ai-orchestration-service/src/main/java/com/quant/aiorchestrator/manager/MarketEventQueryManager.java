package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketEventQueryManager {

    private final MarketEventMapper marketEventMapper;
    private final MarketEventProjectionManager marketEventProjectionManager;

    public MarketEventPageVO pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        MarketEventPageQueryDTO safeQuery = queryDTO == null ? new MarketEventPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<MarketEventListItemVO> matchedRecords = marketEventProjectionManager.listMatchedEvents(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        MarketEventPageVO vo = new MarketEventPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    public MarketEventListItemVO getMarketEvent(String eventId) {
        MarketEventDO event = marketEventMapper.selectOne(
                new LambdaQueryWrapper<MarketEventDO>()
                        .eq(MarketEventDO::getEventId, eventId)
                        .eq(MarketEventDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (event == null) {
            throw new BizException("MARKET_EVENT_NOT_FOUND", "市场事件不存在");
        }
        return marketEventProjectionManager.buildMarketEventDetail(event);
    }
}
