package com.kleaves.demo.model;

import java.util.List;

/**
 * 分页响应通用包装
 *
 * @param <T>        数据项类型
 * @param data       当前页数据列表
 * @param total      数据总条数
 * @param page       当前页码（从 1 开始）
 * @param size       每页条数
 * @param totalPages 总页数
 */
public record PageResponse<T>(
        List<T> data,
        long total,
        int page,
        int size,
        int totalPages
) {}
