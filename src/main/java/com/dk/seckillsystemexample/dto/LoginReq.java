package com.dk.seckillsystemexample.dto;


import jakarta.validation.constraints.NotBlank;

public class LoginReq {
    @NotBlank(message="用户名不能为空")
    public String username;

    @NotBlank(message="密码不能为空")
    public String password;
}