package com.dk.seckillsystemexample.dto;


import jakarta.validation.constraints.Min;

public class SeckillReq {
    @Min(value = 1, message="productId 必须>=1")
    public long productId;
}