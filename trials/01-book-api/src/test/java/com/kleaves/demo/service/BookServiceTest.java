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
        // 先插入更多余华的书（原始已有"活着"，再插入2本 = 共3本余华的书）
        bookService.save(new Book(null, "测试1", "余华", "978-7-0000-0000-1", 10.0));
        bookService.save(new Book(null, "测试2", "余华", "978-7-0000-0000-2", 20.0));

        var pr = new BookService.PageRequest(1, 2, List.of());
        PageResponse<Book> result = bookService.findByAuthor("余华", pr);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.data()).hasSize(2);
    }

    @Test
    void findByAuthor_emptyAuthor_shouldReturnAll() {
        var pr = new BookService.PageRequest(1, 20, List.of());
        PageResponse<Book> result = bookService.findByAuthor("", pr);

        // 空的 author 参数应该返回全部（5 本初始数据）
        assertThat(result.total()).isEqualTo(5);
    }
}
