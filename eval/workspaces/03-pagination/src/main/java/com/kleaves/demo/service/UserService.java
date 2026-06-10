package com.kleaves.demo.service;

import com.kleaves.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ⚠️ 当前状态：findAll() 返回全部用户，无分页无排序
 * 任务要求：添加 page/size/sort 参数 + PageResponse + 排序白名单
 */
@Service
public class UserService {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserService() {
        addSample("alice", "alice@example.com", "pass1", 25);
        addSample("bob", "bob@example.com", "pass2", 30);
        addSample("charlie", "charlie@example.com", "pass3", 22);
        addSample("diana", "diana@example.com", "pass4", 28);
        addSample("eve", "eve@example.com", "pass5", 35);
        addSample("frank", "frank@example.com", "pass6", 40);
        addSample("grace", "grace@example.com", "pass7", 27);
        addSample("henry", "henry@example.com", "pass8", 33);
    }

    private void addSample(String username, String email, String password, Integer age) {
        long id = idGenerator.getAndIncrement();
        userStore.put(id, new User(id, username, email, password, age));
    }

    /** 返回全部用户 — 无分页 */
    public List<User> findAll() {
        return new ArrayList<>(userStore.values());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userStore.get(id));
    }

    public User create(User user) {
        long id = idGenerator.getAndIncrement();
        user.setId(id);
        userStore.put(id, user);
        return user;
    }

    public boolean deleteById(Long id) {
        return userStore.remove(id) != null;
    }
}
