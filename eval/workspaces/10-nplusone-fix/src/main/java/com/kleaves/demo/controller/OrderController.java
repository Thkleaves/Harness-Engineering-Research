package com.kleaves.demo.controller;

import com.kleaves.demo.model.Order;
import com.kleaves.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /** ⚠️ 触发 N+1：返回每个 Order 时序列化 user.getName() */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> listAll() {
        List<Order> orders = orderService.findAll();
        List<OrderResponse> response = orders.stream()
                .map(o -> new OrderResponse(o)) // 每次 new 都触发 o.getUser() → N 次 SQL
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /** DTO 用于 JSON 响应 — 访问 user 触发 N+1 */
    public static class OrderResponse {
        public Long id;
        public String product;
        public String userName; // 第一次访问 → N+1 触发点

        public OrderResponse(Order o) {
            this.id = o.getId();
            this.product = o.getProduct();
            this.userName = o.getUser().getName(); // ⚠️ 每个 Order 一次 SQL
        }
    }
}
