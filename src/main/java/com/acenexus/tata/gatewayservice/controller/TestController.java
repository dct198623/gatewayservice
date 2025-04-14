package com.acenexus.tata.gatewayservice.controller;

import com.acenexus.tata.gatewayservice.provider.JwtTokenProvider;
import com.acenexus.tata.gatewayservice.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {
    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody Map<String, String> body) {
        String userId = "1001";
        String userName = body.get("username");

        if (userName != null) {
            Map<String, Object> tokens = new HashMap<>();
            String accessToken = jwtTokenProvider.generateAccessToken(userId, userName);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userId, userName);

            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            tokens.put("userId", userId);
            tokens.put("userName", userName);

            log.info("User logged in: {}", userName);
            return Mono.just(ResponseEntity.ok(tokens));
        } else {
            log.warn("Login failed for username: {}", userName);
            return ResponseUtil.onErrorResponse("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/test")
    public String testEndpoint() {
        return "Hello!";
    }

}