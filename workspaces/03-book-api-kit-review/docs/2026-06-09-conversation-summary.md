# 对话总结：Book API 请求参数校验

**日期**：2026-06-09
**任务**：为 Book API 项目的 POST 和 PUT 接口添加请求参数校验

| 阶段 | 时间（UTC+8） |
|------|---------------|
| 需求澄清（6 轮问答） | 16:55–16:58 |
| 方案对比 & 选择 A | 16:58–16:59 |
| 设计文档写入 | 17:00 |
| 实现计划写入 | 17:00 |
| Task 1：添加 validation 依赖 | 17:01 |
| Task 2：创建 @ISBN 注解+校验器 | 17:02 |
| Task 3：Book 模型加校验注解 | 17:02 |
| Task 4：创建 GlobalExceptionHandler | 17:03 |
| Task 5：Controller 改造 | 17:04 |
| 编译验证 | 17:04 |
| 首次启动（端口冲突） | 17:01 |
| 杀旧进程 + 二次启动 | 17:06 |
| 首轮 API 验证（发现 @Digits 不兼容 Double） | 17:07 |
| 修复 @PriceFormat 替代 @Digits | 17:07 |
| 再次编译 + 重启 + 验证通过 | 17:07 |
| 清理进程、写总结 | 17:15 |

---

## 1. 需求澄清过程 `16:55–16:58`

在写任何代码之前，通过逐轮问答澄清了以下需求：

| # | 问题 | 选择 | 说明 |
|---|------|------|------|
| 1 | 哪些字段必填？ | A（全部四个） | title, author, isbn, price 全部必填 |
| 2 | "不为空"校验粒度？ | C | 非 null + 去首尾空格后不为空 |
| 3 | price 额外校验？ | D | 非 null + >0 + 最多两位小数 |
| 4 | isbn 格式校验？ | C | 去连字符后必须 10 或 13 位数字，不验证校验位 |
| 5 | 错误响应格式？ | C | 结构化 JSON，一次返回所有字段错误 |
| 6 | PUT 接口行为？ | C | 部分更新 + 宽松：传入的字段校验，null 字段跳过 |

---

## 2. 方案选择 `16:58–16:59`

三选一，选择了 **方案 A — Jakarta Bean Validation 注解 + 全局异常处理器**：

- 方案 B（手动 Service 校验）被否：代码量大，重复代码多
- 方案 C（DTO 分离）被否：多出 DTO 层，对小型项目过度设计
- **方案 A 胜出**：Spring 标准做法，声明式、代码量最少

---

## 3. 实现细节 `17:01–17:06`

### 设计文档 `17:00`

- [设计文档](docs/superpowers/specs/2026-06-09-book-api-validation-design.md) 写入
- [实现计划](docs/superpowers/plans/2026-06-09-book-api-validation-plan.md) 写入
- 转换为 TodoWrite 任务列表，6 个 Task 逐步执行

### 新增文件（5 个）

- `validation/ISBN.java` — 自定义 @ISBN 校验注解
- `validation/ISBNValidator.java` — ISBN 校验器（去连字符 → 长度 10/13 → 全数字）
- `validation/PriceFormat.java` — 自定义 @PriceFormat 校验注解
- `validation/PriceFormatValidator.java` — 价格格式校验器（BigDecimal.valueOf.scale() <= 2）
- `controller/GlobalExceptionHandler.java` — 全局异常处理器，统一返回 `{"errors": [...]}`

### 修改文件（3 个）

- `pom.xml` — 加 `spring-boot-starter-validation` 依赖
- `Book.java` — 字段加 `@NotBlank`、`@ISBN`、`@NotNull`、`@Positive`、`@PriceFormat`
- `BookController.java` — POST 加 `@Valid`，PUT 注入 `Validator` 手动按需校验

---

## 4. 遇到的坑 & 修复 `17:07`

### 坑：`@Digits` 不支持 `Double` 类型 `17:07`

原设计用 `@Digits(integer=10, fraction=2)` 校验 price 的小数位数，但 Jakarta Bean Validation 的 `@Digits` 只支持 `BigDecimal`、`BigInteger`、`String` 和整数框架类型，对 `Double` 会抛内部异常，绕过全局异常处理器，返回 Spring 默认 400 页面。

**修复**：用自定义 `@PriceFormat` + `PriceFormatValidator` 替代，通过 `BigDecimal.valueOf(value).scale() <= 2` 精确校验小位数。

---

## 5. 验证结果 `17:07`

| # | 测试时间 | 测试用例 | 预期 | 实际 |
|---|----------|------|------|
| 1 | 17:07:04 | POST `title="" author="" isbn="" price=null` | 400 + 5 字段错误 | ✅ |
| 2 | 17:07:25 | POST `isbn=123 price=-5` | 400 + isbn + price 错误 | ✅ |
| 3 | 17:07:42 | POST 合法数据 | 201 Created | ✅ |
| 4 | 17:07:58 | PUT `price=35.50`（部分更新） | 200 + 更新后 book | ✅ |
| 5 | 17:08:12 | PUT `title=""`（部分更新 + 空白字段） | 400 + 仅 title 错误 | ✅ |

---

## 6. 关键决策记录

1. **PUT 部分更新 + 宽松校验**：不传的字段不校验，传入的字段按规则校验。因为用 `Validator.validateProperty()` 而非 `@Valid`，完美支持"只改一个字段"的场景。

2. **null 校验排后**：自定义校验器（ISBNValidator、PriceFormatValidator）对 null 值返回 `true`，把 null 报错交给 `@NotBlank`/`@NotNull` 处理，确保错误信息精准。

3. **中文错误消息**：所有校验注解的 `message` 使用中文，直接面向调用方可读。

---

## 关联文件

- 设计文档：[docs/superpowers/specs/2026-06-09-book-api-validation-design.md](docs/superpowers/specs/2026-06-09-book-api-validation-design.md)
- 实现计划：[docs/superpowers/plans/2026-06-09-book-api-validation-plan.md](docs/superpowers/plans/2026-06-09-book-api-validation-plan.md)
