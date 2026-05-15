package com.quant.aiorchestrator.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
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
    }
}