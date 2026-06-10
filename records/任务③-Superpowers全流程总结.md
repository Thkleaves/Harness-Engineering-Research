# 任务③ Superpowers 全流程总结

> 日期: 2026-06-09 | 独立会话 | 耗时: ~20 min (16:55–17:15 UTC+8)
> Prompt: "帮我把 Book API 项目的请求参数加上校验，别让空数据进来。先不要写代码，先澄清需求。"

## 实际流程

```
16:55  brainstorming — 6 轮苏格拉底式提问
16:58  方案三选一 → 选 A（Jakarta Bean Validation + 全局异常处理）
17:00  设计文档 + 实现计划写入
17:01  加 validation 依赖
17:02  创建 @ISBN 自定义注解 + 校验器，Book 模型加校验注解
17:03  创建 GlobalExceptionHandler
17:04  Controller 改造，编译通过
17:07  手动 curl 验证 → 发现 @Digits 不兼容 Double
17:07  创建 @PriceFormat 自定义注解替代，重新验证通过
17:15  收尾
```

## 6 轮需求澄清

| # | 问题 | 选择 |
|---|------|------|
| 1 | 哪些字段必填？ | title、author、isbn、price 全部 |
| 2 | "不为空"的粒度？ | 非 null + 去首尾空格后不为空 |
| 3 | price 额外约束？ | 非 null + >0 + 最多两位小数 |
| 4 | isbn 格式？ | 去连字符后必须 10 或 13 位数字 |
| 5 | 错误响应格式？ | 结构化 JSON，一次返回全部字段错误 |
| 6 | PUT 更新行为？ | 部分更新，传入的字段校验，null 跳过 |

## 方案选择

三选一，选了 **A — Jakarta Bean Validation 直接注解 Book 实体 + 全局异常处理器**。B（手动 Service 校验）代码量大被否，C（DTO 分离）对小型项目过度设计被否。

## 最终产出

| 操作 | 文件 |
|:--:|------|
| ➕ | `validation/ISBN.java` — 自定义 @ISBN 注解 |
| ➕ | `validation/ISBNValidator.java` — 去连字符 → 长度 10/13 → 全数字 |
| ➕ | `validation/PriceFormat.java` — 自定义 @PriceFormat 注解 |
| ➕ | `validation/PriceFormatValidator.java` — BigDecimal.valueOf.scale() <= 2 |
| ➕ | `controller/GlobalExceptionHandler.java` — 统一返回 `{"errors": [{"field":...,"message":...}]}` |
| ✏️ | `Book.java` — 加 @NotBlank/@ISBN/@NotNull/@Positive/@PriceFormat |
| ✏️ | `BookController.java` — POST 加 @Valid，PUT 注入 Validator 手动逐字段校验 |
| ✏️ | `pom.xml` — 加 spring-boot-starter-validation |
| 📄 | 设计文档 + 实现计划 |

## 遇到的坑

`@Digits(integer=10, fraction=2)` 不支持 `Double` 类型——只支持 BigDecimal/BigInteger/String。对 Double 抛内部异常绕过全局处理器。修复：自定义 `@PriceFormat` + `PriceFormatValidator`。

## 验证

5 个手动 curl 测试通过。无自动化测试。

## 产出文档

- 设计文档: `docs/superpowers/specs/2026-06-09-book-api-validation-design.md`
- 实现计划: `docs/superpowers/plans/2026-06-09-book-api-validation-plan.md`
- 对话总结: `docs/2026-06-09-conversation-summary.md`
