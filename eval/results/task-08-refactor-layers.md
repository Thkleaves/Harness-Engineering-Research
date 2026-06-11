# 任务⑧ 三层重构 评测总结

## 任务要求
将 Controller 中的业务逻辑重构为 Repository + Service + Controller 三层，保留所有现有 API，不破坏集成测试，新增单元测试。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 100 | 313s | 44.9K / 15.5K |
| Superpowers | 100 | 623s | 66.6K / 24.8K |
| Gstack | 100 | 381s | 57.9K / 17.1K |
| OpenSpec | 100 | 420s | 46.0K / 18.2K |

## 分析

四组全部满分 100！重构任务是代码结构转换，各组都正确完成了：
- Repository 层抽取（@Repository）
- Service 层抽取（@Service）
- Controller 清洁验证（无 ConcurrentHashMap 残留）
- 原有测试回归通过
- 新增 Service/Repository 测试

**唯一差异在效率**：Superpowers 耗时是 Baseline 的 2 倍（623s vs 313s），token 高出 50%。brainstorming + plan 流程在重构任务上产生冗余思考。

## 结论
重构任务属于"操作明确、判断简单"的类型，裸 Agent 一次做对且最快。Harness 的流程优势在此类任务上完全体现不出来，反而是纯开销。
