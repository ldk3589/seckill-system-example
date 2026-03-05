package com.dk.seckillsystemexample.service;

import com.dk.seckillsystemexample.common.BizException;
import com.dk.seckillsystemexample.common.Constants;
import com.dk.seckillsystemexample.entity.Product;
import com.dk.seckillsystemexample.entity.ProductStock;
import com.dk.seckillsystemexample.repo.ProductRepo;
import com.dk.seckillsystemexample.repo.ProductStockRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepo productRepo;
    private final ProductStockRepo stockRepo;
    private final StringRedisTemplate redis;

    @Value("${seckill.cache.productTtlSeconds}")
    private long productTtlSeconds;

    @Value("${seckill.cache.nullTtlSeconds}")
    private long nullTtlSeconds;

    public ProductService(ProductRepo productRepo, ProductStockRepo stockRepo, StringRedisTemplate redis) {
        this.productRepo = productRepo;
        this.stockRepo = stockRepo;
        this.redis = redis;
    }

    // 简化：返回 "name|price|stock"
    public String getProductDetail(long productId) {
        String cacheKey = "cache:product:" + productId;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            if ("NULL".equals(cached)) throw new BizException(404, "商品不存在");
            return cached;
        }

        // 缓存击穿：互斥锁重建
        String lockKey = "lock:cache:product:" + productId;
        String lockVal = UUID.randomUUID().toString();
        boolean locked = Boolean.TRUE.equals(redis.opsForValue()
                .setIfAbsent(lockKey, lockVal, Duration.ofSeconds(3)));

        try {
            if (!locked) {
                // 没拿到锁：短暂等待后再读一次缓存
                try { Thread.sleep(40); } catch (InterruptedException ignored) {}
                String retry = redis.opsForValue().get(cacheKey);
                if (retry != null) {
                    if ("NULL".equals(retry)) throw new BizException(404, "商品不存在");
                    return retry;
                }
            }

            Optional<Product> pOpt = productRepo.findById(productId);
            if (pOpt.isEmpty()) {
                redis.opsForValue().set(cacheKey, "NULL", Duration.ofSeconds(nullTtlSeconds)); // 空值缓存：防穿透
                throw new BizException(404, "商品不存在");
            }
            ProductStock s = stockRepo.findById(productId).orElse(null);
            int stock = (s == null) ? 0 : s.getStock();

            String val = pOpt.get().getName() + "|" + pOpt.get().getPrice() + "|" + stock;
            redis.opsForValue().set(cacheKey, val, Duration.ofSeconds(productTtlSeconds));
            return val;
        } finally {
            // 解锁：简单做法（面试可讲“需要校验 lockVal 防误删”，这里也做一下）
            String cur = redis.opsForValue().get(lockKey);
            if (lockVal.equals(cur)) redis.delete(lockKey);
        }
    }

    // 初始化 Redis 库存（部署/压测前执行一次）
    public void warmUpStockToRedis(long productId) {
        ProductStock s = stockRepo.findById(productId)
                .orElseThrow(() -> new BizException(404, "库存记录不存在"));
        redis.opsForValue().set(Constants.redisStockKey(productId), String.valueOf(s.getStock()));
    }
}