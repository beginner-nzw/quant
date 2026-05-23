# Project Charter

## Project

金融量化投研与风险预警多智能体平台。

本项目不是一个简单的金融问答助手，而是一个面向机构投研、风控、研究管理、模型运营和合规审计的企业级 AI 原生平台。

## Long-Term Goal

系统长期目标是形成一条可治理、可审计、可回溯、可持续演化的投研与风险预警链路：

1. 外部市场事件、公告、新闻、研报和结构化金融数据进入平台。
2. Java 业务服务负责任务、状态、领域事实、权限、审计和业务投影。
3. Python AI Engine 负责 LangGraph 多 Agent 执行、推理、生成和执行轨迹。
4. Kafka 负责 AI 主链路异步协同。
5. Redis 负责热点缓存、任务状态快照、控制信号和幂等辅助。
6. Vue 前端只消费后端 contract，不自行定义业务真值。

## Current Strategic Choice

当前阶段不继续扩功能，也不立即大拆微服务。

当前优先级是先把已经快速实现出来的系统收回到可治理轨道：

1. 冻结真实架构边界。
2. 冻结 authority matrix。
3. 冻结 host ownership。
4. 冻结后端 contract。
5. 登记 transition lifetime。
6. 建立 eval checklist。

## Engineering Priorities

优先级固定如下：

1. 主路径可运行。
2. 权威对象清晰。
3. contract 清晰。
4. 正式宿主清晰。
5. 过渡层有生命周期。
6. eval / test 能发现架构漂移。
7. 新功能开发。

新功能不能越过 authority、contract、host ownership 直接推进。

## Human Approval Required

以下事项必须由人类批准：

1. 是否接受 breaking change。
2. 是否删除或降级已有 fallback。
3. 是否把临时聚合服务拆成独立微服务。
4. 是否把 JSON 配置迁移到 DB / Nacos。
5. 是否放弃某条兼容路径。
6. 是否让某个过渡层继续留在主路径。

## Review Order

所有实现和评审按以下顺序执行：

```text
belongs -> authority -> contract -> behavior
```

不能先以“能跑”“页面能用”“测试过了”为完成标准。

## Steering Constraint

Window 0 不是最高智能体，也不是更聪明的自由聊天窗口。

Window 0 只能作为受约束的状态机工作：

1. 读取固定 harness 工件。
2. 使用固定评分规则提出下一阶段候选目标。
3. 输出一个 steering decision。
4. 等待人类批准。
5. 获批后才允许 Window 1 进行阶段治理。

Window 0 不能自我批准，不能跳过 Window 1，也不能直接启动实现。
