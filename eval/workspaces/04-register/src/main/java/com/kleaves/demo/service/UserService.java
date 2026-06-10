package com.kleaves.demo.service;

import com.kleaves.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ⚠️ 当前状态：基本 CRUD，无注册流程、无邮箱验证、无 Token、无登录
 * 任务要求：POST /api/auth/register → UNVERIFIED → 验证 Token → ACTIVE
 */
@Service
public class UserService {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserService() {
        addSample("alice", "alice@example.com", "pass1");
        addSample("bob", "bob@example.com", "pass2");
    }

    private void addSample(String username, String email, String password) {
        long id = idGenerator.getAndIncrement();
        userStore.put(id, new User(id, username, email, password, "ACTIVE"));
    }

    public List<User> findAll() {
        return new ArrayList<>(userStore.values());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userStore.get(id));
    }

    public Optional<User> findByEmail(String email) {
        return userStore.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public User create(User user) {
        long id = idGenerator.getAndIncrement();
        user.setId(id);
        user.setStatus("ACTIVE");  // 直接激活，无验证流程
        userStore.put(id, user);
        return user;
    }

    public boolean deleteById(Long id) {
        return userStore.remove(id) != null;
    }
}
