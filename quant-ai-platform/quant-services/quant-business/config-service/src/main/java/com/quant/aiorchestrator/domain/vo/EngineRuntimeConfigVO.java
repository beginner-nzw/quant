package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class EngineRuntimeConfigVO {

    private String engineCode;
    private String env;
    private String host;
    private Integer port;
    private Integer workflowTimeoutSeconds;
    private String consumerGroup;
    private String kafkaBootstrapServers;
    private String dispatchTopic;
    private String statusTopic;
    private String resultTopic;
    private String auditTopic;
    private String redisEndpoint;
    private String runtimeMode;
}
