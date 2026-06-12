package com.kleaves.demo.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * 统一将校验异常转换为结构化 JSON 响应：
 *   {"errors": [{"field": "...", "message": "..."}, ...]}
 *
 * @RestControllerAdvice 会拦截所有 @RestController 抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @Valid 触发的校验失败（POST 创建图书）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("field", fieldError.getField());
            error.put("message", fieldError.getDefaultMessage());
            errors.add(error);
        });
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 处理手动 Validator 触发的校验失败（PUT 部分更新）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            Map<String, String> error = new LinkedHashMap<>();
            // 取属性路径的最后一段作为 field 名
            String propertyPath = violation.getPropertyPath().toString();
            String field = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                    : propertyPath;
            error.put("field", field);
            error.put("message", violation.getMessage());
            errors.add(error);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }
}
