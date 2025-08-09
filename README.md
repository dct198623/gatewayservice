# Gateway Service

![Spring Boot Version](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)
![Spring Cloud Version](https://img.shields.io/badge/Spring%20Cloud-2024.0.0-blue.svg)
![Java Version](https://img.shields.io/badge/Java-21-orange.svg)
![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)

## 概述

Gateway Service 是基於 Spring Cloud Gateway 的 API 網關服務，作為微服務架構中的統一入口。主要特點包括：

- **路由轉發**：路由請求至相應微服務
- **負載均衡**：整合 Spring Cloud LoadBalancer 實現請求分發
- **請求過濾**：支持請求預處理、修改與後處理
- **動態路由**：支持配置中心動態路由規則
- **服務發現整合**：自動發現註冊於 Eureka 的服務
- **跨域支援**：預設實現 CORS 跨域資源共享
- **安全控制**：可整合 Spring Security 實現身份驗證與授權

## 技術堆疊

1. **框架**: Spring Boot 3.4.3
2. **API 網關**: Spring Cloud Gateway 2024.0.0
3. **服務發現**: Spring Cloud Netflix Eureka Client
4. **負載均衡**: Spring Cloud LoadBalancer
5. **監控**: Spring Boot Actuator
6. **Java 版本**: Java 21
7. **構建工具**: Gradle (Kotlin DSL)
8. **版本控制**: 自動從 Git Tag 獲取版本號

## 系統架構

### 1. 路由配置

預設配置中包含多個路由規則，以 `/api/{服務名稱}/**` 的模式轉發至相應的微服務。

```
┌───────────────┐                  
│               │  /api/account/** ┌─────────────────┐
│               │ ────────────────>│  accountservice │
│   Gateway     │                  └─────────────────┘
│   Service     │                  
│               │  /api/order/**   ┌─────────────────┐
│               │ ────────────────>│   orderservice  │
└───────────────┘                  └─────────────────┘
```

### 2. 健康檢查與監控

使用 Spring Boot Actuator 提供的健康檢查端點：

```
http://localhost:8080/actuator/health
```

## 快速入門

### Gateway 微服務初始化步驟

1. **添加依賴**：
   ```kotlin
   dependencies {
       implementation("org.springframework.cloud:spring-cloud-starter-gateway")
       implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
       implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
       implementation("org.springframework.boot:spring-boot-starter-actuator")
   }
   ```

2. **配置 application.yml**：[bootstrap-local.yml](src%2Fmain%2Fresources%2Fbootstrap-local.yml)

3. **啟動 Gateway 服務
   **：[GatewayserviceApplication.java](src%2Fmain%2Fjava%2Fcom%2Facenexus%2Ftata%2Fgatewayservice%2FGatewayserviceApplication.java)

## 部署指南

### 1. 自動部署 (GitHub Actions)

```bash
git checkout main
git pull --rebase origin main
git tag -a v0.0.1 -m "v0.0.1"
git push origin --tags
```

### 2. 手動部署

1. **建立資料夾**：
   ```shell
   sudo mkdir -p /opt/tata/gatewayservice
   ```

2. **放置 JAR 文件**：將 gatewayservice.jar 放入 /opt/tata/gatewayservice

3. **建立 Dockerfile**：
   ```shell
   sudo touch /opt/tata/gatewayservice/Dockerfile
   sudo chown -R ubuntu:ubuntu /opt/tata/gatewayservice/Dockerfile
   ```

4. **建構與啟動**：
   ```shell
   cd /opt/tata/gatewayservice
   docker build --no-cache -t gatewayservice .
   
   # 啟動容器（含環境變數設定）
   docker run -di --name=gatewayservice \
     -p 8080:8080 \
     -e SERVER_HOST=3.27.141.54 \
     -e SERVER_PORT=8080 \
     -e SPRING_PROFILES_ACTIVE=prod \
     -e CONFIG_SERVER_USERNAME=admin \
     -e CONFIG_SERVER_PASSWORD=password \
     -e CONFIG_SERVER_URI=http://3.25.200.179:8888 \
     -e EUREKA_SERVER_HOST=3.25.200.179 \
     -e EUREKA_SERVER_PORT=8761 \
     gatewayservice
   ```

5. **確認狀態**：
   ```shell
   docker logs -f --tail 1000 gatewayservice
   ```

## 自定義路由配置

為添加新的路由，在 `application.yml` 中的 `spring.cloud.gateway.routes` 區塊添加新項目：

```yaml
routes:
  - id: productservice          # 路由唯一識別符
    uri: lb://productservice    # 目標服務（lb:// 前綴表示使用負載均衡）
    predicates:
      - Path=/api/product/**    # 路徑匹配條件
    filters:
      - StripPrefix=2           # 移除前綴（此例中為 "api/product"）
```

## 版本管理

版本號採用語義化版本規範 (Semantic Versioning)：

- **MAJOR**: 不兼容的重大變更（例：1.0.0 → 2.0.0）
- **MINOR**: 新增功能，向下相容（例：1.1.0 → 1.2.0）
- **PATCH**: 修正錯誤，無功能變化（例：1.2.1 → 1.2.2）

## 常見問題

1. **路由無法正確轉發？**
    - 確認目標服務名稱在 Eureka 中正確註冊
    - 檢查路由配置中的路徑設定
    - 驗證前綴移除設定是否正確

2. **高延遲問題？**
    - 檢查目標服務的健康狀態
    - 檢查網路連接與防火墻設定

3. **跨域請求被拒絕？**
    - 確認 CORS 配置已正確啟用
    - 檢查允許的來源域名設定
    - 驗證前端請求頭是否符合要求

## 參考資源

- [Spring Cloud Gateway Reference](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring Cloud Netflix Documentation](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/)
- [Spring Boot Actuator Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)