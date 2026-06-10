package com.kleaves.demo.service;

import com.kleaves.demo.model.User;
import com.kleaves.demo.model.UserCreateRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserService() {
        addSample("alice", "alice@example.com", "Pass1234", 25);
        addSample("bob", "bob@example.com", "Pass5678", 30);
        addSample("charlie", "charlie@example.com", "Pass9012", 22);
    }

    private void addSample(String username, String email, String password, Integer age) {
        long id = idGenerator.getAndIncrement();
        userStore.put(id, new User(id, username, email, password, age));
    }

    public List<User> findAll() {
        return new ArrayList<>(userStore.values());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userStore.get(id));
    }

    /**
     * 从 UserCreateRequest 创建用户 — 当前无任何校验
     * 任务要求 Agent：在 UserCreateRequest 上加注解 + Controller 加 @Valid
     */
    public User create(UserCreateRequest request) {
        long id = idGenerator.getAndIncrement();
        User user = new User(id, request.getUsername(), request.getEmail(),
                             request.getPassword(), request.getAge());
        userStore.put(id, user);
        return user;
    }

    public boolean deleteById(Long id) {
        return userStore.remove(id) != null;
    }
}
