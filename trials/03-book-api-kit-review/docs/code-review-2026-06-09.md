# /review-this-pr 报告 — 03-book-api-kit-review 基线审查

**日期**: 2026-06-09
**审查范围**: 初始提交 `78701e3` — 整个代码库 (9 个 Java 源文件)
**审查者**: Claude Code /review-this-pr

---

- **base**: `78701e3` (初始提交，无 diff)
- **changed files**: 9 个 Java 源文件 (整个代码库)
- **structural-test**: 跳过 (无适配器入口)
- **baseline delta**: N/A（无基线）
- **overall**: ⚠️ 需改进

---

## 项目概要

Spring Boot 3.4 + Java 21 的 Book CRUD REST API，集成 Jakarta Bean Validation 参数校验（自定义 `@ISBN`、`@PriceFormat` 注解），全局异常处理器统一格式化校验错误为结构化 JSON。整体架构清晰，代码可读性好，但存在以下需要改进的问题。

---

## 🔴 严重问题

### 1. 零测试覆盖

`src/test/` 目录完全为空，Maven 构建输出 `No tests to run.`。这违反了项目黄金原则 #4（每个 feature 必须有端到端测试）。[feature_list.json](.harness/feature_list.json) 中两个 feature 均标记为 `passes: false`，没有任何 evidence 文件。

**建议**: 至少添加以下测试：
- `POST /api/books` 正常创建 → 201
- `POST /api/books` 空字段 → 400 + 多字段错误
- `POST /api/books` 无效 ISBN → 400
- `PUT /api/books/{id}` 部分更新 → 200
- `PUT /api/books/{id}` 不存在 → 404
- `GET /api/books` → 200 + 列表

### 2. `price` 字段使用 `Double` 类型 — 精度风险

[Book.java:36-37](src/main/java/com/kleaves/demo/model/Book.java#L36-L37) 中 `price` 声明为 `Double`。金融/价格场景中 `Double` 存在浮点精度问题（如 `0.1 + 0.2` 场景），业界标准是使用 `BigDecimal`。

`PriceFormatValidator` 虽然内部使用了 `BigDecimal.valueOf(value).scale()` 做校验，但 `Double` 在 JSON 反序列化时已经丢失了精度（例如 `19.90` 被反序列化为 `19.9`）。

**建议**: 将 `price` 改为 `BigDecimal`，配合 Jackson 的 `@JsonFormat` 或自定义反序列化器确保精度。

### 3. 设计文档与实现不一致

设计文档（[design spec](docs/superpowers/specs/2026-06-09-book-api-validation-design.md)）规定 `price` 使用 `@Digits(integer=10, fraction=2)`，但实际实现使用了自定义的 `@PriceFormat` 注解。虽然自定义实现更好（解决了 `@Digits` 与 `Double` 的兼容性问题），但设计文档未更新，会造成后续维护者的困惑。

---

## 🟡 中等问题

### 4. 字段注入而非构造器注入

[BookController.java:36-40](src/main/java/com/kleaves/demo/controller/BookController.java#L36-L40) 使用 `@Autowired` 字段注入。Spring 官方推荐构造器注入：
- 依赖关系更明确
- 支持 `final` 字段，防止意外修改
- 单元测试不需要 Spring 容器

**建议**：
```java
private final BookService bookService;
private final Validator validator;

public BookController(BookService bookService, Validator validator) {
    this.bookService = bookService;
    this.validator = validator;
}
```

### 5. `update` 方法的手动校验脆弱且冗长

[BookController.java:115-140](src/main/java/com/kleaves/demo/controller/BookController.java#L115-L140) 的 PUT `update` 方法对每个字段逐个进行 `if (field != null)` + `validator.validateProperty()`。新增字段时必须同步修改此处，容易遗漏。

**建议**：使用 Jakarta Validation 的 `@Valid` + 分组（groups）机制，或定义一个专门的 `BookUpdateRequest` DTO 来区分全量校验和部分更新场景。

### 6. `BookService.save()` 有副作用

[BookService.java:78-82](src/main/java/com/kleaves/demo/service/BookService.java#L78-L82) 的 `save` 方法直接修改传入的 `book` 对象（`book.setId(id)`），这是一种隐式副作用。调用者传入的对象被意外修改。

**建议**：创建新实例返回：
```java
public Book save(Book book) {
    long id = idGenerator.getAndIncrement();
    Book saved = new Book(id, book.getTitle(), book.getAuthor(), book.getIsbn(), book.getPrice());
    bookStore.put(id, saved);
    return saved;
}
```

### 7. `findByAuthor(null)` 返回全部数据 — 语义歧义

[BookService.java:66-73](src/main/java/com/kleaves/demo/service/BookService.java#L66-L73) 当 `author` 为 null 或 blank 时返回全部图书。这掩盖了调用方的 bug — 调用方可能无意中传入了 null。

**建议**：返回空列表或抛出 `IllegalArgumentException`，让调用方显式处理空查询。

---

## 🟢 轻微建议

### 8. `isbn` 空字符串绕过 `@NotBlank`

[ISBNValidator.java:22-25](src/main/java/com/kleaves/demo/validation/ISBNValidator.java#L22-L25) 在 `cleaned.isEmpty()` 时返回 `false`，这是好的防御性编程。但 `@NotBlank` 已经确保了非空 — 这个检查只在 PUT 部分更新场景（`@NotBlank` 不执行）时才有意义。建议在注释中明确说明。

### 9. `GlobalExceptionHandler` 缺少日志记录

[GlobalExceptionHandler.java:30-41](src/main/java/com/kleaves/demo/controller/GlobalExceptionHandler.java#L30-L41) 两个异常处理方法都没有日志输出。在生产环境中，校验失败是重要的运维信号（可能是恶意输入）。

**建议**：添加 `log.warn("Validation failed: {}", errors)`。

### 10. Java 包结构不匹配 harness 层定义

[.harness/config.json](.harness/config.json) 定义了 `types → config → repo → service → runtime → ui` 六层，但实际 Java 包结构是 `model`、`controller`、`service`、`validation`。虽然 Java 项目不一定严格按 harness 层组织包名，但明确映射有助于 harness 工具链（structural-test、fitness rules）正常工作。

### 11. `application.yml` 中的自定义配置未被使用

[application.yml](src/main/resources/application.yml) 定义了 `book.currency: CNY` 和 `book.default-page-size: 20`，但代码中没有任何地方读取这些配置。要么删除以避免误导，要么实现对应的功能。

### 12. ISBN 校验过于宽松

[ISBNValidator.java:27-28](src/main/java/com/kleaves/demo/validation/ISBNValidator.java#L27-L28) 只校验去连字符后为 10 或 13 位数字。真实 ISBN-10 有校验和算法（最后一位可能是 `X`），ISBN-13 也有 EAN-13 校验位。当前实现接受 `0000000000` 这种无效 ISBN。

**建议**：如果只需基本格式校验，在注释中说明这是"格式校验"而非"有效性校验"。如需严格校验，添加校验和算法。

---

## 📊 总结

| 维度 | 评级 | 说明 |
|------|------|------|
| 代码可读性 | ✅ 良好 | 注释详细，命名清晰 |
| 架构合规 | ⚠️ 需改进 | 构造器注入、DTO 分离可加强 |
| 测试覆盖 | 🔴 不足 | 零测试，违反黄金原则 |
| 数据正确性 | ⚠️ 需改进 | `Double` 价格有精度风险 |
| 异常处理 | ✅ 良好 | 统一格式，覆盖两种校验场景 |
| 安全 | ✅ 基本安全 | 输入校验到位，无 SQL 注入风险（内存存储） |

**建议优先处理**：添加测试 → 将 `price` 改为 `BigDecimal` → 更新设计文档。

<!-- machine-tail: {"base":"78701e3c9b155921d4714e17d5ac02e87c250d19","changed_files":9,"violations":0,"baseline_delta":0,"passed":true} -->
