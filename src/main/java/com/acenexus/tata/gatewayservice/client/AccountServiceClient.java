package com.acenexus.tata.gatewayservice.client;

import com.acenexus.tata.gatewayservice.dto.AccountLoginRequest;
import com.acenexus.tata.gatewayservice.dto.AccountLoginResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Account 微服務的 Feign 客戶端
 */
@FeignClient(name = "accountservice", path = "/v1/user")
public interface AccountServiceClient {
    @PostMapping("/login")
    AccountLoginResponse login(@RequestBody AccountLoginRequest accountLoginRequest);
} 