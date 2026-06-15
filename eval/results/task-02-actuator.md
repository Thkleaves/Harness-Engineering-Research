# 任务② Actuator 评测总结

## 任务要求
添加 Spring Boot Actuator 支持，暴露 /health 和 /info 端点，自定义 readiness 检查 Redis，集成测试验证。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 90 | 992s | 80.8K / 35.9K |
| Superpowers | 90 | 686s | 75.1K / 27.1K |
| Gstack | 90 | 488s | 53.0K / 19.4K |
| wow-harness | 90 | — | — |
| OpenSpec | 90 | 1206s | 75.1K / 52.2K |

## 分析

四组得分一致（90/100）。任务有清晰的技术要求（actuator 依赖、health/info/readiness），各组均正确实现。均丢失的 10 分来自测试部分的通过率未达满分（测试部分最多 25 分，各组约 15 分）。

值得注意的异常值：
- **OpenSpec 耗时最长**（20min），输出 token 是 Gstack 的 2.7 倍，可能是 Spec 文档写得太详细
- **Gstack 最均衡**：8min / 72K token，角色分工在结构化任务上高效

## 结论
L1 的 actuator 任务与 validation 类似，技术要求明确，四组没有得分差异。Harness 对简单的"加依赖→配置→写端点"类任务帮助有限。
