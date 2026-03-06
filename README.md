[English](README.md) | [中文](README.zh-CN.md)
# Seckill System Example

A high-concurrency seckill (flash sale) system built with **Java 21 + Spring Boot + MySQL + Redis + JWT + Docker**.

It simulates a common e-commerce promotion scenario:

- limited stock
- many users competing at the same time
- oversell prevention
- duplicate order prevention
- asynchronous order creation
- Redis-based high-concurrency optimization

---

## 1. Project Overview

This project is designed as an interview-oriented backend project that demonstrates how to build a flash sale system under high concurrency.

Core goals:

- support user registration and login
- provide JWT-based authentication
- manage product and stock data
- implement a seckill API
- prevent overselling
- prevent duplicate orders
- use Redis + Lua for atomic pre-deduction
- use Redis Stream for asynchronous order processing
- support Docker deployment

---

## 2. Tech Stack

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- MySQL 8
- Redis 7
- JWT
- Docker / Docker Compose
- Vue 3 + Vite (frontend demo)

---

## 3. Core Features

### Authentication
- user registration
- user login
- JWT token generation
- protected API access

### Product & Stock
- product query
- stock warm-up into Redis
- database stock management

### Seckill
- Redis Lua atomic script:
    - check duplicate order
    - check stock
    - pre-decrement stock
    - write seckill message into Redis Stream
- asynchronous order creation by consumer
- duplicate order prevention
- order status query

### Reliability
- Redis Stream consumer group
- pending list retry
- dead letter queue (DLQ)
- Redis order status cache

### Performance Optimization
- cache penetration protection with null-cache
- cache breakdown protection with mutex lock
- user-level rate limiting
- asynchronous database write

---

## 4. Project Structure

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
       └─ Vue frontend demo
---
## 5. Business Flow
### Seckill request flow

1. User logs in and gets a JWT token

2. User calls the seckill API

3. Redis Lua script performs atomic checks:

- whether the user has already ordered

- whether stock is sufficient

4. If valid:

- Redis stock is decremented

- duplicate-order marker is stored

- order status is set to ACCEPTED

- message is written into Redis Stream

5. Background consumer reads the message from Stream

6. Order is inserted into MySQL

7. MySQL stock is decremented with stock > 0 condition

8. Order status becomes SUCCESS

9. User can query order status by orderNo
---
## 6. Key Design Points
### 6.1 Oversell Prevention

Two-layer protection is used:

- Redis pre-deduction in Lua

- MySQL conditional stock update:

      update ... set stock = stock - 1 where product_id = ? and stock > 0

### 6.2 Duplicate Order Prevention

Two-layer protection is used:

- Redis key: order: user:{userId}:product:{productId}

- MySQL unique index: (user_id, product_id)

### 6.3 Asynchronous Order Processing

The seckill API only performs:

- Lua execution

- writing to Redis Stream

Actual order creation is handled asynchronously by the consumer.

### 6.4 Order Status

Redis stores:

- ACCEPTED

- SUCCESS

- FAILED

This makes order-status query fast and user-friendly.

### 6.5 Retry & DLQ

If Stream consumption fails:

- retry count is increased

- failed messages remain in pending list

- after max retry, message is sent to DLQ

---
## 7. Environment Requirements

- JDK 21

- Maven 3.9+

- Docker Desktop

- Git

---
## 8. Configuration
Local profile

    src/main/resources/application.yml

Used when running backend directly from IDE or command line.

- MySQL: localhost:3307

- Redis: localhost:6379

Docker profile

    src/main/resources/application-docker.yml

Used when running the app inside Docker.

- MySQL: mysql:3306

- Redis: redis:6379

---
## 9. Quick Start with Docker
### 9.1 Build and start
    docker compose down -v
    docker compose up -d --build
### 9.2 Check containers
    docker ps

Expected containers:

- seckill-mysql

- seckill-redis

- seckill-app

### 9.3 Check database initialization
    docker exec -it seckill-mysql mysql -uroot -proot -e "SHOW DATABASES;"
    docker exec -it seckill-mysql mysql -uroot -proot -D seckill -e "SHOW TABLES;"
    docker exec -it seckill-mysql mysql -uroot -proot -D seckill -e "SELECT * FROM t_product; SELECT * FROM t_product_stock;"
### 9.4 Check Redis
    docker exec -it seckill-redis redis-cli ping

Expected result:

    PONG
### 9.5 Check application logs
    docker logs -n 200 seckill-app
---
## 10. Run Backend Locally

If you want to run the backend in IntelliJ IDEA:

1. Start MySQL and Redis with Docker:

       docker compose up -d mysql redis

2. Use application.yml

3. Run SeckillSystemExampleApplication

---
## 11. API Testing

You can test APIs with:

- IntelliJ HTTP Client

- Postman

- curl

Recommended file:

    src/test/http/seckill-test.http

#### Main APIs

Register

    POST /api/auth/register

Login

    POST /api/auth/login

Product detail

    GET /api/product/{id}

Warm up stock

    POST /api/product/{id}/warmup

Do seckill

    POST /api/seckill/do

Query order status

    GET /api/order/status/{orderNo}

---
## 12. Example Test Flow

1. Register a user

2. Login and get token

3. Warm up stock into Redis

4. Query product detail

5. Call seckill API

6. Get orderNo

7. Query order status until SUCCESS

---
## 13. Frontend Demo

A simple Vue 3 frontend is used for demonstration.

- Frontend features:

- register/login

- query product

- warm up stock

- submit seckill request

- poll order status

- display response logs

If frontend and backend run locally:

- frontend: http://localhost:5173

- backend: http://localhost:8080

---
## 14. Common Issues
### 14.1 MySQL connection failed

Check:

- whether MySQL container is running

- whether application.yml or application-docker.yml matches your running mode

- whether MySQL port mapping is correct

### 14.2 Docker app cannot start

Check:

- image build logs

- whether SPRING_PROFILES_ACTIVE=docker is set

- whether MySQL healthcheck passes first

### 14.3 Duplicate order returned

This is expected behavior. The same user cannot seckill the same product twice.

### 14.4 Product name is garbled

Check:

- MySQL database/table charset is utf8mb4

- schema.sql file encoding is UTF-8

- old corrupted data has been reset or updated

---
## 15. License

This project is for study, practice, and interview demonstration.