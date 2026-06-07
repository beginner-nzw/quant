package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.manager.KafkaMessagePublisherManager;
import com.quant.aiorchestrator.manager.MarketEventStandardizedMessageManager;
import com.quant.aiorchestrator.service.MarketEventStandardizedPublisherService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.message.MarketEventStandardizedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventStandardizedPublisherServiceImpl implements MarketEventStandardizedPublisherService {

    private final MarketEventStandardizedMessageManager marketEventStandardizedMessageManager;
    private final KafkaMessagePublisherManager kafkaMessagePublisherManager;

    public void publish(MarketEventDO event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            return;
        }

        MarketEventStandardizedMessage message = marketEventStandardizedMessageManager.buildMessage(event);
        try {
            kafkaMessagePublisherManager.publish(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED, event.getEventId(), message);
        } catch (KafkaMessagePublisherManager.KafkaMessagePublishException e) {
            log.warn("publish market event standardized message failed, eventId={}, messageId={}",
                    event.getEventId(), message.getMessageId(), e);
        }
    }
}
