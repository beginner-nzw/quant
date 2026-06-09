package com.quant.aiapi;

import com.quant.aiorchestrator.service.AiTaskDeadLetterPublisherService;
import com.quant.aiorchestrator.service.AiTaskInboundMessageSupportService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiApiBoundaryTests {

    @Test
    void aiMessageServiceContractsBelongToApiModule() {
        assertEquals("com.quant.aiorchestrator.service",
                AiTaskDeadLetterPublisherService.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service",
                AiTaskInboundMessageSupportService.class.getPackageName());
    }
}
