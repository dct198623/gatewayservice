package com.acenexus.tata.gatewayservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 登入驗證流程
 * 1. 登入時，後端驗證帳密後，發給使用者兩個 Token
 * - Access Token（短效期，如 15 分鐘）：用來驗證 API 請求的身份
 * - Refresh Token（長效期，如 7 天）：用來換取新的 Access Token
 * 2. 使用 API 時，前端帶著 Access Token 發送請求，後端驗證它是否合法
 * 3. 當 Access Token 過期時，前端用 Refresh Token 請求新的 Access Token，讓用戶不用重新登入
 * 4. 如果 Refresh Token 也過期，則要求用戶重新登入
 */
@Component
public class JwtTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final Key signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 days

    private static String generateAccessToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userName", username);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private static String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private static Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    private static String extractUserName(String token) {
        return extractAllClaims(token).get("userName", String.class);
    }

    public static void main(String[] args) throws InterruptedException {
        // 1. 登入時，後端驗證帳密後，發給使用者兩個 Token
        String userId = "1001";
        String userName = "user1";

        // Access Token（短效期，如 15 分鐘）：用來驗證 API 請求的身份
        String accessToken = generateAccessToken(userId, userName);
        log.info("Access Token: " + accessToken);

        // Refresh Token（長效期，如 7 天）：用來換取新的 Access Token
        String refreshToken = generateRefreshToken(userId);
        log.info("Refresh Token: " + refreshToken);

        // 2. 使用 API 時，前端帶著 Access Token 發送請求，後端驗證它是否合法
        log.info("Validate Access Token: " + validateToken(accessToken));
        log.info("User ID: " + extractUserId(accessToken));
        log.info("Username: " + extractUserName(accessToken));

        // 3. 當 Access Token 過期時，前端用 Refresh Token 請求新的 Access Token，讓用戶不用重新登入
        Thread.sleep(ACCESS_TOKEN_EXPIRATION + 1000);

        // Now, validate again
        boolean isValidAccessToken = validateToken(accessToken);
        if (!isValidAccessToken) {
            log.info("Access Token has expired. Generating a new one using Refresh Token.");
            accessToken = generateAccessToken(userId, userName);
            log.info("New Access Token: " + accessToken);
        } else {
            log.info("Access Token is still valid.");
        }
    }

}