package com.acenexus.tata.gatewayservice.controller;

import com.acenexus.tata.gatewayservice.define.ApiResponse;
import com.acenexus.tata.gatewayservice.dto.RefreshTokenRequest;
import com.acenexus.tata.gatewayservice.provider.JwtTokenProvider;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> body) {
        String userId = "1";
        String userName = body.get("username");

        if (userName != null && !userName.trim().isEmpty()) {
            String accessToken = jwtTokenProvider.generateAccessToken(userId, userName);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userId, userName);

            Map<String, Object> data = new HashMap<>();
            data.put("id", userId);
            data.put("token", accessToken);
            data.put("refreshToken", refreshToken);

            ApiResponse<Map<String, Object>> response = new ApiResponse<>(0, data);
            log.info("User logged in: {}", userName);
            return ResponseEntity.ok(response);
        } else {
            log.warn("Login failed: username is null or empty");
            ApiResponse<Map<String, Object>> error = new ApiResponse<>(1, "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> testEndpoint() {
        ApiResponse<String> response = new ApiResponse<>(0, "Hello!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh/token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            ApiResponse<Map<String, Object>> errorResponse = new ApiResponse<>(1, "Invalid refresh token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            String newAccessToken = JwtTokenProvider.refreshAccessToken(refreshToken);

            String userId = JwtTokenProvider.extractUserId(refreshToken);
            String userName = JwtTokenProvider.extractUserName(refreshToken);

            Map<String, Object> data = new HashMap<>();
            data.put("id", userId);
            data.put("username", userName);
            data.put("token", newAccessToken);
            data.put("refreshToken", refreshToken);

            ApiResponse<Map<String, Object>> response = new ApiResponse<>(0, data);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<Map<String, Object>> errorResponse = new ApiResponse<>(1, "Error refreshing token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

}