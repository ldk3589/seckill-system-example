package com.dk.seckillsystemexample.entity;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "t_seckill_order",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_user_product", columnNames={"user_id","product_id"})
        })
public class SeckillOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="order_no", nullable=false, unique=true, length=64)
    private String orderNo;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="product_id", nullable=false)
    private Long productId;

    @Column(nullable=false)
    private Integer status; // 0 created

    @Column(name="created_at", nullable=false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }

    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setStatus(Integer status) { this.status = status; }
}