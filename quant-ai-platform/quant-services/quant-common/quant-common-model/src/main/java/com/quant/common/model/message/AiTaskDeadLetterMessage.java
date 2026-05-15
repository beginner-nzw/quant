package com.quant.common.model.message;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTaskDeadLetterMessage extends MessageEnvelope {

    private Payload payload;

    @Data
    public static class Payload {
        private String originalTopic;
        private String originalMessageType;
        private String originalProducerService;
        private String consumerService;
        private String consumerGroup;
        private String failureStage;
        private String errorCode;
        private String errorMessage;
        private String rawMessage;
    }
}
