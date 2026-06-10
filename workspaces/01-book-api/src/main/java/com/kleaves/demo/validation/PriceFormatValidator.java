package com.kleaves.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * 价格格式校验器 — 校验 Double 值的小数位数不超过 2 位
 */
public class PriceFormatValidator implements ConstraintValidator<PriceFormat, Double> {

    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 交给 @NotNull 处理
        }
        // BigDecimal.valueOf 取 Double 的精确字符串表示，避免浮点精度问题
        return BigDecimal.valueOf(value).scale() <= 2;
    }
}
