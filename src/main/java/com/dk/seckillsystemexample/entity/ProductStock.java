package com.dk.seckillsystemexample.entity;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "t_product_stock")
public class ProductStock {
    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getProductId() { return productId; }
    public Integer getStock() { return stock; }
    public Integer getVersion() { return version; }

    public void setStock(Integer stock) { this.stock = stock; }
    public void setVersion(Integer version) { this.version = version; }
}