package com.kleaves.demo.service;

import com.kleaves.demo.model.Book;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 业务逻辑层 — Book 服务
 *
 * @Service 标记此类为 Spring Bean，由容器管理
 * 职责：业务逻辑（CRUD、查询、校验）
 *
 * 当前版本：内存存储（ConcurrentHashMap）
 * Day 3 会替换为数据库 + JPA Repository
 */
@Service
public class BookService {

    // 线程安全的 Map（生产环境用数据库，这里只是模拟）
    private final Map<Long, Book> bookStore = new ConcurrentHashMap<>();
    // 自增 ID 生成器
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 构造时预置几条示例数据（方便测试）
     */
    public BookService() {
        addSampleBook("活着", "余华", "978-7-5302-2155-6", 29.90);
        addSampleBook("三体", "刘慈欣", "978-7-5366-9293-0", 39.90);
        addSampleBook("百年孤独", "加西亚·马尔克斯", "978-7-5442-5399-4", 55.00);
        addSampleBook("围城", "钱钟书", "978-7-02-007005-3", 28.00);
        addSampleBook("红楼梦", "曹雪芹", "978-7-02-000220-7", 59.70);
    }

    private void addSampleBook(String title, String author, String isbn, Double price) {
        long id = idGenerator.getAndIncrement();
        bookStore.put(id, new Book(id, title, author, isbn, price));
    }

    // ===== CRUD 方法 =====

    /**
     * 获取所有图书（返回列表）
     * 类比 Day1 学生管理系统的 listAllStudents()
     */
    public List<Book> findAll() {
        return new ArrayList<>(bookStore.values());
    }

    /**
     * 按 ID 查找
     * 返回 Optional：调用方需要处理"找不到"的情况
     */
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(bookStore.get(id));
    }

    /**
     * 按作者搜索（模糊匹配）
     * 用 Stream API 过滤，和 Day1 中学生管理系统 searchByName 一样
     */
    public List<Book> findByAuthor(String author) {
        if (author == null || author.isBlank()) {
            return findAll();
        }
        return bookStore.values().stream()
                .filter(book -> book.getAuthor().contains(author))
                .collect(Collectors.toList());
    }

    /**
     * 新增图书，返回保存后的对象（带 ID）
     */
    public Book save(Book book) {
        long id = idGenerator.getAndIncrement();
        book.setId(id);
        bookStore.put(id, book);
        return book;
    }

    /**
     * 更新图书（如果存在返回更新后的对象，否则返回空）
     */
    public Optional<Book> update(Long id, Book newData) {
        return Optional.ofNullable(bookStore.get(id))
                .map(existing -> {
                    // 只更新非空字段
                    if (newData.getTitle() != null) existing.setTitle(newData.getTitle());
                    if (newData.getAuthor() != null) existing.setAuthor(newData.getAuthor());
                    if (newData.getIsbn() != null) existing.setIsbn(newData.getIsbn());
                    if (newData.getPrice() != null) existing.setPrice(newData.getPrice());
                    return existing;
                });
    }

    /**
     * 按 ID 删除
     * 返回 true 表示删除成功，false 表示 ID 不存在
     */
    public boolean deleteById(Long id) {
        return bookStore.remove(id) != null;
    }
}
