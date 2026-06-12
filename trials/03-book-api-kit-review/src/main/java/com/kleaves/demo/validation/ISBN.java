package com.kleaves.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 自定义校验注解 — ISBN 格式校验
 *
 * 校验规则：去连字符（-）后，必须为 10 位或 13 位纯数字
 * null 值放行，交给 @NotBlank 处理
 */
@Documented
@Constraint(validatedBy = ISBNValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ISBN {

    String message() default "ISBN格式不正确，必须为10位或13位数字";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
