package com.dk.seckillsystemexample.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "t_product")
public class Product {
    @Id
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getPrice() { return price; }
    public Integer getStatus() { return status; }
}