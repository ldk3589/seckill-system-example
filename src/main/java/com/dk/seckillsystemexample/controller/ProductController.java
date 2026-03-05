package com.dk.seckillsystemexample.controller;


import com.dk.seckillsystemexample.common.ApiResponse;
import com.dk.seckillsystemexample.service.ProductService;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;
    public ProductController(ProductService productService) { this.productService = productService; }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") @Min(1) long id) {
        String val = productService.getProductDetail(id);
        String[] arr = val.split("\\|");
        return ApiResponse.ok(Map.of(
                "name", arr[0],
                "price", Long.parseLong(arr[1]),
                "stock", Integer.parseInt(arr[2])
        ));
    }

    @PostMapping("/{id}/warmup")
    public ApiResponse<Void> warmup(@PathVariable("id") @Min(1) long id) {
        productService.warmUpStockToRedis(id);
        return ApiResponse.ok(null);
    }
}