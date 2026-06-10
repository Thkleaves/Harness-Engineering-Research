package com.kleaves.demo.controller;

import com.kleaves.demo.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * ⚠️ FAT CONTROLLER — 200+ 行业务逻辑混在控制器中
 * 需要重构：抽取 UserService + UserRepository，Controller 只做路由
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    // ── 内嵌数据存储（应该在 Repository 层）──
    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserController() {
        // 种子数据
        User u1 = new User(idGenerator.getAndIncrement(), "Alice Wang", "alice@example.com", 28, "Engineering");
        User u2 = new User(idGenerator.getAndIncrement(), "Bob Li", "bob@example.com", 35, "Marketing");
        User u3 = new User(idGenerator.getAndIncrement(), "Charlie Zhang", "charlie@example.com", 22, "Engineering");
        User u4 = new User(idGenerator.getAndIncrement(), "Diana Chen", "diana@example.com", 31, "Finance");
        User u5 = new User(idGenerator.getAndIncrement(), "Eve Liu", "eve@example.com", 27, "Engineering");
        userStore.put(u1.getId(), u1);
        userStore.put(u2.getId(), u2);
        userStore.put(u3.getId(), u3);
        userStore.put(u4.getId(), u4);
        userStore.put(u5.getId(), u5);
    }

    // ── 业务校验逻辑（应该在 Service 层）──
    private void validateUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("名字不能为空");
        }
        if (user.getName().length() < 2 || user.getName().length() > 50) {
            throw new IllegalArgumentException("名字长度必须在 2-50 之间");
        }
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        if (user.getAge() != null && (user.getAge() < 18 || user.getAge() > 120)) {
            throw new IllegalArgumentException("年龄必须在 18-120 之间");
        }
        if (user.getDepartment() == null || user.getDepartment().isBlank()) {
            throw new IllegalArgumentException("部门不能为空");
        }
        // 检查邮箱唯一性（业务规则）
        boolean emailExists = userStore.values().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(user.getEmail()) && !u.getId().equals(user.getId()));
        if (emailExists) {
            throw new IllegalArgumentException("邮箱已被注册: " + user.getEmail());
        }
    }

    // ── 搜索逻辑（应该在 Service 层）──
    private List<User> searchUsers(String name, String department, Integer minAge, Integer maxAge) {
        return userStore.values().stream()
                .filter(u -> name == null || u.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(u -> department == null || u.getDepartment().equalsIgnoreCase(department))
                .filter(u -> minAge == null || u.getAge() >= minAge)
                .filter(u -> maxAge == null || u.getAge() <= maxAge)
                .collect(Collectors.toList());
    }

    // ── 统计逻辑（应该在 Service 层）──
    private Map<String, Long> getDepartmentStats() {
        return userStore.values().stream()
                .collect(Collectors.groupingBy(User::getDepartment, Collectors.counting()));
    }

    // ═══════════════════════════════════════
    // API 端点
    // ═══════════════════════════════════════

    @GetMapping
    public ResponseEntity<List<User>> listAll() {
        return ResponseEntity.ok(new ArrayList<>(userStore.values()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable @Positive Long id) {
        User user = userStore.get(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    /** 多条件搜索 */
    @GetMapping("/search")
    public ResponseEntity<List<User>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) @Min(0) @Max(150) Integer minAge,
            @RequestParam(required = false) @Min(0) @Max(150) Integer maxAge) {
        return ResponseEntity.ok(searchUsers(name, department, minAge, maxAge));
    }

    /** 批量创建 */
    @PostMapping("/batch")
    public ResponseEntity<List<User>> batchCreate(@RequestBody @Valid List<User> users) {
        List<User> saved = new ArrayList<>();
        for (User user : users) {
            validateUser(user);
            user.setId(idGenerator.getAndIncrement());
            userStore.put(user.getId(), user);
            saved.add(user);
        }
        return ResponseEntity.ok(saved);
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        validateUser(user);
        user.setId(idGenerator.getAndIncrement());
        userStore.put(user.getId(), user);
        URI location = URI.create("/api/users/" + user.getId());
        return ResponseEntity.created(location).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable @Positive Long id, @RequestBody User newData) {
        User existing = userStore.get(id);
        if (existing == null) return ResponseEntity.notFound().build();
        // 部分更新逻辑（应该在 Service 层）
        if (newData.getName() != null) existing.setName(newData.getName());
        if (newData.getEmail() != null) existing.setEmail(newData.getEmail());
        if (newData.getAge() != null) existing.setAge(newData.getAge());
        if (newData.getDepartment() != null) existing.setDepartment(newData.getDepartment());
        validateUser(existing);
        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        return userStore.remove(id) != null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** 部门统计 */
    @GetMapping("/stats/departments")
    public ResponseEntity<Map<String, Long>> departmentStats() {
        return ResponseEntity.ok(getDepartmentStats());
    }

    /** 全局异常处理（也应该单独抽出） */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
