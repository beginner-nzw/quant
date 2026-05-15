package com.quant.aiorchestrator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;

public interface MarketEventAutoTriggerService {
        String AUTO_TRIGGER_DISABLED = "DISABLED";
        String AUTO_TRIGGER_NO_MATCH = "NO_MATCH";
        String AUTO_TRIGGER_SUCCESS = "SUCCESS";
        String AUTO_TRIGGER_FAILED = "FAILED";
        String AUTO_TRIGGER_WILL_TRIGGER = "WILL_TRIGGER";

        public MarketEventDO prepareAutoTrigger(MarketEventDO event);

        public MarketEventDO loadEvent(String eventId);

        public boolean isPendingAutoTrigger(MarketEventDO event);

        public String executePendingAutoTrigger(MarketEventDO event);

        public boolean shouldCountAsQueued(String autoTriggerStatus);
}
