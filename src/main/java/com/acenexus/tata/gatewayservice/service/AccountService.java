package com.acenexus.tata.gatewayservice.service;

import com.acenexus.tata.gatewayservice.client.AccountServiceClient;
import com.acenexus.tata.gatewayservice.dto.AccountLoginRequest;
import com.acenexus.tata.gatewayservice.dto.AccountLoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
     * 用戶登入，使用 OpenFeign 調用 Account 微服務
     *
     * @param request 登入請求
     * @return 登入響應的 Mono
     */
    public Mono<AccountLoginResponse> login(AccountLoginRequest request) {
        log.info("調用 Account 服務進行登入: {}", request.getAccount());

        return Mono.fromCallable(() -> accountServiceClient.login(request))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(response -> log.info("登入成功: {}", response.getName()))
                .doOnError(error -> log.error("登入失敗: {}", error.getMessage()));
    }

}