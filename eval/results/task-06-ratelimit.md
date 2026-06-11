# 任务⑥ 限流 评测总结

## 任务要求
实现 Token Bucket 限流算法（手写、不用第三方库）：每个用户独立桶（X-User-Id）、60 req/min、429+Retry-After、可配置、线程安全、并发测试。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 90 | 268s | 42.5K / 9.6K |
| **Superpowers** | **100** | **928s** | 104.0K / 27.2K |
| Gstack | 90 | 442s | 48.3K / 21.4K |
| OpenSpec | 90 | 325s | 39.7K / 14.4K |

## 分析

Superpowers 唯一满分 100！Token Bucket 算法涉及线程安全（ConcurrentHashMap）、可配置桶参数、HTTP 429 状态码和 Retry-After 响应头——TDD 驱动的先测试再实现方法在算法实现类任务上表现最好。

- **Baseline 90 分**：裸 Agent 一次性手写算法，高效但漏掉部分细节（可能是 Retry-After 或并发测试的边界）
- **Gstack & OpenSpec 90 分**：与 Baseline 同分，Harness 流程未带来额外收益

## 结论
Superpowers 在需要算法实现+并发安全的场景下首次拿到满分。TDD 在类似"写代码→验证正确性"的算法类任务上价值最大。但代价是耗时最长（15min）和最高 token 消耗（131K）。
