package com.kleaves.demo.service;

import com.kleaves.demo.model.Order;
import com.kleaves.demo.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⚠️ FLAKY TEST — 跨 UTC 日期边界时（UTC 0-2点）必挂
 * 问题：测试创建"今天"的订单 → 调用 todaySales() → LocalDate.now() 可能已是"明天"
 */
@SpringBootTest
public class SalesReportServiceTest {

    @Autowired
    private SalesReportService salesReportService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    public void todaySales_shouldReturnPositiveAmount_whenOrdersExist() {
        // 创建一个"今天"的订单
        orderRepository.save(new Order(
                new BigDecimal("100.00"),
                LocalDateTime.now() // 用当前时间模拟"今天"
        ));

        BigDecimal sales = salesReportService.todaySales();

        // ⚠️ 这个断言在 UTC 23:55 创建订单 + UTC 00:05 运行时失败
        // 因为 LocalDate.now() 已经是新的一天，但订单是"昨天"的
        assertTrue(sales.compareTo(BigDecimal.ZERO) > 0,
                "今天的销售额应该大于0，但实际是: " + sales);
    }
}
