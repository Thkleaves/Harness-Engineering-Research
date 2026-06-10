package com.kleaves.demo.service;

import com.kleaves.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ⚠️ 当前状态：无任何限流机制，所有调用无频率限制
 * 任务要求：实现 Token Bucket 算法，每用户每分钟 60 次，超额返回 429
 */
@Service
public class UserService {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserService() {
        for (int i = 1; i <= 10; i++) {
            long id = idGenerator.getAndIncrement();
            userStore.put(id, new User(id, "user" + i, "user" + i + "@example.com"));
        }
    }

    public List<User> findAll() {
        return new ArrayList<>(userStore.values());
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userStore.get(id));
    }
}
