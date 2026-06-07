package com.quant.aiorchestrator.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelRuleConfig {

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule pageTasksRule = new FlowRule();
        pageTasksRule.setResource("pageTasks");
        pageTasksRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        pageTasksRule.setCount(10);
        rules.add(pageTasksRule);

        FlowRule fullDetailRule = new FlowRule();
        fullDetailRule.setResource("getTaskFullDetail");
        fullDetailRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        fullDetailRule.setCount(10);
        rules.add(fullDetailRule);

        FlowRuleManager.loadRules(rules);

        List<ParamFlowRule> paramRules = new ArrayList<>();

        ParamFlowRule taskDetailHotRule = new ParamFlowRule("getTaskFullDetail")
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(5);

        paramRules.add(taskDetailHotRule);

        ParamFlowRuleManager.loadRules(paramRules);

        List<DegradeRule> degradeRules = new ArrayList<>();

        DegradeRule pageTasksCircuitBreaker = new DegradeRule("pageTasks")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.2)
                .setMinRequestAmount(20)
                .setStatIntervalMs(60_000)
                .setTimeWindow(30);
        degradeRules.add(pageTasksCircuitBreaker);

        DegradeRule fullDetailCircuitBreaker = new DegradeRule("getTaskFullDetail")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.2)
                .setMinRequestAmount(20)
                .setStatIntervalMs(60_000)
                .setTimeWindow(30);
        degradeRules.add(fullDetailCircuitBreaker);

        DegradeRuleManager.loadRules(degradeRules);
    }
}
