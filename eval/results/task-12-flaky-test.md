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

四组**完全同分 70**——这是整轮评测唯一一组得分完全一致的 L4 任务。

所有组正确引入了 Clock 注入、Clock.fixed()、多时区参数化测试。但"移除 LocalDate.now()"检查项**全部未通过**。

### ⚠️ 评分脚本缺陷发现（2026-06-15 Reasonix 沙箱验证）

事后审查 `verify.sh` 中该检查的逻辑：

```bash
# 旧逻辑（有 Bug）
grep -rl "LocalDate.now" src --include="*.java" | grep -v "test" | grep -v "import"
```

该 grep 使用 `LocalDate.now` 匹配，**无法区分**以下两种情况：
- `LocalDate.now()` ← 真正的 Bug（无 Clock 参数）
- `LocalDate.now(clock)` ← **正确修复**（Clock 注入）

所有 Provider 的代码实际上都用 `LocalDate.now(clock)` 正确修复了，但因为 grep 误匹配全被判为"未移除"。此外，JavaDoc 注释中的 `LocalDate.now()` 也会被误匹配。

修复后：将 grep 改为 `LocalDate\.now()`（仅匹配空括号）并排除注释行，该检查恢复正常。详见 `eval/scripts/verify.sh` 任务 12 case。

Reasonix 沙箱重跑确认：修复 verify.sh 后，正确实现的 wow-harness 12-flaky-test 可达 **100 分**（旧脚本误扣的 15+15 分已修正）。

### 修正后的结论

原结论"所有 Harness 都推不动改旧代码"不完全准确——**Clock 注入 + LocalDate.now()→LocalDate.now(clock) 确实是正确的"改旧代码"**，只是评分脚本无法识别。真正的共性问题（④条仍然有效）是所有 Agent 倾向"加新代码"而非"全局重构"。但在这个具体任务上，各组很可能已经正确完成修复。
