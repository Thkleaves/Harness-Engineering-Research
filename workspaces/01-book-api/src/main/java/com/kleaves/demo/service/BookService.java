package com.kleaves.demo.service;

import com.kleaves.demo.model.Book;
import com.kleaves.demo.model.PageResponse;
import com.kleaves.demo.model.SortParser;
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
     * 分页请求参数
     */
    public record PageRequest(int page, int size, List<SortParser.SortOrder> sorts) {}

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
     * 获取所有图书（带分页和排序）
     *
     * @param pageRequest 分页排序参数
     * @return 分页响应
     */
    public PageResponse<Book> findAll(PageRequest pageRequest) {
        return paginate(new ArrayList<>(bookStore.values()), pageRequest);
    }

    /**
     * 按 ID 查找
     * 返回 Optional：调用方需要处理"找不到"的情况
     */
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(bookStore.get(id));
    }

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
        int safeSize = pageRequest.size() < 1 ? 20 : Math.min(100, pageRequest.size());

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
}
