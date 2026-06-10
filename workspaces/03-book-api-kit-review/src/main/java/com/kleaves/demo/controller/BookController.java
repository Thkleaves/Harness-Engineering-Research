package com.kleaves.demo.controller;

import com.kleaves.demo.model.Book;
import com.kleaves.demo.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * REST 控制器 — 处理 HTTP 请求
 *
 * @RestController = @Controller + @ResponseBody
 *   所有方法的返回值自动序列化为 JSON 写入 HTTP 响应体
 *
 * @RequestMapping("/api/books")
 *   类级别路径前缀，所有方法路径都会拼接在这个前缀后面
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    /**
     * @Autowired：字段注入
     * Spring 会自动把容器中的 BookService Bean 注入到这里
     * 不需要写 bookService = new BookService()
     */
    @Autowired
    private BookService bookService;

    @Autowired
    private Validator validator;

    // ===== REST API 接口 =====

    /**
     * GET /api/books
     * 获取全部图书列表
     *
     * 测试: curl http://localhost:8080/api/books
     */
    @GetMapping
    public ResponseEntity<List<Book>> listAll() {
        List<Book> books = bookService.findAll();
        return ResponseEntity.ok(books);
    }

    /**
     * GET /api/books/{id}
     * 按 ID 获取单本图书
     *
     * @PathVariable: 从 URL 路径中提取 {id} 的值
     *
     * 测试: curl http://localhost:8080/api/books/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getById(@PathVariable Long id) {
        return bookService.findById(id)
                .map(ResponseEntity::ok)                    // 找到了 → 200 OK
                .orElse(ResponseEntity.notFound().build());  // 没找到 → 404 Not Found
    }

    /**
     * GET /api/books?author=余华
     * 按作者搜索
     *
     * @RequestParam: 从 URL 查询字符串中提取参数
     *   required = false 表示这个参数可选
     *
     * 测试: curl "http://localhost:8080/api/books?author=余华"
     */
    @GetMapping(params = "author")
    public ResponseEntity<List<Book>> searchByAuthor(@RequestParam String author) {
        List<Book> books = bookService.findByAuthor(author);
        return ResponseEntity.ok(books);
    }

    /**
     * POST /api/books
     * 新增一本图书
     *
     * @RequestBody: Spring 自动把请求体的 JSON → Book 对象（Jackson 库在干活）
     *
     * 返回 201 Created + 新创建的图书（带 ID）+ Location 响应头
     *
     * 测试: curl -X POST http://localhost:8080/api/books \
     *        -H "Content-Type: application/json" \
     *        -d '{"title":"朝花夕拾","author":"鲁迅","isbn":"978-7-02-000220-8","price":19.9}'
     */
    @PostMapping
    public ResponseEntity<Book> create(@Valid @RequestBody Book book) {
        Book saved = bookService.save(book);
        URI location = URI.create("/api/books/" + saved.getId());
        return ResponseEntity.created(location).body(saved);
    }

    /**
     * PUT /api/books/{id}
     * 更新图书信息
     *
     * 只更新请求体中传入的非空字段（部分更新）
     *
     * 测试: curl -X PUT http://localhost:8080/api/books/1 \
     *        -H "Content-Type: application/json" \
     *        -d '{"price":35.0}'
     */
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

    /**
     * DELETE /api/books/{id}
     * 删除图书
     *
     * 成功返回 204 No Content（响应体为空）
     * ID 不存在返回 404 Not Found
     *
     * 测试: curl -X DELETE http://localhost:8080/api/books/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (bookService.deleteById(id)) {
            return ResponseEntity.noContent().build();  // 204
        }
        return ResponseEntity.notFound().build();       // 404
    }
}
