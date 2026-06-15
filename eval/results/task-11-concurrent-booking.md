# 任务⑪ 并发预订 评测总结

## 任务要求
实现并发安全的票务预订：@Version 乐观锁、捕获 OptimisticLockException、冲突自动重试 3 次、100 线程并发测试验证无超卖。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 75 | 387s | 43.9K / 18.7K |
| **Gstack** | **85** | **330s** | 41.0K / 13.6K |
| wow-harness | 85 | — | — |
| OpenSpec | 75 | 1051s | 62.9K / 48.9K |
| Superpowers | 75 | 503s | 100.0K / 20.8K |

## 诊断详情

| 检查项 | Baseline | Gstack | OpenSpec | Superpowers |
|--------|:--:|:--:|:--:|:--:|
| @Version 乐观锁 (15) | ✓ | ✓ | ✓ | ✓ |
| OptimisticLockException 处理 (10) | ✗ | ✓ | ✗ | ✗ |
| retry 机制 (10) | ✓ | ✓ | ✓ | ✓ |
| database index (5) | ✗ | ✗ | ✗ | ✗ |
| 100线程并发测试 (15) | ✓ | ✓ | ✓ | ✓ |
| quantity assertion (5) | ✓ | ✓ | ✓ | ✓ |

## 分析

Gstack 以 85 分领先，是**唯一正确处理 OptimisticLockException 的组**。竞争条件处理包括两个层次：加锁（@Version）+ 处理锁冲突（catch OptimisticLockException 并重试）。Baseline、OpenSpec、Superpowers 都只做了前半部分。

- **Baseline/Superpowers/OpenSpec（75）**：三组再次同分，都漏了异常捕获。Superpowers 的 TDD 虽测了并发但没有覆盖锁冲突的异常路径
- **Gstack 最均衡**：330s 最快，且 Engineering + QA 的组合确保了实现+测试的完整性
- **OpenSpec 耗时最长**（17.5min）：Spec 中可能过度设计了并发模型，实际代码产出并未更优
- **无人加 database index**：四组都漏了 5 分的数据库索引

- **wow-harness (85)** 与 Gstack 并列最高（+10 vs Baseline），@Version 乐观锁 + 重试 + 100 线程并发测试覆盖完整

## 结论
并发安全是"80% 容易、20% 难"的任务——大多数组完成了乐观锁本身，但只有 Gstack 的 Engineer/QA 组合补上了异常处理。这说明 Harness 价值在于**覆盖同一问题的不同角色视角**，而非流程步骤的多少。
