package com.acenexus.tata.gatewayservice.filter;

import com.acenexus.tata.gatewayservice.define.ApiResponse;
import com.acenexus.tata.gatewayservice.provider.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String UNAUTHORIZED_MSG = "Unauthorized";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/gateway/login",
            "/api/gateway/refresh/token"
    );

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isExcludedPath(path)) {
            log.debug("Skipping JWT validation for path: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Invalid Authorization header for path: {}", path);
            return onError(exchange, UNAUTHORIZED_MSG, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("JWT validation failed for path: {}", path);
                return onError(exchange, UNAUTHORIZED_MSG, HttpStatus.UNAUTHORIZED);
            }

            Claims claims = jwtTokenProvider.extractAllClaims(token);

            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-ID", claims.getSubject())
                    .header("X-User-Name", claims.get("userName", String.class))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("JWT processing error for path: {}, reason: {}", path, e.getMessage());
            return onError(exchange, UNAUTHORIZED_MSG, HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private static <T> Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ApiResponse<T> response = ApiResponse.error(message);

        byte[] responseBytes = getResponseBytes(response);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(responseBytes)));
    }

    private static <T> byte[] getResponseBytes(ApiResponse<T> response) {
        try {
            return objectMapper.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            return "{\"status\":1,\"message\":\"Server Error\"}".getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}