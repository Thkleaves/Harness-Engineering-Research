# Book API 请求参数校验 — 设计文档

**日期**: 2026-06-09
**方案**: A — Jakarta Bean Validation 注解 + 全局异常处理器

## 需求摘要

为 Book API 的 POST 和 PUT 接口增加请求参数校验，防止空数据或无效数据进入系统。

## 校验规则

### 字段规则

| 字段 | 规则 | 校验注解 |
|------|------|----------|
| `title` | 非 null，trim 后不为空 | `@NotBlank` |
| `author` | 非 null，trim 后不为空 | `@NotBlank` |
| `isbn` | 非 null，trim 后不为空，去连字符后 10 或 13 位数字 | `@NotBlank` + `@ISBN`（自定义） |
| `price` | 非 null，> 0，最多两位小数 | `@NotNull` + `@Positive` + `@Digits(integer=10, fraction=2)` |

### 接口策略

| 接口 | 策略 |
|------|------|
| POST `/api/books` | 全量校验：四字段全部必填，用 `@Valid` 触发 |
| PUT `/api/books/{id}` | 部分更新 + 宽松：只校验传入的非 null 字段，用 `Validator` 手动触发 |

### 错误响应格式

```json
{
  "errors": [
    {"field": "title", "message": "书名不能为空"},
    {"field": "price", "message": "价格必须大于0"}
  ]
}
```

HTTP 状态码：400 Bad Request。

## 实现计划

### 1. 添加依赖

`pom.xml` 加 `spring-boot-starter-validation`。

### 2. Book 模型加注解

`Book.java` 的 `title`、`author`、`isbn`、`price` 字段加上 Jakarta Validation 注解。

### 3. 自定义 @ISBN 注解

新建 `validation/ISBN.java`（注解）和 `validation/ISBNValidator.java`（校验器）：
- 去连字符（`-`）
- 校验剩余字符串长度是 10 或 13，且全为数字

### 4. 全局异常处理器

新建 `controller/GlobalExceptionHandler.java`：
- `@RestControllerAdvice`
- 捕获 `MethodArgumentNotValidException`，提取每个字段错误，返回结构化 JSON
- 捕获 `ConstraintViolationException`（手动校验时触发），同样格式化

### 5. Controller 改造

- `POST create()`：参数加 `@Valid @RequestBody Book book`
- `PUT update()`：注入 `jakarta.validation.Validator`，遍历非 null 字段手动校验，收集违反约束的字段，抛 `ConstraintViolationException`（如有）

## 新增文件

- `src/main/java/com/kleaves/demo/validation/ISBN.java`
- `src/main/java/com/kleaves/demo/validation/ISBNValidator.java`
- `src/main/java/com/kleaves/demo/controller/GlobalExceptionHandler.java`

## 修改文件

- `pom.xml`
- `Book.java`
- `BookController.java`
