package com.dk.seckillsystemexample.service;


import com.dk.seckillsystemexample.common.BizException;
import com.dk.seckillsystemexample.entity.User;
import com.dk.seckillsystemexample.repo.UserRepo;
import com.dk.seckillsystemexample.util.JwtUtil;
import com.dk.seckillsystemexample.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepo userRepo;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expireSeconds}")
    private long expireSeconds;

    public AuthService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Transactional
    public void register(String username, String password) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new BizException(409, "用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(PasswordUtil.hash(password));
        userRepo.save(u);
    }

    public String login(String username, String password) {
        User u = userRepo.findByUsername(username).orElseThrow(() -> new BizException(404, "用户不存在"));
        if (!PasswordUtil.matches(password, u.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }
        return JwtUtil.generateToken(u.getId(), u.getUsername(), jwtSecret, expireSeconds);
    }
}