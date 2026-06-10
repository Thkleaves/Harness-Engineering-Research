# 任务④ 对比记录 — Superpowers vs 裸 Agent

> 日期: 2026-06-09 | 两个独立会话, 同一项目基线, 同一需求

## 实验条件

| | Superpowers 会话 | 裸 Agent 会话 |
|------|:--:|:--:|
| Prompt | "帮我把 Book API 项目的请求参数加上校验，别让空数据进来。**先不要写代码，先澄清需求。**" | "帮我把 Book API 项目的请求参数加上校验，别让空数据进来。" |
| 最后一句 | 有刹车句 → 触发 brainstorming | 无 → 直接动手 |
| 耗时 | ~20 min (16:55–17:15) | ~3-4 min |

## 逐项对比

### 1. 架构决策

| | Superpowers | 裸 Agent |
|---|:--:|:--:|
| 方案 | 直接注解 Book 实体（三选一后决定） | 直接注解 Book 实体（未经澄清，直接选） |
| 是否建 DTO | 否 | 否 |

**结论：两者做了相同的架构选择。这个层级上 Harness 不影响判断。**

### 2. 校验深度

| 字段 | Superpowers | 裸 Agent |
|------|:--:|:--:|
| title | `@NotBlank` | `@NotBlank` |
| author | `@NotBlank` | `@NotBlank` |
| isbn | `@NotBlank` + 自定义 `@ISBN`（去连字符 → 10/13 位纯数字） | `@NotBlank`（无格式校验） |
| price | `@NotNull` + `@Positive` + 自定义 `@PriceFormat`（最多 2 位小数） | `@NotNull` + `@Positive`（无小数位校验） |

**差异：Superpowers 通过 brainstorming 追问出了 ISBN 格式和价格小数位需求，裸 Agent 没人问就没做。**

### 3. 异常处理

| Superpowers | 裸 Agent |
|-------------|----------|
| 新建 `GlobalExceptionHandler`，捕获 `MethodArgumentNotValidException` 和 `ConstraintViolationException`，返回 `{"errors": [{"field":"...","message":"..."}]}` | 无。依赖 Spring 默认 400 页面 |

### 4. PUT 部分更新

| Superpowers | 裸 Agent |
|-------------|----------|
| 注入 `Validator`，逐字段 `validateProperty()`，只校验传入的非 null 字段，抛出 `ConstraintViolationException` | 无任何校验，原样透传给 Service |

### 5. 额外行为

| Superpowers | 裸 Agent |
|-------------|----------|
| 未超出需求范围 | 加了 `@Validated` 在类上、`@Positive` 在 `@PathVariable Long id`、`@NotBlank` 在 `@RequestParam author`——需求没提这些 |

### 6. 测试

| Superpowers | 裸 Agent |
|-------------|----------|
| 5 个手动 curl 验证（记录在对话总结中） | 0 |

### 7. 文档

| Superpowers | 裸 Agent |
|-------------|----------|
| spec.md + plan.md + conversation-summary.md | 0 |

### 8. 踩坑与修复

| Superpowers | 裸 Agent |
|-------------|----------|
| 发现 `@Digits` 不兼容 `Double`，自定义 `@PriceFormat` 修复 | 无此类问题（没做价格小数位校验，自然不会遇到） |

## 结论

1. **架构决策层面两者没有差异。** 模型自己知道该选什么方案。

2. **差异不在"会不会做"，在"做了多深"。** Superpowers 的 brainstorming 把一句"加校验"追问出了 6 个具体选择——ISBN 格式、价格小数位、PUT 部分更新策略、错误响应格式。裸 Agent 没问，用户也没说，这些细节就丢了。

3. **裸 Agent 会擅自扩大范围。** 加了 PathVariable 和 RequestParam 校验，需求没要求这些——没有 brainstorming 的边界确认，Agent 按自己的直觉走。

4. **坑的发现需要深度。** `@Digits` 不兼容 `Double` 这个坑，裸 Agent 根本不会遇到——因为它没做价格小数位校验。越深入越能发现真问题。

5. **文档是 Superpowers 最确定的产出差异。** 不管做得多好或多差，spec + plan 留下来了，别人能 Review、能复用、能追责。
