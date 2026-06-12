package com.kleaves.demo.model;

import com.kleaves.demo.validation.ISBN;
import com.kleaves.demo.validation.PriceFormat;
import jakarta.validation.constraints.*;

/**
 * Book 实体类 — 表示一本书
 *
 * 类比 Day1 的 Student 类：
 *   Student 有 id/name/age/major
 *   Book    有 id/title/author/isbn/price
 *
 * Spring 内置的 Jackson 库会自动把 Book 对象 ↔ JSON 互相转换：
 *   对象 → JSON（序列化）: 响应时自动转
 *   JSON  → 对象（反序列化）: @RequestBody 时自动转
 *
 * 注意：Jackson 通过 getter 方法序列化，所以每个字段都必须有 getter
 */
public class Book {

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
    @PriceFormat
    private Double price;

    // ===== 构造方法 =====

    public Book() {
    }

    public Book(Long id, String title, String author, String isbn, Double price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
    }

    // ===== Getter 和 Setter（Jackson 需要 getter 来序列化为 JSON）=====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", isbn='" + isbn + '\'' +
                ", price=" + price +
                '}';
    }
}
