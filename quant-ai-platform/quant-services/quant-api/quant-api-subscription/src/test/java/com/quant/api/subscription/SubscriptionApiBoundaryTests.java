package com.quant.api.subscription;

import com.quant.api.subscription.dto.NotificationPublishDTO;
import com.quant.api.subscription.dto.SubscriptionCreateDTO;
import com.quant.api.subscription.vo.NotificationVO;
import com.quant.api.subscription.vo.SubscriptionVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionApiBoundaryTests {

    @Test
    void subscriptionContractsBelongToApiModule() {
        assertEquals("com.quant.api.subscription", SubscriptionPort.class.getPackageName());
        assertEquals("com.quant.api.subscription.dto", SubscriptionCreateDTO.class.getPackageName());
        assertEquals(SubscriptionCreateDTO.class.getPackageName(), NotificationPublishDTO.class.getPackageName());
        assertEquals("com.quant.api.subscription.vo", SubscriptionVO.class.getPackageName());
        assertEquals(SubscriptionVO.class.getPackageName(), NotificationVO.class.getPackageName());
    }
}
