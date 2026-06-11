# 任务⑦ JWT 认证 评测总结

## 任务要求
从零实现 JWT 认证：register/login/refresh/logout、Token 黑名单、角色权限（ADMIN/USER）、拦截器、完整测试覆盖。

## 得分对比

| Provider | Score | Duration | Token(in/out) |
|----------|:-----:|:--------:|---------------|
| **Baseline** | **100** | **383s** | 42.0K / 21.6K |
| Superpowers | 95 | 643s | 99.0K / 33.1K |
| **Gstack** | **100** | **531s** | 54.0K / 25.3K |
| **OpenSpec** | **100** | **301s** | 62.1K / 17.2K |

## 分析

**Superpowers 是唯一丢分的**（95 vs 其余三组满分 100）。丢失的 5 分是代码质量中的"异常处理"检查——Superpowers 实现了所有功能但漏了全局异常处理（@ExceptionHandler），这可能是因为 TDD 流程中过分关注功能测试而忽略了横切关注点。

- **Baseline 100 分**：裸 Agent 在明确的 JWT 需求下能做到满分，且 token 最低
- **OpenSpec 100 分且最快**（5min）：Spec 驱动对 Auth 接口设计有天然优势（先在 Spec 中定义 token、refresh、role 等概念）
- **Gstack 100 分**：CEO（需求）+ Engineer（实现）+ QA（测试）+ DevOps（配置）角色链覆盖全面

## 结论
JWT 认证是需求边界清晰的任务，裸 Agent 表现最好（满分+最快+最低成本）。Superpowers 因为流程步骤多导致遗漏了全局异常处理，这是"流程密集但细节丢失"的典型案例。
