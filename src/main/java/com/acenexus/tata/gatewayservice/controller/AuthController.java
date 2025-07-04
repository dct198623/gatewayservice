package com.acenexus.tata.gatewayservice.controller;

import com.acenexus.tata.gatewayservice.dto.AccountLoginRequest;
import com.acenexus.tata.gatewayservice.dto.LoginRequest;
import com.acenexus.tata.gatewayservice.dto.LoginResponse;
import com.acenexus.tata.gatewayservice.dto.RefreshTokenRequest;
import com.acenexus.tata.gatewayservice.dto.RefreshTokenResponse;
import com.acenexus.tata.gatewayservice.provider.JwtTokenProvider;
import com.acenexus.tata.gatewayservice.service.AccountService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Authentication", description = "Authentication API")
@RestController
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AccountService accountService;

    @Operation(summary = "User login", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    @PostMapping("/v1/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        String account = request.getAccount();
        String password = request.getPassword();

        if (account == null || password == null || account.isBlank() || password.isBlank()) {
            log.warn("Login failed: missing account or password");
            return Mono.just(ResponseEntity.badRequest().build());
        }

        AccountLoginRequest accountLoginRequest = new AccountLoginRequest(account, password);
        return accountService.login(accountLoginRequest);
    }

    @Operation(summary = "Refresh access token", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class)))
    }, security = @SecurityRequirement(name = "Authorization"))
    @PostMapping("/v1/refresh/token")
    public Mono<ResponseEntity<RefreshTokenResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            log.warn("Token refresh failed: missing refreshToken");
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return accountService.refreshToken(request.getRefreshToken());
    }
}