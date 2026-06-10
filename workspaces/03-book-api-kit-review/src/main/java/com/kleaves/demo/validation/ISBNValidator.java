package com.kleaves.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * ISBN 校验器实现
 *
 * 逻辑：
 *   1. null → true（交给 @NotBlank 报错）
 *   2. 去连字符，trim
 *   3. 检查长度是否为 10 或 13，且全为数字
 */
public class ISBNValidator implements ConstraintValidator<ISBN, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 交给 @NotBlank 处理
        }

        String cleaned = value.replace("-", "").trim();
        if (cleaned.isEmpty()) {
            return false;
        }

        return (cleaned.length() == 10 || cleaned.length() == 13)
                && cleaned.matches("\\d+");
    }
}
