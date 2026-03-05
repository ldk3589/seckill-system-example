package com.dk.seckillsystemexample.service;

import com.dk.seckillsystemexample.common.BizException;
import com.dk.seckillsystemexample.common.Constants;
import com.dk.seckillsystemexample.repo.SeckillOrderRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class SeckillService {

    private final StringRedisTemplate redis;
    private final SeckillOrderRepo orderRepo;

    @Value("${seckill.stream.key}")
    private String streamKey;

    @Value("${seckill.stream.dlqKey}")
    private String dlqKey;

    @Value("${seckill.rateLimit.perUserPerSecond}")
    private int perUserPerSecond;

    public SeckillService(StringRedisTemplate redis, SeckillOrderRepo orderRepo) {
        this.redis = redis;
        this.orderRepo = orderRepo;
    }

    // Lua：限流（每秒 N 次） 简化版：INCR + EXPIRE
    private static final DefaultRedisScript<Long> RATE_LIMIT_LUA = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])
            local c = redis.call('INCR', key)
            if c == 1 then
              redis.call('EXPIRE', key, ttl)
            end
            if c > limit then
              return 0
            end
            return 1
            """,
            Long.class
    );

    // Lua：原子校验 + 预减库存 + 防重复下单 + 写入 Stream
    // KEYS[1]=stockKey  KEYS[2]=userOrderKey  KEYS[3]=streamKey KEYS[4]=orderStatusKey
    // ARGV[1]=userId ARGV[2]=productId ARGV[3]=orderNo
    // 返回：1成功；-1库存不足；-2重复下单
    private static final DefaultRedisScript<Long> SECKILL_LUA = new DefaultRedisScript<>(
            """
            local stockKey = KEYS[1]
            local orderKey = KEYS[2]
            local streamKey = KEYS[3]
            local statusKey = KEYS[4]
            
            local userId = ARGV[1]
            local productId = ARGV[2]
            local orderNo = ARGV[3]
            
            if redis.call('EXISTS', orderKey) == 1 then
              return -2
            end
            
            local stock = tonumber(redis.call('GET', stockKey) or '-1')
            if stock <= 0 then
              return -1
            end
            
            redis.call('DECR', stockKey)
            redis.call('SET', orderKey, orderNo)
            redis.call('SET', statusKey, 'ACCEPTED')
            
            redis.call('XADD', streamKey, '*',
              'userId', userId,
              'productId', productId,
              'orderNo', orderNo
            )
            return 1
            """,
            Long.class
    );

    public String doSeckill(long userId, long productId) {
        // 1) 限流（用户维度）
        Long allowed = redis.execute(
                RATE_LIMIT_LUA,
                List.of(Constants.rateLimitKey(userId)),
                String.valueOf(perUserPerSecond),
                "1"
        );
        if (allowed == null || allowed == 0) {
            throw new BizException(429, "请求过于频繁");
        }

        String orderNo = "SN" + System.currentTimeMillis() + "_" + userId + "_" + productId;

        Long r = redis.execute(
                SECKILL_LUA,
                List.of(
                        Constants.redisStockKey(productId),
                        Constants.redisUserOrderKey(userId, productId),
                        streamKey,
                        Constants.orderStatusKey(orderNo)
                ),
                String.valueOf(userId),
                String.valueOf(productId),
                orderNo
        );

        if (r == null) throw new BizException(500, "Redis 执行失败");
        if (r == -1) throw new BizException(410, "库存不足");
        if (r == -2) {
            var existing = orderRepo.findByUserIdAndProductId(userId, productId);
            if (existing.isPresent()) {
                throw new BizException(409, "请勿重复下单，已有订单号：" + existing.get().getOrderNo());
            }
            throw new BizException(409, "请勿重复下单");
        }

        return orderNo;
    }

    public void ensureStreamGroup(String group) {
        try {
            redis.opsForStream().createGroup(streamKey, ReadOffset.from("0-0"), group);
        } catch (Exception ignored) {
            // group 已存在
        }
    }

    /** 读取新消息（>） */
    public List<MapRecord<String, Object, Object>> readNew(String group, String consumer) {
        return redis.opsForStream().read(
                Consumer.from(group, consumer),
                StreamReadOptions.empty().count(20).block(Duration.ofSeconds(2)),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        );
    }

    /** 读取 Pending 消息（从 0-0 开始，能把 pending 拉出来） */
    public List<MapRecord<String, Object, Object>> readPending(String group, String consumer) {
        return redis.opsForStream().read(
                Consumer.from(group, consumer),
                StreamReadOptions.empty().count(20),
                StreamOffset.create(streamKey, ReadOffset.from("0-0"))
        );
    }

    public void ack(String group, RecordId id) {
        redis.opsForStream().acknowledge(streamKey, group, id);
    }

    public void addToDlq(Map<Object, Object> fields) {
        // DLQ 里保留原始字段 + 你也可以加 failReason
        redis.opsForStream().add(dlqKey, fields);
    }
}