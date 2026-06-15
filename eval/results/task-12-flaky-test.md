# 任务⑫ Flaky 测试修复 评测总结

## 任务要求
修复时区相关的 Flaky 测试：Clock 注入替代 LocalDate.now()、Clock.fixed() 固定时间、参数化测试覆盖 UTC/Asia/Shanghai/America/New_York 三个时区。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 70 | 324s | 38.0K / 14.6K |
| Superpowers | 70 | 412s | 61.1K / 15.5K |
| Gstack | 70 | 473s | 53.7K / 16.3K |
| wow-harness | 70 | — | — |
| OpenSpec | 70 | 243s | 41.8K / 11.5K |

## 诊断详情

| 检查项 | Baseline | Superpowers | Gstack | OpenSpec |
|--------|:--:|:--:|:--:|:--:|
| Clock 注入 (15) | ✓ | ✓ | ✓ | ✓ |
| Clock.fixed() in test (10) | ✓ | ✓ | ✓ | ✓ |
| 时区处理 (10) | ✓ | ✓ | ✓ | ✓ |
| 多时区参数化测试 (10) | ✓ | ✓ | ✓ | ✓ |
| 移除 LocalDate.now() (15) | ✗ | ✗ | ✗ | ✗ |

## 分析

四组**完全同分 70**——这是整轮评测唯一一组得分完全一致的 L4 任务，也是唯一一个四组漏了同一项的案例。

所有组正确引入了 Clock 注入、Clock.fixed()、多时区参数化测试，但**无一组彻底移除主代码中的 LocalDate.now() 调用**。这可能是因为 Agent 倾向于"添加 Clock 注入"而非"全局替换 LocalDate.now()"——这是一个代码惯性的问题，所有 Harness 的引导都未能打破。

- **OpenSpec 最快**（4min）：任务明确，Spec 直接指导了修复方案
- **Superpowers/Baseline/Gstack 耗时波动小**：各组实现路径高度趋同

## 结论
这是 Harness 评测最有启发性的结果：当问题需要**全局代码重构**（替换所有 LocalDate.now()）而不仅仅是**添加新代码**（Clock 注入）时，所有 Harness 都无法引导 Agent 完成重构。这说明当前 Harness 更擅长"加功能"而不是"全局替换"。
