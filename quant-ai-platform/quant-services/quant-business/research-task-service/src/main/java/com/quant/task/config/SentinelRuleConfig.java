package com.quant.task.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelRuleConfig {

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule createTaskRule = new FlowRule();
        createTaskRule.setResource("createResearchTask");
        createTaskRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createTaskRule.setCount(5);
        rules.add(createTaskRule);

        FlowRuleManager.loadRules(rules);

        List<DegradeRule> degradeRules = new ArrayList<>();
        DegradeRule createTaskCircuitBreaker = new DegradeRule("createResearchTask")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.2)
                .setMinRequestAmount(20)
                .setStatIntervalMs(60_000)
                .setTimeWindow(30);
        degradeRules.add(createTaskCircuitBreaker);

        DegradeRuleManager.loadRules(degradeRules);
    }
}
