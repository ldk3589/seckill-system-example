package com.dk.seckillsystemexample.controller;


import com.dk.seckillsystemexample.common.ApiResponse;
import com.dk.seckillsystemexample.common.BizException;
import com.dk.seckillsystemexample.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @GetMapping("/status/{orderNo}")
    public ApiResponse<Map<String, Object>> status(@PathVariable("orderNo") String orderNo, HttpServletRequest request) {
        // 必须登录（与秒杀一致）
        if (request.getAttribute("userId") == null) throw new BizException(401, "未登录");
        return ApiResponse.ok(orderService.queryStatus(orderNo));
    }
}