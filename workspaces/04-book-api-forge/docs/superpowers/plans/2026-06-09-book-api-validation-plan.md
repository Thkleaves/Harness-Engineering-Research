# Book API 请求参数校验 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Book API 的 POST 和 PUT 接口添加 Jakarta Bean Validation 参数校验，防止空数据进入系统。

**Architecture:** 方案 A — Book 模型字段加 Jakarta 校验注解，自定义 `@ISBN` 约束，`@RestControllerAdvice` 统一格式化校验错误为结构化 JSON。POST 用 `@Valid` 全量校验，PUT 注入 `Validator` 手动校验非 null 字段以支持部分更新。

**Tech Stack:** Spring Boot 3.4, Java 21, Maven, Jakarta Bean Validation (Hibernate Validator)

---

### Task 1: 添加 spring-boot-starter-validation 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 在 pom.xml dependencies 中添加 validation starter**

```xml
<!-- Bean Validation：参数校验（@Valid, @NotBlank 等） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

插入位置：`spring-boot-starter-web` 依赖之后、`spring-boot-starter-test` 之前。

- [ ] **Step 2: 验证依赖下载**

Run: `cd D:\Work\kleaves\Harness\workspaces\01-book-api && mvn dependency:resolve -q`
Expected: BUILD SUCCESS，无报错

- [ ] **Step 3: 验证编译通过**

Run: `cd D:\Work\kleaves\Harness\workspaces\01-book-api && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 2: 创建自定义 @ISBN 校验注解和校验器

**Files:**
- Create: `src/main/java/com/kleaves/demo/validation/ISBN.java`
- Create: `src/main/java/com/kleaves/demo/validation/ISBNValidator.java`

- [ ] **Step 1: 创建 ISBN 注解**

```java
package com.kleaves.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ISBNValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ISBN {

    String message() default "ISBN格式不正确，必须为10位或13位数字";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

- [ ] **Step 2: 创建 ISBNValidator 校验器**

```java
package com.kleaves.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ISBNValidator implements ConstraintValidator<ISBN, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 交给 @NotBlank 处理
        }
        // 去连字符
        String cleaned = value.replace("-", "").trim();
        if (cleaned.isEmpty()) {
            return false;
        }
        // 必须是 10 或 13 位，且全是数字
        return (cleaned.length() == 10 || cleaned.length() == 13)
                && cleaned.matches("\\d+");
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `cd D:\Work\kleaves\Harness\workspaces\01-book-api && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 3: Book 模型添加校验注解

**Files:**
- Modify: `src/main/java/com/kleaves/demo/model/Book.java`

- [ ] **Step 1: 添加 import**

在现有 import 区域后添加：
```java
import com.kleaves.demo.validation.ISBN;
import jakarta.validation.constraints.*;
```

- [ ] **Step 2: 给字段加注解**

将现有的字段声明替换为带注解版本：

```java
private Long id;

@NotBlank(message = "书名不能为空")
private String title;

@NotBlank(message = "作者不能为空")
private String author;

@NotBlank(message = "ISBN不能为空")
@ISBN
private String isbn;

@NotNull(message = "价格不能为空")
@Positive(message = "价格必须大于0")
@Digits(integer = 10, fraction = 2, message = "价格最多保留两位小数")
private Double price;
```

注意：只改字段声明上的注解，构造方法、getter/setter 保持不变。

- [ ] **Step 3: 验证编译**

Run: `cd D:\Work\kleaves\Harness\workspaces\01-book-api && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 4: 创建全局异常处理器

**Files:**
- Create: `src/main/java/com/kleaves/demo/controller/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建 GlobalExceptionHandler**

```java
package com.kleaves.demo.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @Valid 触发的校验失败（Controller 方法参数校验）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("field", fieldError.getField());
            error.put("message", fieldError.getDefaultMessage());
            errors.add(error);
        });
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 处理手动 Validator 触发的校验失败（PUT 部分更新）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            Map<String, String> error = new LinkedHashMap<>();
            // 取属性路径的最后一段作为 field 名
            String propertyPath = violation.getPropertyPath().toString();
            String field = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                    : propertyPath;
            error.put("field", field);
            error.put("message", violation.getMessage());
            errors.add(error);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd D:\Work\kleaves\Harness\workspaces\01-book-api && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 5: Controller 改造 — POST 加 @Valid，PUT 手动校验

**Files:**
- Modify: `src/main/java/com/kleaves/demo/controller/BookController.java`

- [ ] **Step 1: 添加 import**

在现有 import 区域后添加：
```java
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.Set;
```

- [ ] **Step 2: 注入 Validator**

在 `@Autowired private BookService bookService;` 之后添加：
```java
@Autowired
private Validator validator;
```

- [ ] **Step 3: POST create() 加 @Valid**

将 `create` 方法签名从：
```java
public ResponseEntity<Book> create(@RequestBody Book book) {
```
改为：
```java
public ResponseEntity<Book> create(@Valid @RequestBody Book book) {
```

- [ ] **Step 4: PUT update() 改为手动按需校验**

将整个 `update` 方法替换为：

```java
@PutMapping("/{id}")
public ResponseEntity<Book> update(@PathVariable Long id, @RequestBody Book book) {
    // 部分更新：只校验传入的非 null 字段
    Set<ConstraintViolation<Book>> violations = new HashSet<>();

    if (book.getTitle() != null) {
        violations.addAll(validator.validateProperty(book, "title"));
    }
    if (book.getAuthor() != null) {
        violations.addAll(validator.validateProperty(book, "author"));
    }
    if (book.getIsbn() != null) {
        violations.addAll(validator.validateProperty(book, "isbn"));
    }
    if (book.getPrice() != null) {
        violations.addAll(validator.validateProperty(book, "price"));
    }

    if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
    }

    return bookService.update(id, book)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

- [ ] **Step 5: 验证编译**

Run: `cd D:\Work\kleaves\Harness\workspaces\01-book-api && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 6: 手动验证校验行为

- [ ] **Step 1: 启动应用**

Run: `cd D:\Work\kleaves\Harness\workspaces\01-book-api && mvn spring-boot:run`

- [ ] **Step 2: 测试 POST 空数据**

```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"","author":"","isbn":"","price":null}'
```

Expected: 400 + 多字段错误（书名不能为空、作者不能为空、ISBN不能为空、价格不能为空）

- [ ] **Step 3: 测试 POST 无效 isbn 和负价格**

```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"测试","author":"测试","isbn":"123","price":-5}'
```

Expected: 400 + isbn 格式错误 + 价格必须大于 0

- [ ] **Step 4: 测试 POST 正常数据**

```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"活着","author":"余华","isbn":"978-7-5302-2155-6","price":29.90}'
```

Expected: 201 Created

- [ ] **Step 5: 测试 PUT 部分更新（只改 price）**

```bash
curl -X PUT http://localhost:8080/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{"price":35.50}'
```

Expected: 200 OK，返回完整的 Book，price 已更新为 35.50

- [ ] **Step 6: 测试 PUT 传入空字符串 title**

```bash
curl -X PUT http://localhost:8080/api/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"","author":"余华"}'
```

Expected: 400 + title 校验错误，author 正常不报错

- [ ] **Step 7: 停止应用**

Ctrl+C
