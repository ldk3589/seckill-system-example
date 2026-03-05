package com.dk.seckillsystemexample.config;


import com.dk.seckillsystemexample.common.Constants;
import com.dk.seckillsystemexample.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends GenericFilter {

    @Value("${app.jwt.secret}")
    private String secret;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        String auth = request.getHeader(Constants.AUTH_HEADER);

        if (auth != null && auth.startsWith(Constants.BEARER)) {
            String token = auth.substring(Constants.BEARER.length());
            try {
                Jws<Claims> jws = JwtUtil.parse(token, secret);
                long userId = Long.parseLong(jws.getBody().getSubject());
                String username = String.valueOf(jws.getBody().get("username"));

                var principal = new org.springframework.security.core.userdetails.User(
                        username, "", List.of()
                );

                var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                request.setAttribute("userId", userId);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                // token 无效则当未登录处理
            }
        }

        chain.doFilter(req, res);
    }
}