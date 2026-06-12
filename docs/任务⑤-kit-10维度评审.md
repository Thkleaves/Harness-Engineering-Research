# 任务⑤ — agent-harness-kit 10 维度评审总结

> 日期: 2026-06-09 | 审查对象: 03-book-api-kit-review（即 01-book-api 的副本）
> 调用方式: `/review-this-pr`

## 执行流程

```
1. cd 03-book-api-kit-review
2. npx agent-harness-kit init --yes     → 生成 .claude/ + .harness/ + CLAUDE.md
3. 新开会话，输入 /review-this-pr       → 自动驱动 10 agent 评审
```

## 评审结果摘要

| 维度 | 评级 |
|------|:----:|
| 代码可读性 | ✅ 良好 |
| 架构合规 | ⚠️ 需改进 |
| 测试覆盖 | 🔴 零测试 |
| 数据正确性 | ⚠️ Double 精度风险 |
| 异常处理 | ✅ 良好 |
| 安全 | ✅ 基本安全 |

## 发现的 12 个问题

### 🔴 严重（3 个）
1. **零测试覆盖** — src/test/ 为空，违反黄金原则 #4
2. **price 用 Double** — 浮点精度风险，JSON 反序列化时 19.90 → 19.9
3. **设计文档与实现不一致** — spec 写 @Digits，代码用 @PriceFormat

### 🟡 中等（4 个）
4. **字段注入而非构造器注入** — Spring 官方不推荐
5. **PUT 手动逐字段校验** — 脆弱，新增字段易遗漏
6. **BookService.save() 有副作用** — 直接修改入参对象
7. **findByAuthor(null) 返回全量** — 掩盖调用方 bug

### 🟢 轻微（5 个）
8. ISBN 空字符串防御检查意义不明确
9. GlobalExceptionHandler 缺少日志
10. Java 包结构与 harness 六层定义不匹配
11. application.yml 未使用的配置项
12. ISBN 校验过于宽松（无校验和）

## 与 Superpowers Review 的对比

| | Superpowers（任务③） | agent-harness-kit（任务⑤） |
|---|---|---|
| 评审维度 | 2（规范合规 + 代码质量） | 10（但本次主要靠 advisor/security/architecture） |
| 发现问题数 | 较少（流程中 TDD 前置避免了大部分） | 12 个 |
| 深度 | 聚焦功能是否正确实现 | 覆盖精度/副作用/依赖注入/配置冗余 |
| 独特发现 | — | Double 精度、save() 副作用、构造器注入、ISBN 校验和 |
| 输出格式 | 通过/修改 二元 | 严重/中等/轻微 三级 + machine-tail JSON |
| 后续机制 | 无 | Failures → Rules（可自动将发现转为规则） |
| Token 消耗 | 较低 | 较高（10 agent 并行） |

## 关键发现

kit 评审发现了 Superpowers 双阶段 Review 没覆盖到的领域：
- **数据类型选择**（Double vs BigDecimal）— 安全/可靠性视角
- **副作用检测**（save() 修改入参）— 代码质量视角
- **依赖注入模式**（字段 vs 构造器）— 架构合规视角
- **ISBN 校验和算法** — 领域深度视角

**结论: kit 和 Superpowers 互补性强。Superpowers 保证"做对的事"，kit 保证"事做得对"。**
