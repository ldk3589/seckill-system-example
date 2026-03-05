package com.dk.seckillsystemexample.common;

public class Constants {
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER = "Bearer ";

    public static String redisStockKey(long productId) { return "stock:product:" + productId; }
    public static String redisUserOrderKey(long userId, long productId) { return "order:user:" + userId + ":product:" + productId; }

    // 订单状态
    public static String orderStatusKey(String orderNo) { return "order:status:" + orderNo; }

    // 消费失败重试次数
    public static String orderRetryKey(String orderNo) { return "order:retry:" + orderNo; }

    // 限流
    public static String rateLimitKey(long userId) { return "rl:seckill:user:" + userId; }
}