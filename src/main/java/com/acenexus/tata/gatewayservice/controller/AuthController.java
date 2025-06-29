package com.acenexus.tata.gatewayservice.controller;

import com.acenexus.tata.gatewayservice.dto.LoginRequest;
import com.acenexus.tata.gatewayservice.dto.LoginResponse;
import com.acenexus.tata.gatewayservice.dto.RefreshTokenRequest;
import com.acenexus.tata.gatewayservice.provider.JwtTokenProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/v1/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        String account = request.getAccount();
        String password = request.getPassword();

        if (account == null || password == null || account.isBlank() || password.isBlank()) {
            log.warn("Login failed: missing account or password");
            return ResponseEntity.badRequest().body("Account and password are required");
        }

        try {
            // TODO 模擬登入
            if (!"ace".equals(account) || !"123".equals(password)) {
                log.warn("Login failed for account '{}': Invalid credentials", account);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            String userId = "1";
            String userName = "user001";
            String accessToken = jwtTokenProvider.generateAccessToken(userId, userName);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userId, userName);
            LoginResponse data = new LoginResponse(userId, userName, accessToken, refreshToken);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Login error for account '{}': {}", account, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing your login");
        }
    }

    @PostMapping("/v1/refresh/token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Token refresh failed: missing refreshToken");
            return ResponseEntity.badRequest().body("Refresh token is required");
        }

        try {
            String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken);
            String userId = jwtTokenProvider.extractUserId(refreshToken);
            String userName = jwtTokenProvider.extractUserName(refreshToken);

            Map<String, Object> data = new HashMap<>();
            data.put("id", userId);
            data.put("username", userName);
            data.put("token", newAccessToken);
            data.put("refreshToken", refreshToken);

            return ResponseEntity.ok(data);
        } catch (RuntimeException e) {
            log.error("Token refresh error for refreshToken '{}': {}", refreshToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error refreshing token: " + e.getMessage());
        }
    }

}