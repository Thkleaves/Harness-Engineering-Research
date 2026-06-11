# 任务④ 注册验证 评测总结

## 任务要求
实现完整注册+邮箱验证流程：register（UNVERIFIED）→ 生成 Token → verify 激活 → 未验证不能登录 → Token 24h 过期 → 全流程测试。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 95 | 269s | 42.2K / 10.4K |
| Superpowers | 90 | 466s | 91.2K / 23.4K |
| Gstack | 90 | 144s | 13.1K / 7.9K |
| **OpenSpec** | **100** | **330s** | 56.1K / 14.1K |

## 分析

OpenSpec 拿到满分 100——唯一一组满分。注册流程涉及多个状态转换（UNVERIFIED → ACTIVE）和边界条件（过期、重复验证、未验证登录拦截），OpenSpec 的 Spec 驱动方式在状态机设计上优势明显。

值得注意：
- **Gstack 速度惊人**：144s / 21K token，角色分工极其高效，但丢分可能因为 QA 角色检查不够细致
- **Superpowers 最低**（90）：TDD + review 流程并未转化为更全面的测试覆盖，token 消耗反而是 Baseline 的 5 倍
- **Baseline 95 分**仅次于 OpenSpec：裸 Agent 在流程类任务上表现出色

## 结论
状态机驱动的流程任务中，OpenSpec 的 Spec-first 思路有明确优势。Superpowers 的繁重流程反而可能让模型注意力分散。
