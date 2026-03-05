package com.dk.seckillsystemexample.controller;


import com.dk.seckillsystemexample.common.ApiResponse;
import com.dk.seckillsystemexample.common.BizException;
import com.dk.seckillsystemexample.dto.SeckillReq;
import com.dk.seckillsystemexample.service.SeckillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;
    public SeckillController(SeckillService seckillService) { this.seckillService = seckillService; }

    @PostMapping("/do")
    public ApiResponse<Map<String, String>> doSeckill(@Valid @RequestBody SeckillReq req,
                                                      HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) throw new BizException(401, "未登录");
        long userId = (long) uidObj;

        String orderNo = seckillService.doSeckill(userId, req.productId);
        return ApiResponse.ok(Map.of("orderNo", orderNo, "msg", "下单请求已受理"));
    }
}