package com.kleaves.demo.service;

import com.kleaves.demo.model.Order;
import com.kleaves.demo.model.User;
import com.kleaves.demo.repository.OrderRepository;
import com.kleaves.demo.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void seedData() {
        User u1 = userRepository.save(new User("Alice", "alice@example.com"));
        User u2 = userRepository.save(new User("Bob", "bob@example.com"));
        User u3 = userRepository.save(new User("Charlie", "charlie@example.com"));

        for (int i = 0; i < 30; i++) {
            orderRepository.save(new Order("Product-" + i,
                    new BigDecimal("99.99"),
                    LocalDateTime.now().minusDays(i),
                    i % 3 == 0 ? u1 : (i % 3 == 1 ? u2 : u3)));
        }
    }

    /**
     * ⚠️ N+1 问题：findAll() 只查 Order 表 1 次，
     * 但遍历每个 Order 访问 getUser() 时触发 N 次额外查询
     */
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}
