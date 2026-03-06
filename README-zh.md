[English](README.md) | [中文](README.zh-CN.md)
# 高并发秒杀系统示例

这是一个基于 **Java 21 + Spring Boot + MySQL + Redis + JWT + Docker** 实现的高并发秒杀系统示例项目。

该项目模拟了电商大促中的典型场景：

- 商品库存有限
- 大量用户同时抢购
- 防止超卖
- 防止重复下单
- 异步生成订单
- 基于 Redis 的高并发优化

---

## 1. 项目简介

本项目是一个偏面试展示型的后端项目，重点演示如何设计和实现一个高并发秒杀系统。

核心目标：

- 支持用户注册与登录
- 基于 JWT 的身份认证
- 管理商品和库存
- 提供秒杀接口
- 防止超卖
- 防止重复下单
- 使用 Redis + Lua 实现原子预扣减
- 使用 Redis Stream 异步处理订单
- 支持 Docker 部署

---

## 2. 技术栈

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- MySQL 8
- Redis 7
- JWT
- Docker / Docker Compose
- Vue 3 + Vite（前端演示页面）

---

## 3. 核心功能

### 用户认证
- 用户注册
- 用户登录
- 生成 JWT Token
- 受保护接口鉴权访问

### 商品与库存
- 商品查询
- 库存预热到 Redis
- 数据库存储商品库存

### 秒杀核心
- Redis Lua 原子脚本完成：
  - 校验是否重复下单
  - 校验库存是否充足
  - 预扣减库存
  - 写入 Redis Stream
- 异步消费者创建订单
- 防重复下单
- 订单状态查询

### 可靠性设计
- Redis Stream 消费组
- Pending List 重试
- 死信队列（DLQ）
- Redis 订单状态缓存

### 性能优化
- 缓存穿透处理：空值缓存
- 缓存击穿处理：互斥锁重建缓存
- 用户级接口限流
- 异步落库提升吞吐量

---

## 4. 项目结构
    seckill-system-example/
    ├─ docker-compose.yml
    ├─ Dockerfile
    ├─ pom.xml
    ├─ README.md
    ├─ README-zh.md
    ├─ src/
    │  ├─ main/
    │  │  ├─ java/com/dk/seckillsystemexample/
    │  │  │  ├─ common/
    │  │  │  ├─ config/
    │  │  │  ├─ controller/
    │  │  │  ├─ dto/
    │  │  │  ├─ entity/
    │  │  │  ├─ repo/
    │  │  │  ├─ service/
    │  │  │  ├─ util/
    │  │  │  └─ SeckillSystemExampleApplication.java
    │  │  └─ resources/
    │  │     ├─ application.yml
    │  │     ├─ application-docker.yml
    │  │     └─ schema.sql
    │  └─ test/
    │     └─ http/
    │        └─ seckill-test.http
    └─ seckill-web/
       └─ Vue 前端演示页面
---
## 5. 业务流程
### 秒杀请求主流程

1. 用户登录，获取 JWT Token

2. 用户调用秒杀接口

3. Redis Lua 脚本原子执行：

- 判断用户是否已下单

- 判断库存是否充足

4. 若通过校验：

- Redis 扣减库存

- 写入用户下单标记

- 订单状态写入 ACCEPTED

- 秒杀消息写入 Redis Stream

5. 后台消费者异步读取 Stream

6. 将订单写入 MySQL

7. 使用 stock > 0 条件更新扣减数据库库存

8. 订单状态变为 SUCCESS

9. 用户通过 orderNo 查询订单状态

---
## 6. 关键设计点
### 6.1 防超卖

采用双层保护：

- Redis Lua 预扣减库存

- MySQL 条件更新库存

      update ... set stock = stock - 1 where product_id = ? and stock > 0

### 6.2 防重复下单

采用双层保护：

- Redis 标记：order:user:{userId}:product:{productId}

- MySQL 唯一索引：(user_id, product_id)

### 6.3 异步落库

秒杀接口本身只做两件事：

- 执行 Lua

- 写入 Redis Stream

真正的订单生成由后台消费者异步完成，从而降低接口响应时间、提升系统吞吐量。

### 6.4 订单状态设计

Redis 中维护订单状态：

- ACCEPTED

- SUCCESS

- FAILED

这样用户查询订单状态会更快，也更适合秒杀场景。

### 6.5 重试与死信队列

如果 Stream 消费失败：

- 增加重试次数

- 消息保留在 Pending List 中等待重试

- 超过最大重试次数后写入 DLQ

---
## 7. 环境要求

- JDK 21

- Maven 3.9+

- Docker Desktop

- Git

---
## 8. 配置说明
本机运行配置

    src/main/resources/application.yml

用于在 IDEA 或命令行中直接启动后端服务。

- MySQL：localhost:3307

- Redis：localhost:6379

Docker 运行配置

    src/main/resources/application-docker.yml

用于将应用运行在 Docker 容器中。

- MySQL：mysql:3306

- Redis：redis:6379

## 9. 使用 Docker 快速启动
### 9.1 构建并启动
    docker compose down -v
    docker compose up -d --build
### 9.2 检查容器状态
    docker ps

预期容器：

- seckill-mysql

- seckill-redis

- seckill-app

### 9.3 检查数据库初始化
    docker exec -it seckill-mysql mysql -uroot -proot -e "SHOW DATABASES;"
    docker exec -it seckill-mysql mysql -uroot -proot -D seckill -e "SHOW TABLES;"
    docker exec -it seckill-mysql mysql -uroot -proot -D seckill -e "SELECT * FROM t_product; SELECT * FROM t_product_stock;"
### 9.4 检查 Redis
    docker exec -it seckill-redis redis-cli ping

预期输出：

    PONG
### 9.5 查看应用日志
    docker logs -n 200 seckill-app
---
## 10. 本机运行后端

如果你希望在 IntelliJ IDEA 中直接运行后端：

1. 先启动 MySQL 和 Redis：

        docker compose up -d mysql redis

2. 使用默认配置 application.yml

3. 运行 SeckillSystemExampleApplication

---
## 11. 接口测试

你可以用以下方式测试接口：

- IntelliJ HTTP Client

- Postman

- curl

推荐使用项目中的测试文件：

    src/test/http/seckill-test.http

#### 主要接口
注册

    POST /api/auth/register

登录

    POST /api/auth/login

查询商品

    GET /api/product/{id}

预热库存

    POST /api/product/{id}/warmup

秒杀下单

    POST /api/seckill/do

查询订单状态

    GET /api/order/status/{orderNo}

---
## 12. 示例测试流程

1. 注册用户

2. 登录并获取 token

3. 预热库存到 Redis

4. 查询商品详情

5. 调用秒杀接口

6. 获取 orderNo

7. 循环查询订单状态直到变成 SUCCESS

---
## 13. 前端演示页面

项目配套了一个简单的 Vue 3 演示前端，便于演示完整流程。

前端功能包括：

- 注册 / 登录

- 查询商品

- 预热库存

- 提交秒杀请求

- 轮询订单状态

- 展示接口日志

如果前后端都本机运行：

- 前端：http://localhost:5173

- 后端：http://localhost:8080

---
## 14. 常见问题
### 14.1 MySQL 连接失败

请检查：

- MySQL 容器是否正常启动

- 当前使用的是本机配置还是 Docker 配置

- MySQL 端口映射是否正确

### 14.2 Docker 中 app 启动失败

请检查：

- 镜像构建日志

- 是否启用了 SPRING_PROFILES_ACTIVE=docker

- MySQL 健康检查是否先通过

### 14.3 返回重复下单

这是系统设计的正常行为，同一用户不能对同一商品重复秒杀。

### 14.4 商品名乱码

请检查：

- MySQL 数据库和表的字符集是否为 utf8mb4

- schema.sql 是否以 UTF-8 保存

- 旧的乱码数据是否已经被删除或重新插入

---
## 15.说明

本项目主要用于学习、练习和面试展示。