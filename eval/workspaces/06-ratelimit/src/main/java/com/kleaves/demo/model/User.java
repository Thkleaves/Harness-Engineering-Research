package com.kleaves.demo.model;

/**
 * User 实体 — 公共 API，无限流
 *
 * ⚠️ 当前状态：GET /api/users 对所有调用者开放，无频率限制
 * 任务要求：实现 Token Bucket 限流，每用户每分钟 60 次，超额返回 429
 */
public class User {

    private Long id;
    private String username;
    private String email;

    public User() {}

    public User(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}
