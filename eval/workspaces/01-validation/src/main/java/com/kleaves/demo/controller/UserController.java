package com.kleaves.demo.controller;

import com.kleaves.demo.model.User;
import com.kleaves.demo.model.UserCreateRequest;
import com.kleaves.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * ⚠️ 当前状态：POST /api/users 未使用 @Valid
 * 任务要求 Agent：加 @Valid + 全局异常处理返回 400 + 结构化错误信息
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> listAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ⚠️ 注意：@RequestBody 没有 @Valid — Agent 需要添加
     * 当前非法数据（空 username、无效 email 等）会直接入库
     */
    @PostMapping
    public ResponseEntity<User> create(@RequestBody UserCreateRequest request) {
        User saved = userService.create(request);
        URI location = URI.create("/api/users/" + saved.getId());
        return ResponseEntity.created(location).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (userService.deleteById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
