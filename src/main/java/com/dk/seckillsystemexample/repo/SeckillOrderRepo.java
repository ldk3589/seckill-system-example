package com.dk.seckillsystemexample.repo;


import com.dk.seckillsystemexample.entity.SeckillOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeckillOrderRepo extends JpaRepository<SeckillOrder, Long> {
    Optional<SeckillOrder> findByUserIdAndProductId(long userId, long productId);
    Optional<SeckillOrder> findByOrderNo(String orderNo);
}