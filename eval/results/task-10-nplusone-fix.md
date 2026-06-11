# 任务⑩ N+1 查询修复 评测总结

## 任务要求
修复 GET /api/orders 的 N+1 查询问题：用 @EntityGraph 或 JOIN FETCH 优化为 1 次 SQL，验证 SQL 次数降为 1，不改变 JSON 结构，回归测试。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 65 | 483s | 59.5K / 19.4K |
| **Gstack** | **90** | **426s** | 77.7K / 16.0K |
| OpenSpec | 80 | 455s | 52.8K / 18.7K |
| Superpowers | 65 | 578s | 83.2K / 22.9K |

## 诊断详情

| 检查项 | Baseline | Gstack | OpenSpec | Superpowers |
|--------|:--:|:--:|:--:|:--:|
| JOIN FETCH / @EntityGraph (20) | ✓ | ✓ | ✓ | ✓ |
| SQL count 验证 (15) | ✗ | ✓ | ✗ | ✗ |
| N+1 测试 (15) | ✓ | ✓ | ✓ | ✓ |
| API response preserved (10) | ✗ | ✓ | ✓ | ✗ |

## 分析

本任务产生**最大得分分化**：Gstack 90 vs Baseline/Superpowers 65（差距 25 分）。

- **Gstack 唯一做出了 SQL count 验证**：QA 角色可能推动了用 StatementInspector 或 datasource-proxy 统计 SQL 次数的测试，这是其他组都遗漏的关键项
- **Superpowers = Baseline（65）**：TDD + review 流程完全没有帮助——Superpowers 和裸 Agent 漏的项完全一样（SQL count 验证 + API response preserved）。brainstorming 和 plan 没有让 Agent 想到要验证 SQL 次数
- **OpenSpec 80**：Spec 中定义了 API 响应格式，所以保住了 API response preserved，但仍漏了 SQL count

## 结论
这是整轮评测最具区分度的任务。Gstack 的角色分工在性能优化任务上效果显著——QA 角色的"验证是否真的优化了"思维补上了其他 Harness 的盲区。Superpowers 虽然在流程上最严格，但"先写测试"并不能替代"知道要测什么"的领域知识。
