package com.kleaves.demo.service;

import com.kleaves.demo.model.Order;
import com.kleaves.demo.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售报告服务
 *
 * ⚠️ BUG: todaySalesReport() 使用 LocalDate.now() 获取"今天"
 * - CI 服务器时区 = UTC
 * - 本地开发时区 = Asia/Shanghai (UTC+8)
 * - 当 UTC 时间是 2026-06-10 00:30 时：
 *   CI 认为"今天"是 2026-06-10，但上海时间已经是 2026-06-10 08:30
 * - 如果数据里有 2026-06-10 00:00-08:00 之间的订单，CI 和本地的"今天销售额"会不同
 * - 更糟糕的是 UTC 0-2 点可能还在"昨天"，测试断言会随机失败
 */
@Service
public class SalesReportService {

    private final OrderRepository orderRepository;

    public SalesReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 计算今天的销售总额
     * ⚠️ LocalDate.now() 依赖系统默认时区 → CI(UTC) vs 本地(Asia/Shanghai) 不一致
     */
    public BigDecimal todaySales() {
        LocalDate today = LocalDate.now(); // ⚠️ 时区相关！
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<Order> todayOrders = orderRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        return todayOrders.stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
