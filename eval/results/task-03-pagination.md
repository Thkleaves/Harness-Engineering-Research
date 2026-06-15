# 任务③ 分页排序 评测总结

## 任务要求
实现分页参数（page/size）、排序参数（sort，白名单过滤防 SQL 注入）、返回 Page<UserSummary>（不暴露 password），三层都有测试。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 90 | 628s | 66.2K / 30.2K |
| Superpowers | 90 | 645s | 75.0K / 28.1K |
| Gstack | 90 | 279s | 47.8K / 12.6K |
| **OpenSpec** | **95** | **295s** | 46.4K / 14.9K |

## 分析

OpenSpec 以 95 分领先。差异在于 UserSummary DTO 的 password 泄露检查：OpenSpec 更严格地确保 DTO 不暴露 password 字段（+15），其他三组在此项上略逊。

- **Gstack & OpenSpec** 效率高（~5min），token 低
- **Superpowers** TDD 流程生成了更多代码（28K output），但没有带来额外得分
- **Baseline** 一次过，无流程开销

- **wow-harness (95)** 与 OpenSpec 并列最高，hooks 治理机制防止了排序白名单遗漏

## 结论
L2 任务开始出现得分分化。OpenSpec 的"先写 Spec 定义接口"方式在分页这种 API 设计任务上有优势。Superpowers 的流程优势在此任务上未体现。
