package com.quant.common.kafka;

import com.quant.common.messaging.KafkaTopicConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfiguration {

    private static final int DEFAULT_PARTITIONS = 3;
    private static final short DEFAULT_REPLICAS = 1;

    @Bean
    public KafkaAdmin.NewTopics platformTopics() {
        return new KafkaAdmin.NewTopics(
                buildTopic(KafkaTopicConstants.MARKET_NEWS_RAW),
                buildTopic(KafkaTopicConstants.MARKET_ANNOUNCEMENT_RAW),
                buildTopic(KafkaTopicConstants.MARKET_FINANCIAL_REPORT_RAW),
                buildTopic(KafkaTopicConstants.MARKET_POLICY_RAW),
                buildTopic(KafkaTopicConstants.MARKET_EVENT_STANDARDIZED),
                buildTopic(KafkaTopicConstants.AI_TASK_DISPATCH),
                buildTopic(KafkaTopicConstants.AI_TASK_STATUS),
                buildTopic(KafkaTopicConstants.AI_TASK_RESULT),
                buildTopic(KafkaTopicConstants.AI_TASK_AUDIT),
                buildTopic(KafkaTopicConstants.AI_TASK_DEADLETTER),
                buildTopic(KafkaTopicConstants.RISK_WARNING_GENERATED),
                buildTopic(KafkaTopicConstants.STRATEGY_SIGNAL_GENERATED),
                buildTopic(KafkaTopicConstants.REPORT_GENERATED),
                buildTopic(KafkaTopicConstants.NOTIFICATION_DISPATCH),
                buildTopic(KafkaTopicConstants.MARKET_EVENT_DEADLETTER),
                buildTopic(KafkaTopicConstants.BUSINESS_EVENT_DEADLETTER)
        );
    }

    private NewTopic buildTopic(String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }
}
