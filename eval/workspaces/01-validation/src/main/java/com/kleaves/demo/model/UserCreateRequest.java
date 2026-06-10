package com.kleaves.demo.model;

/**
 * 用户创建请求 DTO
 *
 * ⚠️ 当前状态：没有任何 Jakarta Validation 注解
 * 任务要求 Agent 为以下字段添加校验：
 *   - username: @NotBlank, @Size(min=3, max=50), 字母数字
 *   - email:    @Email
 *   - password: @NotBlank, @Size(min=8), 含大小写+数字
 *   - age:      @Min(18) @Max(120)
 */
public class UserCreateRequest {

    private String username;
    private String email;
    private String password;
    private Integer age;

    public UserCreateRequest() {}

    public UserCreateRequest(String username, String email, String password, Integer age) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.age = age;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
