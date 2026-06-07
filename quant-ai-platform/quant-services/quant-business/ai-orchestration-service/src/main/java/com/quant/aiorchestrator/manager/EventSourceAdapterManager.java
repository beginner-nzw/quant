package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.service.EventSourceSyncAdapter;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventSourceAdapterManager {

    private final List<EventSourceSyncAdapter> eventSourceSyncAdapters;

    public EventSourceSyncAdapter resolveAdapter(EventSourceConfigItemVO sourceConfig, String errorCode, String errorMessage) {
        EventSourceSyncAdapter adapter = eventSourceSyncAdapters.stream()
                .filter(item -> item.supports(sourceConfig))
                .findFirst()
                .orElse(null);
        if (adapter == null) {
            throw new BizException(errorCode, errorMessage);
        }
        return adapter;
    }
}
