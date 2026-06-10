package com.kleaves.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 自定义校验注解 — 价格格式校验
 *
 * 校验 Double 类型价格的小数位数不超过 2 位
 * null 值放行，交给 @NotNull 处理
 */
@Documented
@Constraint(validatedBy = PriceFormatValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PriceFormat {

    String message() default "价格最多保留两位小数";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
