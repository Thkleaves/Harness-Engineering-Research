# GET /api/books 分页与排序 — 设计规格

## 背景

Harness Engineering 调研 demo，Spring Boot 3.4 + Java 21，内存存储（ConcurrentHashMap），5 本示例书。当前 `GET /api/books` 返回全部数据，需增加分页和排序能力。

面向听众：实习生分享（4天，10+人），要求简单清晰、易于讲解和演示。

## 参数设计

| 参数 | 示例 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | `?page=2` | `1` | 页码，从 1 开始 |
| `size` | `?size=10` | `20`（来自 `book.default-page-size`） | 每页条数，上限 100 |
| `sort` | `?sort=-price,author` | 无（保持原始顺序） | 逗号分隔字段名，`-` 前缀表示降序 |

排序支持字段：`title`、`author`、`price`。非法字段名静默忽略。`-` 前缀表示降序，无前缀默认升序。

## 响应格式

```json
{
  "data": [ ... ],
  "total": 5,
  "page": 1,
  "size": 3,
  "totalPages": 2
}
```

## 组件设计

### 新增文件

#### `model/PageResponse.java` — 分页响应包装

```java
public record PageResponse<T>(
    List<T> data,
    long total,
    int page,
    int size,
    int totalPages
) {}
```

#### `model/SortParser.java` — 排序参数解析

```java
public class SortParser {
    public static List<SortOrder> parse(String sort) { ... }
    public record SortOrder(String field, boolean ascending) {}
}
```

- 静态工具类，无状态
- 输入 `"-price,author"` 解析为 `[{price, false}, {author, true}]`
- 只接受 `title`/`author`/`price`，非法字段忽略

### 修改文件

#### `service/BookService.java`

- `findAll()` → `findAll(PageRequest)` 返回 `PageResponse<Book>`
- `findByAuthor(String)` → `findByAuthor(String, PageRequest)` 返回 `PageResponse<Book>`
- 内部流程：全量获取 → 排序 → 计算 `total` → 截取分页 → 构造 `PageResponse`

`PageRequest` 定义为 Service 内的 record：

```java
public record PageRequest(int page, int size, List<SortParser.SortOrder> sorts) {}
```

#### `controller/BookController.java`

- `GET /api/books` 和 `GET /api/books?author=xxx` 合并为一个方法
- `author` 参数 `required=false`
- `page` 默认 `1`，`size` 默认注入 `@Value("${book.default-page-size}")`
- 构造 `PageRequest` → 调用 `BookService` → 返回 `ResponseEntity<PageResponse<Book>>`

## 边界处理

| 场景 | 行为 |
|------|------|
| `page < 1` | 取 `1` |
| `size < 1` | 取默认值 `20` |
| `size > 100` | 截断为 `100` |
| 请求页码超出实际总页数 | 返回空 `data` 列表，不报错 |
| 无效排序字段 | 静默忽略该字段，其余排序继续生效 |
| `sort` 为空或 null | 不排序，保持原始顺序 |

## 方案选择

- **方案二（Service 层参数化）**：Controller 解析 HTTP 参数构造 `PageRequest`，传入 Service 执行排序分页。兼顾自定义 `?sort=-price` 格式的灵活性和关注点分离。
- 排序风格 **JSON:API 惯例**（`-` 前缀降序）：一个参数搞定字段和方向，天然支持多列排序，讲解直观。
- 分页风格 **偏移分页**（`?page=1&size=20`）：传统易懂，与 `application.yml` 已有 `default-page-size` 配置呼应，返回总页数方便演示。
