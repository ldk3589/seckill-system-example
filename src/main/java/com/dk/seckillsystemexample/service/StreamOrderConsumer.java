package com.dk.seckillsystemexample.service;

import com.dk.seckillsystemexample.common.Constants;
import com.dk.seckillsystemexample.entity.SeckillOrder;
import com.dk.seckillsystemexample.repo.ProductStockRepo;
import com.dk.seckillsystemexample.repo.SeckillOrderRepo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
public class StreamOrderConsumer {

    private final SeckillService seckillService;
    private final SeckillOrderRepo orderRepo;
    private final ProductStockRepo stockRepo;
    private final StringRedisTemplate redis;

    @Value("${seckill.stream.group}")
    private String group;

    @Value("${seckill.stream.consumer}")
    private String consumer;

    @Value("${seckill.consumer.maxRetry}")
    private int maxRetry;

    @Value("${seckill.orderStatus.ttlSeconds}")
    private long statusTtlSeconds;

    public StreamOrderConsumer(SeckillService seckillService,
                               SeckillOrderRepo orderRepo,
                               ProductStockRepo stockRepo,
                               StringRedisTemplate redis) {
        this.seckillService = seckillService;
        this.orderRepo = orderRepo;
        this.stockRepo = stockRepo;
        this.redis = redis;
    }

    @PostConstruct
    public void init() {
        seckillService.ensureStreamGroup(group);
    }

    /** 1) 正常消费新消息 */
    @Scheduled(fixedDelay = 150)
    public void pollNew() {
        List<MapRecord<String, Object, Object>> records = seckillService.readNew(group, consumer);
        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> r : records) {
            processOne(r);
        }
    }

    /** 2) 定期扫描 Pending List（处理消费者宕机/异常导致的未 ack 消息） */
    @Scheduled(fixedDelay = 1200)
    public void pollPending() {
        List<MapRecord<String, Object, Object>> records = seckillService.readPending(group, consumer);
        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> r : records) {
            processOne(r);
        }
    }

    private void processOne(MapRecord<String, Object, Object> r) {
        Map<Object, Object> v = r.getValue();
        String orderNo = String.valueOf(v.get("orderNo"));

        try {
            handleRecordTransactional(v);

            // 成功：更新状态 + ack
            redis.opsForValue().set(Constants.orderStatusKey(orderNo), "SUCCESS", Duration.ofSeconds(statusTtlSeconds));
            redis.delete(Constants.orderRetryKey(orderNo));
            seckillService.ack(group, r.getId());

        } catch (Exception e) {
            // 失败：+1 重试计数
            Long retry = redis.opsForValue().increment(Constants.orderRetryKey(orderNo));
            if (retry != null && retry == 1) {
                redis.expire(Constants.orderRetryKey(orderNo), Duration.ofHours(2));
            }

            if (retry != null && retry >= maxRetry) {
                // 超过最大重试：进 DLQ，标记失败，并 ack 原消息（避免一直卡 pending）
                v.put("failReason", e.getClass().getSimpleName() + ":" + e.getMessage());
                v.put("retry", String.valueOf(retry));
                seckillService.addToDlq(v);

                redis.opsForValue().set(Constants.orderStatusKey(orderNo), "FAILED", Duration.ofSeconds(statusTtlSeconds));
                seckillService.ack(group, r.getId());
            }
            // 未到阈值：不 ack，让它留在 pending，等待下次 pollPending 重试
        }
    }

    @Transactional
    public void handleRecordTransactional(Map<Object, Object> v) {
        long userId = Long.parseLong(String.valueOf(v.get("userId")));
        long productId = Long.parseLong(String.valueOf(v.get("productId")));
        String orderNo = String.valueOf(v.get("orderNo"));

        // 1) 订单落库（唯一约束防重复）
        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setStatus(0);

        try {
            orderRepo.save(order);
        } catch (DataIntegrityViolationException ex) {
            // 重复消息/重复请求：视作成功
            return;
        }

        // 2) 扣减 MySQL 库存（兜底：stock>0）
        int updated = stockRepo.decrementIfStock(productId);
        if (updated <= 0) {
            // 这里抛异常触发重试；生产会做补偿/对账任务
            throw new IllegalStateException("DB 库存扣减失败(可能不一致)");
        }
    }
}