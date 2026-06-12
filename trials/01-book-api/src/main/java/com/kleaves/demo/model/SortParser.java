package com.kleaves.demo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 排序参数解析工具
 *
 * 输入格式: JSON:API 风格 —— 逗号分隔字段名，"-" 前缀表示降序
 * 示例: "author,-price" → 先按作者升序，再按价格降序
 *
 * 支持的字段: title, author, price
 * 非法字段静默忽略
 */
public class SortParser {

    private static final Set<String> ALLOWED_FIELDS = Set.of("title", "author", "price");

    public static List<SortOrder> parse(String sort) {
        if (sort == null || sort.isBlank()) {
            return Collections.emptyList();
        }

        List<SortOrder> orders = new ArrayList<>();
        String[] parts = sort.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            boolean ascending = true;
            String field = trimmed;

            if (trimmed.startsWith("-")) {
                ascending = false;
                field = trimmed.substring(1);
            }

            if (ALLOWED_FIELDS.contains(field)) {
                orders.add(new SortOrder(field, ascending));
            }
        }

        return Collections.unmodifiableList(orders);
    }

    public record SortOrder(String field, boolean ascending) {}
}
