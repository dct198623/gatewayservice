package com.acenexus.tata.gatewayservice.filter;

import com.acenexus.tata.gatewayservice.provider.JwtTokenProvider;
import com.acenexus.tata.gatewayservice.util.ResponseUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    // 不需要驗證 JWT 路徑的列表
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/gateway/login"
    );

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 檢查是否不需要驗證 JWT
        if (isExcludedPath(path)) {
            log.debug("Path excluded from JWT verification: {}", path);
            return chain.filter(exchange);
        }

        // 取得 Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header. Path: {}", path);
            return ResponseUtil.onError(exchange, "Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7); // 去掉 Bearer 前綴

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid JWT token. Path: {}", path);
                return ResponseUtil.onError(exchange, "Unauthorized", HttpStatus.UNAUTHORIZED);
            }

            Claims claims = JwtTokenProvider.extractAllClaims(token);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-ID", claims.getSubject())
                    .header("X-User-Name", claims.get("userName", String.class))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("JWT validation failed. Path: {}, Error: {}", path, e.getMessage());
            return ResponseUtil.onError(exchange, "Unauthorized", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
