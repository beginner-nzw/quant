package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StrategySignalTaskReadManager {

    private final StrategySignalMapper strategySignalMapper;

    public Map<String, StrategySignalDO> loadLatestStrategySignalMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return strategySignalMapper.selectList(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getDeleted, 0)
                        .in(StrategySignalDO::getTaskId, taskIds)
                        .orderByDesc(StrategySignalDO::getSignalDate, StrategySignalDO::getCreatedAt, StrategySignalDO::getId)
        ).stream()
                .filter(item -> item.getTaskId() != null && !item.getTaskId().isBlank())
                .collect(Collectors.toMap(
                        StrategySignalDO::getTaskId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }
}
