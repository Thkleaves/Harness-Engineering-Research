package com.kleaves.demo.service;

import com.kleaves.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserService() {
        addSample("alice", "alice@example.com");
        addSample("bob", "bob@example.com");
        addSample("charlie", "charlie@example.com");
    }

    private void addSample(String username, String email) {
        long id = idGenerator.getAndIncrement();
        userStore.put(id, new User(id, username, email));
    }

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
