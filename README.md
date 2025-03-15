# Gateway Service

## 概述

基於 **Spring Cloud Gateway**，提供 **API 網關** 功能，作為微服務架構中的統一入口，實現路由轉發、負載均衡、請求過濾與跨域支援等功能。

## 快速入門

### Gateway 微服務初始化步驟

#### 1. 配置 `build.gradle.kts`

```kotlin
dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
```

#### 2. 配置 `application.yml`

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: gatewayservice
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: accountservice
          uri: lb://accountservice
          predicates:
            - Path=/api/account/**
          filters:
            - StripPrefix=2

eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_SERVER_HOST:127.0.0.1}:${EUREKA_SERVER_PORT:8761}/eureka/

management:
  endpoints:
    web:
      exposure:
        include: "*"
      base-path: /actuator
```

## 參考資源

- [Spring Cloud Gateway Reference](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring Cloud Netflix Documentation](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/)