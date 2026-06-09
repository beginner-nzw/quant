package com.quant.subscription.controller;

import com.quant.api.subscription.SubscriptionPort;
import com.quant.api.subscription.dto.NotificationPublishDTO;
import com.quant.api.subscription.dto.SubscriptionCreateDTO;
import com.quant.api.subscription.vo.NotificationVO;
import com.quant.api.subscription.vo.SubscriptionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionPort subscriptionService;

    public SubscriptionController(SubscriptionPort subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public SubscriptionVO createSubscription(@RequestBody SubscriptionCreateDTO dto) {
        return subscriptionService.createSubscription(dto);
    }

    @GetMapping("/users/{userId}")
    public List<SubscriptionVO> listUserSubscriptions(@PathVariable String userId) {
        return subscriptionService.listUserSubscriptions(userId);
    }

    @PostMapping("/notifications")
    public List<NotificationVO> publishNotification(@RequestBody NotificationPublishDTO dto) {
        return subscriptionService.publishNotification(dto);
    }

    @GetMapping("/users/{userId}/notifications")
    public List<NotificationVO> listUserNotifications(@PathVariable String userId) {
        return subscriptionService.listUserNotifications(userId);
    }
}
