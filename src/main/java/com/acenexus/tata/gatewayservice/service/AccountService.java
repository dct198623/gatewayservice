package com.acenexus.tata.gatewayservice.service;

import com.acenexus.tata.gatewayservice.client.AccountServiceClient;
import com.acenexus.tata.gatewayservice.dto.AccountLoginRequest;
import com.acenexus.tata.gatewayservice.dto.LoginResponse;
import com.acenexus.tata.gatewayservice.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Account 服務類別
 * 在 WebFlux 環境中使用 OpenFeign 客戶端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountServiceClient accountServiceClient;
    private final JwtTokenProvider jwtTokenProvider;

    public Mono<ResponseEntity<LoginResponse>> login(AccountLoginRequest request) {
//        return Mono.fromSupplier(() -> new AccountLoginResponse(1001L, request.getAccount() != null ? request.getAccount() : "假用戶")) // 假資料
        return Mono.fromCallable(() -> accountServiceClient.login(request))
                .map(accountLoginResponse -> {
                    String accessToken = jwtTokenProvider.generateAccessToken(accountLoginResponse.getId(), accountLoginResponse.getName());
                    String refreshToken = jwtTokenProvider.generateRefreshToken(accountLoginResponse.getId(), accountLoginResponse.getName());
                    LoginResponse loginResponse = new LoginResponse(accountLoginResponse.getId(), accountLoginResponse.getName(), accessToken, refreshToken);
                    return ResponseEntity.ok(loginResponse);
                })
                .onErrorResume(e -> {
                    log.error("Login error: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

}