package com.kleaves.demo.service;

import com.kleaves.demo.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ⚠️ 当前状态：每次 findById 都查内存 Map，无任何缓存
 * 任务要求：添加 Redis 缓存，@Cacheable / @CacheEvict，TTL 可配置
 */
@Service
public class ProductService {

    private final Map<Long, Product> productStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public ProductService() {
        addSample("机械键盘", "Cherry MX 青轴，87键", new BigDecimal("399.00"), 50);
        addSample("无线鼠标", "罗技 MX Master 3S", new BigDecimal("699.00"), 30);
        addSample("显示器", "27寸 4K IPS", new BigDecimal("2999.00"), 15);
        addSample("笔记本支架", "铝合金可升降", new BigDecimal("159.00"), 100);
    }

    private void addSample(String name, String desc, BigDecimal price, Integer stock) {
        long id = idGenerator.getAndIncrement();
        productStore.put(id, new Product(id, name, desc, price, stock));
    }

    public List<Product> findAll() {
        return new ArrayList<>(productStore.values());
    }

    /** 每次查 Map — 无缓存 */
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(productStore.get(id));
    }

    public Product create(Product product) {
        long id = idGenerator.getAndIncrement();
        product.setId(id);
        productStore.put(id, product);
        return product;
    }

    public Optional<Product> update(Long id, Product newData) {
        return Optional.ofNullable(productStore.get(id)).map(existing -> {
            if (newData.getName() != null) existing.setName(newData.getName());
            if (newData.getDescription() != null) existing.setDescription(newData.getDescription());
            if (newData.getPrice() != null) existing.setPrice(newData.getPrice());
            if (newData.getStock() != null) existing.setStock(newData.getStock());
            return existing;
        });
    }

    public boolean deleteById(Long id) {
        return productStore.remove(id) != null;
    }
}
