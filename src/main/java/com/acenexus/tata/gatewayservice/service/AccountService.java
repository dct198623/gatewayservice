package com.acenexus.tata.gatewayservice.service;

import com.acenexus.tata.gatewayservice.dto.AccountLoginRequest;
import com.acenexus.tata.gatewayservice.dto.AccountLoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Account 服務類別
 * 在 WebFlux 環境中使用 OpenFeign 客戶端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountServiceClient accountServiceClient;

    /**
     * 用戶登入
     * 使用 OpenFeign 調用 Account 微服務
     *
     * @param request 登入請求
     * @return 登入響應的 Mono
     */
    public Mono<AccountLoginResponse> login(AccountLoginRequest request) {
        log.info("調用 Account 服務進行登入: {}", request.getAccount());

        return Mono.fromCallable(() -> accountServiceClient.login(request))
                .subscribeOn(Schedulers.boundedElastic()) // 使用有界彈性執行器處理阻塞操作
                .doOnSuccess(response -> log.info("登入成功: {}", response.getName()))
                .doOnError(error -> log.error("登入失敗: {}", error.getMessage()));
    }

    /**
     * Account 微服務的 Feign 客戶端
     * 本地環境：使用固定 URL http://localhost:8081
     * 生產環境：使用服務名稱 accountservice 進行服務發現
     */
    @FeignClient(
            name = "accountservice",
            url = "${feign.client.accountservice.url:}", // 本地環境使用固定 URL
            path = "/v1/user"
    )
    public interface AccountServiceClient {
        @PostMapping("/login")
        AccountLoginResponse login(@RequestBody AccountLoginRequest accountLoginRequest);
    }

}