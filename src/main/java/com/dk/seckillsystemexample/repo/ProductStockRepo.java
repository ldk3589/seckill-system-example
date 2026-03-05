package com.dk.seckillsystemexample.repo;


import com.dk.seckillsystemexample.entity.ProductStock;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductStockRepo extends JpaRepository<ProductStock, Long> {

    @Modifying
    @Query("update ProductStock s set s.stock = s.stock - 1, s.version = s.version + 1 where s.productId = :pid and s.stock > 0")
    int decrementIfStock(@Param("pid") long productId);
}