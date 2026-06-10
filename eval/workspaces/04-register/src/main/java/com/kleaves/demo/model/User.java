package com.kleaves.demo.model;

/**
 * User 实体
 *
 * ⚠️ 当前状态：只有 ACTIVE 状态，无注册流程、无邮箱验证、无 Token
 * 任务要求：添加注册→UNVERIFIED→邮箱验证→ACTIVE 流程
 */
public class User {

    private Long id;
    private String username;
    private String email;
    private String password;
    private String status;  // 当前只有 "ACTIVE"，需支持 "UNVERIFIED"

    public User() {}

    public User(Long id, String username, String email, String password, String status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "', status=" + status + "}";
    }
}
