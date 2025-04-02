package com.acenexus.tata.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class GatewayLoggerFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoggerFilter.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie", "jwt", "api-key");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = request.getId();
        LocalDateTime startTime = LocalDateTime.now();
        long startTimeMillis = System.currentTimeMillis();
        String clientIP = getClientIP(request);
        String method = request.getMethod().name();
        String path = request.getPath().value();

        // 記錄請求開始
        log.info("[REQUEST START] requestId={} | method={} | path={} | clientIP={} | timestamp={}", requestId, method, path, clientIP, formatter.format(startTime));

        // 排除敏感訊息
        request.getHeaders().forEach((name, values) -> {
            if (SENSITIVE_HEADERS.contains(name.toLowerCase())) {
                log.debug("[REQUEST HEADER] requestId={} | {}=[PROTECTED]", requestId, name);
            } else {
                values.forEach(value -> log.debug("[REQUEST HEADER] requestId={} | {}={}", requestId, name, value));
            }
        });

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long executionTime = System.currentTimeMillis() - startTimeMillis;
                    ServerHttpResponse response = exchange.getResponse();
                    response.getStatusCode();
                    HttpStatus statusCode = (HttpStatus) response.getStatusCode();

                    Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
                    String routeId = (route != null) ? route.getId() : "unknown";

                    URI targetUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
                    String target = (targetUri != null) ? targetUri.toString() : "unknown";

                    // 記錄回應日誌
                    if (statusCode.is5xxServerError()) {
                        log.error("[REQUEST END] requestId={} | method={} | path={} | status={} | route={} | target={} | duration={}ms", requestId, method, path, statusCode, routeId, target, executionTime);
                    } else if (statusCode.is4xxClientError()) {
                        log.warn("[REQUEST END] requestId={} | method={} | path={} | status={} | route={} | target={} | duration={}ms", requestId, method, path, statusCode, routeId, target, executionTime);
                    } else {
                        log.info("[REQUEST END] requestId={} | method={} | path={} | status={} | route={} | target={} | duration={}ms", requestId, method, path, statusCode, routeId, target, executionTime);
                    }

                    // 記錄慢請求
                    if (executionTime > 3000) { // 設定閾值，例如 3 秒
                        log.warn("[SLOW REQUEST] requestId={} | method={} | path={} | duration={}ms (over threshold)", requestId, method, path, executionTime);
                    }
                }));
    }

    /**
     * 獲取客戶端真實 IP 地址
     */
    private String getClientIP(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return (request.getRemoteAddress() != null) ? request.getRemoteAddress().getHostString() : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
