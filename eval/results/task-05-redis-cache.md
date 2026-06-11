# 任务⑤ Redis 缓存 评测总结

## 任务要求
为 GET /api/products/{id} 添加 Redis 缓存层：@Cacheable/@CacheEvict/@CachePut，TTL 可配置，@EnableCaching，Mock Redis 测试验证命中/未命中/失效。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| Baseline | 80 | 336s | 47.6K / 14.2K |
| **Superpowers** | **90** | **809s** | 91.3K / 22.2K |
| **Gstack** | **90** | **395s** | 52.2K / 15.7K |
| OpenSpec | 80 | 306s | 47.1K / 9.8K |

## 分析

首次出现分化：Superpowers 和 Gstack 各 90 分，Baseline 和 OpenSpec 仅 80 分。

- **Baseline 丢分**：缺少 TTL 配置或 @EnableCaching 注解
- **OpenSpec 丢分**：Spec 驱动可能过度关注缓存接口定义，忽略了 Spring 缓存配置细节
- **Gstack 90 分**：DevOps 角色关注配置项（TTL），Engineer 角色实现代码，协同覆盖更全
- **Superpowers 90 分**：TDD 流程确保测试覆盖，但流程开销大（13.5min / 113K token）

## 结论
涉及中间件集成 + 配置 + 测试的场景，Harness（尤其在 DevOps 角色下）比裸 Agent 覆盖更全面。Superpowers 得分与 Gstack 持平但 token 消耗翻倍。
