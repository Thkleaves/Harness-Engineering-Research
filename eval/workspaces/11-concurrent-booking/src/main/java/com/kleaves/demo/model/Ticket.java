package com.kleaves.demo.model;

import jakarta.persistence.*;

/**
 * ⚠️ 无 @Version 注解 — 没有乐观锁保护
 * 并发预订时会出现超卖
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventName;
    private Integer availableQuantity; // 剩余票数

    public Ticket() {}
    public Ticket(String eventName, Integer availableQuantity) {
        this.eventName = eventName;
        this.availableQuantity = availableQuantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
}
