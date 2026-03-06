package com.dk.seckillsystemexample.service;

import com.dk.seckillsystemexample.common.Constants;
import com.dk.seckillsystemexample.repo.SeckillOrderRepo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class OrderService {

    private final SeckillOrderRepo orderRepo;
    private final StringRedisTemplate redis;

    public OrderService(SeckillOrderRepo orderRepo, StringRedisTemplate redis) {
        this.orderRepo = orderRepo;
        this.redis = redis;
    }

    /**
     * 返回：
     * - ACCEPTED：已受理/排队中
     * - SUCCESS：订单已生成（DB 已落库）
     * - FAILED：失败（进入 DLQ 或最终失败）
     * - UNKNOWN：查不到（可能 orderNo 不存在 / 过期）
     */
    public Map<String, Object> queryStatus(String orderNo) {
        String key = Constants.orderStatusKey(orderNo);
        String status = redis.opsForValue().get(key);

        if ("SUCCESS".equals(status)) {
            return Map.of("orderNo", orderNo, "status", "SUCCESS");
        }

        if ("FAILED".equals(status)) {
            return Map.of("orderNo", orderNo, "status", "FAILED");
        }

        if ("ACCEPTED".equals(status)) {
            boolean existsInDb = orderRepo.findByOrderNo(orderNo).isPresent();
            if (existsInDb) {
                redis.opsForValue().set(key, "SUCCESS", Duration.ofHours(1));
                return Map.of("orderNo", orderNo, "status", "SUCCESS");
            }
            return Map.of("orderNo", orderNo, "status", "ACCEPTED");
        }

        boolean exists = orderRepo.findByOrderNo(orderNo).isPresent();
        if (exists) {
            redis.opsForValue().set(key, "SUCCESS", Duration.ofHours(1));
            return Map.of("orderNo", orderNo, "status", "SUCCESS");
        }

        return Map.of("orderNo", orderNo, "status", "UNKNOWN");
    }
}