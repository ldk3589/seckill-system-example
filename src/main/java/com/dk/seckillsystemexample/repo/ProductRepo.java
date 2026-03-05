package com.dk.seckillsystemexample.repo;


import com.dk.seckillsystemexample.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<Product, Long> {
}