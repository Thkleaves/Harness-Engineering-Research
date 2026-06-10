# GET /api/books 分页与排序 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `GET /api/books` 和 `GET /api/books?author=xxx` 增加偏移分页（`?page=1&size=20`）和 JSON:API 风格排序（`?sort=-price,author`）。

**Architecture:** Controller 解析 HTTP 参数构造 `PageRequest` → 传入 Service → Service 获取全量数据、排序、分页截取 → 返回 `PageResponse<T>`。`SortParser` 负责将 `-price,author` 字符串解析为排序描述列表。

**Tech Stack:** Spring Boot 3.4, Java 21, JUnit 5, Mockito, MockMvc, 内存存储 (ConcurrentHashMap)

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/java/com/kleaves/demo/model/SortParser.java` | 创建 | 解析 `?sort=-price,author` 为排序描述 |
| `src/main/java/com/kleaves/demo/model/PageResponse.java` | 创建 | 分页响应通用包装 record |
| `src/main/java/com/kleaves/demo/service/BookService.java` | 修改 | 新增 `PageRequest` record，`findAll`/`findByAuthor` 支持分页排序 |
| `src/main/java/com/kleaves/demo/controller/BookController.java` | 修改 | 合并两个 GET 端点，解析分页排序参数 |
| `src/test/java/com/kleaves/demo/model/SortParserTest.java` | 创建 | SortParser 单元测试 |
| `src/test/java/com/kleaves/demo/service/BookServiceTest.java` | 创建 | BookService 分页排序测试 |

---

### Task 1: SortParser — 测试先行

**Files:**
- Create: `src/test/java/com/kleaves/demo/model/SortParserTest.java`
- Create: `src/main/java/com/kleaves/demo/model/SortParser.java`

- [ ] **Step 1: 编写 SortParser 失败测试**

```java
package com.kleaves.demo.model;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SortParserTest {

    @Test
    void parseNull_shouldReturnEmpty() {
        List<SortParser.SortOrder> result = SortParser.parse(null);
        assertThat(result).isEmpty();
    }

    @Test
    void parseEmpty_shouldReturnEmpty() {
        List<SortParser.SortOrder> result = SortParser.parse("");
        assertThat(result).isEmpty();
    }

    @Test
    void parseBlank_shouldReturnEmpty() {
        List<SortParser.SortOrder> result = SortParser.parse("   ");
        assertThat(result).isEmpty();
    }

    @Test
    void parseSingleAscending_shouldReturnOneOrder() {
        List<SortParser.SortOrder> result = SortParser.parse("price");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).field()).isEqualTo("price");
        assertThat(result.get(0).ascending()).isTrue();
    }

    @Test
    void parseSingleDescending_shouldReturnDescending() {
        List<SortParser.SortOrder> result = SortParser.parse("-price");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).field()).isEqualTo("price");
        assertThat(result.get(0).ascending()).isFalse();
    }

    @Test
    void parseMultipleMixed_shouldReturnAll() {
        List<SortParser.SortOrder> result = SortParser.parse("author,-price");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).field()).isEqualTo("author");
        assertThat(result.get(0).ascending()).isTrue();
        assertThat(result.get(1).field()).isEqualTo("price");
        assertThat(result.get(1).ascending()).isFalse();
    }

    @Test
    void parseInvalidField_shouldBeIgnored() {
        List<SortParser.SortOrder> result = SortParser.parse("invalidField");

        assertThat(result).isEmpty();
    }

    @Test
    void parseMixedValidAndInvalid_shouldKeepOnlyValid() {
        List<SortParser.SortOrder> result = SortParser.parse("price,invalid,author");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).field()).isEqualTo("price");
        assertThat(result.get(1).field()).isEqualTo("author");
    }

    @Test
    void parseWithSpaces_shouldTrimFields() {
        List<SortParser.SortOrder> result = SortParser.parse(" price , -author ");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).field()).isEqualTo("price");
        assertThat(result.get(0).ascending()).isTrue();
        assertThat(result.get(1).field()).isEqualTo("author");
        assertThat(result.get(1).ascending()).isFalse();
    }

    @Test
    void parseAllThreeValidFields() {
        List<SortParser.SortOrder> result = SortParser.parse("title,author,price");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).field()).isEqualTo("title");
        assertThat(result.get(1).field()).isEqualTo("author");
        assertThat(result.get(2).field()).isEqualTo("price");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && mvn test -Dtest=SortParserTest
```
Expected: 编译失败（`SortParser` 类不存在）。

- [ ] **Step 3: 实现 SortParser**

```java
package com.kleaves.demo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 排序参数解析工具
 *
 * 输入格式: JSON:API 风格 —— 逗号分隔字段名，"-" 前缀表示降序
 * 示例: "author,-price" → 先按作者升序，再按价格降序
 *
 * 支持的字段: title, author, price
 * 非法字段静默忽略
 */
public class SortParser {

    private static final Set<String> ALLOWED_FIELDS = Set.of("title", "author", "price");

    public static List<SortOrder> parse(String sort) {
        if (sort == null || sort.isBlank()) {
            return Collections.emptyList();
        }

        List<SortOrder> orders = new ArrayList<>();
        String[] parts = sort.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            boolean ascending = true;
            String field = trimmed;

            if (trimmed.startsWith("-")) {
                ascending = false;
                field = trimmed.substring(1);
            }

            if (ALLOWED_FIELDS.contains(field)) {
                orders.add(new SortOrder(field, ascending));
            }
        }

        return Collections.unmodifiableList(orders);
    }

    public record SortOrder(String field, boolean ascending) {}
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && mvn test -Dtest=SortParserTest
```
Expected: 全部 10 个测试 PASS。

- [ ] **Step 5: 提交**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && git add src/main/java/com/kleaves/demo/model/SortParser.java src/test/java/com/kleaves/demo/model/SortParserTest.java && git commit -m "feat: add SortParser for ?sort=-price,author parameter"
```

---

### Task 2: PageResponse record

**Files:**
- Create: `src/main/java/com/kleaves/demo/model/PageResponse.java`

- [ ] **Step 1: 创建 PageResponse record**

```java
package com.kleaves.demo.model;

import java.util.List;

/**
 * 分页响应通用包装
 *
 * @param <T>  数据项类型
 * @param data       当前页数据列表
 * @param total      数据总条数
 * @param page       当前页码（从 1 开始）
 * @param size       每页条数
 * @param totalPages 总页数
 */
public record PageResponse<T>(
        List<T> data,
        long total,
        int page,
        int size,
        int totalPages
) {}
```

- [ ] **Step 2: 编译验证**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && mvn compile
```
Expected: BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && git add src/main/java/com/kleaves/demo/model/PageResponse.java && git commit -m "feat: add PageResponse<T> record for paginated responses"
```

---

### Task 3: BookService 分页排序 — 测试先行

**Files:**
- Create: `src/test/java/com/kleaves/demo/service/BookServiceTest.java`
- Modify: `src/main/java/com/kleaves/demo/service/BookService.java`

- [ ] **Step 1: 编写 BookService 分页排序测试**

```java
package com.kleaves.demo.service;

import com.kleaves.demo.model.Book;
import com.kleaves.demo.model.PageResponse;
import com.kleaves.demo.model.SortParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookServiceTest {

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService();
    }

    // ===== 基本分页 =====

    @Test
    void findAll_withDefaultPagination_shouldReturnAllBooks() {
        var pr = new BookService.PageRequest(1, 20, List.of());
        PageResponse<Book> result = bookService.findAll(pr);

        assertThat(result.total()).isEqualTo(5);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.data()).hasSize(5);
    }

    @Test
    void findAll_page1Size2_shouldReturnFirst2Books() {
        var pr = new BookService.PageRequest(1, 2, List.of());
        PageResponse<Book> result = bookService.findAll(pr);

        assertThat(result.total()).isEqualTo(5);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.data()).hasSize(2);
    }

    @Test
    void findAll_page3Size2_shouldReturnLast1Book() {
        var pr = new BookService.PageRequest(3, 2, List.of());
        PageResponse<Book> result = bookService.findAll(pr);

        assertThat(result.total()).isEqualTo(5);
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void findAll_pageOutOfRange_shouldReturnEmptyList() {
        var pr = new BookService.PageRequest(99, 20, List.of());
        PageResponse<Book> result = bookService.findAll(pr);

        assertThat(result.total()).isEqualTo(5);
        assertThat(result.data()).isEmpty();
    }

    // ===== 边界值处理 =====

    @Test
    void findAll_pageZero_shouldTreatAsPage1() {
        var pr = new BookService.PageRequest(0, 2, List.of());
        PageResponse<Book> result = bookService.findAll(pr);

        assertThat(result.data()).hasSize(2);
    }

    @Test
    void findAll_sizeZero_shouldTreatAsDefault20() {
        var pr = new BookService.PageRequest(1, 0, List.of());
        PageResponse<Book> result = bookService.findAll(pr);

        // 只有 5 本书，所以全返回
        assertThat(result.data()).hasSize(5);
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void findAll_sizeExceeds100_shouldCapAt100() {
        var pr = new BookService.PageRequest(1, 200, List.of());
        PageResponse<Book> result = bookService.findAll(pr);

        assertThat(result.size()).isEqualTo(100);
    }

    // ===== 排序 =====

    @Test
    void findAll_sortedByPriceAsc_shouldReturnInPriceOrder() {
        var sorts = SortParser.parse("price");
        var pr = new BookService.PageRequest(1, 20, sorts);
        PageResponse<Book> result = bookService.findAll(pr);

        List<Double> prices = result.data().stream().map(Book::getPrice).toList();
        assertThat(prices).isSorted();
    }

    @Test
    void findAll_sortedByPriceDesc_shouldReturnInPriceDescOrder() {
        var sorts = SortParser.parse("-price");
        var pr = new BookService.PageRequest(1, 20, sorts);
        PageResponse<Book> result = bookService.findAll(pr);

        List<Double> prices = result.data().stream().map(Book::getPrice).toList();
        // 验证降序：后一个不大于前一个
        for (int i = 0; i < prices.size() - 1; i++) {
            assertThat(prices.get(i)).isGreaterThanOrEqualTo(prices.get(i + 1));
        }
    }

    @Test
    void findAll_sortedByAuthorThenPriceDesc_shouldApplyBothSorts() {
        // 先插入一本同作者不同价格的书来测试多列排序
        bookService.save(new Book(null, "测试书", "余华", "978-7-0000-0000-0", 10.0));

        var sorts = SortParser.parse("author,-price");
        var pr = new BookService.PageRequest(1, 20, sorts);
        PageResponse<Book> result = bookService.findAll(pr);

        // 验证数据按 author 升序为主, price 降序为辅
        List<Book> books = result.data();
        for (int i = 0; i < books.size() - 1; i++) {
            String author1 = books.get(i).getAuthor();
            String author2 = books.get(i + 1).getAuthor();
            assertThat(author1.compareTo(author2)).isLessThanOrEqualTo(0);
            if (author1.equals(author2)) {
                assertThat(books.get(i).getPrice())
                        .isGreaterThanOrEqualTo(books.get(i + 1).getPrice());
            }
        }
    }

    @Test
    void findAll_noSort_shouldPreserveOriginalOrder() {
        var pr = new BookService.PageRequest(1, 20, List.of());
        PageResponse<Book> result = bookService.findAll(pr);

        // 原始顺序：活着、三体、百年孤独、围城、红楼梦
        assertThat(result.data().get(0).getTitle()).isEqualTo("活着");
        assertThat(result.data().get(1).getTitle()).isEqualTo("三体");
        assertThat(result.data().get(2).getTitle()).isEqualTo("百年孤独");
        assertThat(result.data().get(3).getTitle()).isEqualTo("围城");
        assertThat(result.data().get(4).getTitle()).isEqualTo("红楼梦");
    }

    // ===== 搜索 + 分页 =====

    @Test
    void findByAuthor_withPagination_shouldReturnFilteredAndPaged() {
        // 先插入更多余华的书
        bookService.save(new Book(null, "测试1", "余华", "978-7-0000-0000-1", 10.0));
        bookService.save(new Book(null, "测试2", "余华", "978-7-0000-0000-2", 20.0));

        var pr = new BookService.PageRequest(1, 2, List.of());
        PageResponse<Book> result = bookService.findByAuthor("余华", pr);

        assertThat(result.total()).isEqualTo(2); // 活着 + 新增2本，但"活着"是第一本
        // 实际上余华只有原始"活着" + 两本新书 = 3本
        assertThat(result.data()).hasSize(2);
    }

    @Test
    void findByAuthor_emptyAuthor_shouldReturnAll() {
        var pr = new BookService.PageRequest(1, 20, List.of());
        PageResponse<Book> result = bookService.findByAuthor("", pr);

        // 空的 author 参数应该返回全部
        assertThat(result.total()).isGreaterThanOrEqualTo(5);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && mvn test -Dtest=BookServiceTest
```
Expected: 编译失败（`BookService.PageRequest` 和新的 `findAll`/`findByAuthor` 签名不存在）。

- [ ] **Step 3: 修改 BookService — 添加 PageRequest 和新的方法签名**

用以下内容替换 `BookService.java` 中的 `findAll()` 和 `findByAuthor()` 方法，并在类顶部添加 `PageRequest` record 和 `VALID_SORT_FIELDS` 常量：

首先，在 `BookService` 类的 `import` 区域后、类定义内部，添加 `PageRequest` record 和辅助常量：

在 `BookService` 类体内（`private final AtomicLong idGenerator = new AtomicLong(1);` 之后）添加：

```java
    /**
     * 分页请求参数
     */
    public record PageRequest(int page, int size, List<SortParser.SortOrder> sorts) {}
```

然后，将原来的 `findAll()` 方法替换为：

```java
    /**
     * 获取所有图书（带分页和排序）
     *
     * @param pageRequest 分页排序参数
     * @return 分页响应
     */
    public PageResponse<Book> findAll(PageRequest pageRequest) {
        return paginate(new ArrayList<>(bookStore.values()), pageRequest);
    }
```

将原来的 `findByAuthor(String author)` 方法替换为：

```java
    /**
     * 按作者搜索（带分页和排序）
     *
     * @param author      作者名（模糊匹配），null 或空白返回全部
     * @param pageRequest 分页排序参数
     * @return 分页响应
     */
    public PageResponse<Book> findByAuthor(String author, PageRequest pageRequest) {
        List<Book> filtered;
        if (author == null || author.isBlank()) {
            filtered = new ArrayList<>(bookStore.values());
        } else {
            filtered = bookStore.values().stream()
                    .filter(book -> book.getAuthor().contains(author))
                    .collect(Collectors.toList());
        }
        return paginate(filtered, pageRequest);
    }
```

最后，添加私有的 `paginate` 方法：

```java
    /**
     * 核心分页排序逻辑
     *
     * 流程：排序 → 计算总数 → 截取分页 → 构造 PageResponse
     */
    private PageResponse<Book> paginate(List<Book> books, PageRequest pageRequest) {
        // 1. 排序
        List<SortParser.SortOrder> sorts = pageRequest.sorts();
        if (sorts != null && !sorts.isEmpty()) {
            Comparator<Book> comparator = null;
            for (SortParser.SortOrder sort : sorts) {
                Comparator<Book> fieldComp = switch (sort.field()) {
                    case "title"  -> Comparator.comparing(Book::getTitle);
                    case "author" -> Comparator.comparing(Book::getAuthor);
                    case "price"  -> Comparator.comparing(Book::getPrice);
                    default       -> null;
                };
                if (fieldComp == null) {
                    continue;
                }
                if (!sort.ascending()) {
                    fieldComp = fieldComp.reversed();
                }
                comparator = (comparator == null) ? fieldComp : comparator.thenComparing(fieldComp);
            }
            if (comparator != null) {
                books.sort(comparator);
            }
        }

        // 2. 边界修正
        int safePage = Math.max(1, pageRequest.page());
        int safeSize = Math.max(1, Math.min(100, pageRequest.size()));

        // 3. 分页截取
        long total = books.size();
        int totalPages = (total == 0) ? 1 : (int) Math.ceil((double) total / safeSize);
        int fromIndex = (safePage - 1) * safeSize;

        List<Book> data;
        if (fromIndex >= total) {
            data = List.of();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, (int) total);
            data = books.subList(fromIndex, toIndex);
        }

        return new PageResponse<>(data, total, safePage, safeSize, totalPages);
    }
```

需要在 `BookService.java` 的 import 区域添加：

```java
import com.kleaves.demo.model.PageResponse;
import com.kleaves.demo.model.SortParser;
import java.util.Comparator;
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && mvn test -Dtest=BookServiceTest
```
Expected: 全部测试 PASS。

- [ ] **Step 5: 提交**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && git add src/main/java/com/kleaves/demo/service/BookService.java src/test/java/com/kleaves/demo/service/BookServiceTest.java && git commit -m "feat: add pagination and sorting to BookService"
```

---

### Task 4: BookController 合并端点

**Files:**
- Modify: `src/main/java/com/kleaves/demo/controller/BookController.java`

- [ ] **Step 1: 修改 BookController**

首先在 `BookController` 类中添加 `@Value` 注入字段（放在 `private Validator validator;` 之后）：

```java
    @Value("${book.default-page-size:20}")
    private int defaultPageSize;
```

需要添加 import：

```java
import org.springframework.beans.factory.annotation.Value;
```

然后将原来的两个方法合并：

删除这两个方法：
- `public ResponseEntity<List<Book>> listAll()`
- `public ResponseEntity<List<Book>> searchByAuthor(@RequestParam String author)`

替换为一个方法：

```java
    /**
     * GET /api/books
     * 获取图书列表（支持分页、排序和按作者搜索）
     *
     * 参数（全部可选）:
     *   author — 按作者搜索（模糊匹配）
     *   page   — 页码，从 1 开始，默认 1
     *   size   — 每页条数，默认 20（来自配置文件），上限 100
     *   sort   — 排序，逗号分隔，- 前缀表示降序（如 -price,author）
     *
     * 测试:
     *   curl "http://localhost:8080/api/books"
     *   curl "http://localhost:8080/api/books?page=1&size=3"
     *   curl "http://localhost:8080/api/books?sort=-price"
     *   curl "http://localhost:8080/api/books?author=余华&sort=-price&page=1&size=5"
     */
    @GetMapping
    public ResponseEntity<PageResponse<Book>> listAll(
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {

        int actualSize = (size != null) ? size : defaultPageSize;
        BookService.PageRequest pr = new BookService.PageRequest(
                page, actualSize, SortParser.parse(sort));

        PageResponse<Book> result = (author != null && !author.isBlank())
                ? bookService.findByAuthor(author, pr)
                : bookService.findAll(pr);

        return ResponseEntity.ok(result);
    }
```

需要添加 import：

```java
import com.kleaves.demo.model.PageResponse;
import com.kleaves.demo.model.SortParser;
```

- [ ] **Step 2: 编译验证**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && mvn compile
```
Expected: BUILD SUCCESS。

- [ ] **Step 3: 启动应用并手动验证**

在一个终端启动应用：

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && mvn spring-boot:run
```

在另一个终端测试以下场景：

```bash
# 1. 默认分页（全部5本书在一页）
curl -s http://localhost:8080/api/books | python -m json.tool
# Expected: {"data":[...5本书...],"total":5,"page":1,"size":20,"totalPages":1}

# 2. 分页：每页2条
curl -s "http://localhost:8080/api/books?page=1&size=2" | python -m json.tool
# Expected: {"data":[...2本书...],"total":5,"page":1,"size":2,"totalPages":3}

# 3. 按价格降序
curl -s "http://localhost:8080/api/books?sort=-price" | python -m json.tool
# Expected: 第一本应是红楼梦(59.70)，最后一本应是围城(28.00)

# 4. 按作者搜索
curl -s "http://localhost:8080/api/books?author=余华" | python -m json.tool
# Expected: {"data":[{"title":"活着",...}],"total":1,...}

# 5. 分页超出范围
curl -s "http://localhost:8080/api/books?page=99" | python -m json.tool
# Expected: {"data":[],"total":5,"page":99,"size":20,"totalPages":1}

# 6. 多列排序
curl -s "http://localhost:8080/api/books?sort=author,-price" | python -m json.tool
# Expected: 按作者升序，同作者按价格降序
```

- [ ] **Step 4: 提交**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && git add src/main/java/com/kleaves/demo/controller/BookController.java && git commit -m "feat: merge list endpoints, add pagination and sorting params to BookController"
```

---

### Task 5: 运行全部测试确认回归

- [ ] **Step 1: 运行全部测试**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && mvn test
```
Expected: 全部测试 PASS（SortParserTest + BookServiceTest 共 20+ 个测试）。

- [ ] **Step 2: 最终提交（如有未提交内容）**

```bash
cd d:/Work/kleaves/Harness/workspaces/01-book-api && git status
```

---

## 实现顺序依赖

```
Task 1 (SortParser) ──┐
                       ├──> Task 3 (BookService) ──> Task 4 (BookController) ──> Task 5 (全部测试)
Task 2 (PageResponse) ─┘
```

Task 1 和 Task 2 无依赖，可并行。Task 3 依赖 1+2。Task 4 依赖 3。
