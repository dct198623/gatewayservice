package com.acenexus.tata.gatewayservice.controller;

import com.acenexus.tata.gatewayservice.client.AccountServiceClient;
import com.acenexus.tata.gatewayservice.dto.AccountLoginRequest;
import com.acenexus.tata.gatewayservice.dto.AccountLoginResponse;
import com.acenexus.tata.gatewayservice.dto.LoginRequest;
import com.acenexus.tata.gatewayservice.dto.LoginResponse;
import com.acenexus.tata.gatewayservice.dto.RefreshTokenRequest;
import com.acenexus.tata.gatewayservice.dto.RefreshTokenResponse;
import com.acenexus.tata.gatewayservice.provider.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Authentication API")
@RestController
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Operation(summary = "User login", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    })
    @PostMapping("/v1/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        String account = request.getAccount();
        String password = request.getPassword();

        if (account == null || password == null || account.isBlank() || password.isBlank()) {
            log.warn("Login failed: missing account or password");
            return ResponseEntity.badRequest().body("Account and password are required");
        }

        try {
            AccountLoginRequest accountLoginRequest = new AccountLoginRequest(account, password);

            // 呼叫 Account 服務進行驗證
            AccountLoginResponse accountLoginResponse = accountServiceClient.login(accountLoginRequest);

            if (accountLoginResponse == null || accountLoginResponse.getId() == null) {
                log.warn("Login failed: invalid response from account service for account '{}'", account);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            String userName = accountLoginResponse.getName() != null ? accountLoginResponse.getName() : account;

            String accessToken = jwtTokenProvider.generateAccessToken(accountLoginResponse.getId(), userName);
            String refreshToken = jwtTokenProvider.generateRefreshToken(accountLoginResponse.getId(), userName);

            LoginResponse data = new LoginResponse(accountLoginResponse.getId(), userName, accessToken, refreshToken);

            log.info("Login successful for account '{}', userId: {}", account, accountLoginResponse.getId());
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("Login error for account '{}': {}", account, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing your login");
        }
    }

    @Operation(summary = "Refresh access token", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class)))
    }, security = @SecurityRequirement(name = "Authorization"))
    @PostMapping("/v1/refresh/token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Token refresh failed: missing refreshToken");
            return ResponseEntity.badRequest().body("Refresh token is required");
        }

        try {
            String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken);
            Long userId = jwtTokenProvider.extractUserId(refreshToken);
            String userName = jwtTokenProvider.extractUserName(refreshToken);

            RefreshTokenResponse data = new RefreshTokenResponse(userId, userName, newAccessToken, refreshToken);
            return ResponseEntity.ok(data);
        } catch (RuntimeException e) {
            log.error("Token refresh error for refreshToken '{}': {}", refreshToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error refreshing token: " + e.getMessage());
        }
    }

}