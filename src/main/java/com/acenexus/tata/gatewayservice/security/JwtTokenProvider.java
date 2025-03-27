package com.acenexus.tata.gatewayservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private Key signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    public String generateToken(String userId, String[] roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        return createToken(claims, userId);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long EXPIRATION_TIME = 3600000; // 1 hour
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String refreshToken(String token) {
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Invalid token. Cannot refresh.");
        }
        String userId = extractUserId(token);
        String[] roles = extractRoles(token);
        return generateToken(userId, roles);
    }

    public boolean validateToken(String token) {
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

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String[] extractRoles(String token) {
        List<String> rolesList = extractClaim(token, claims -> claims.get("roles", List.class));
        return rolesList.toArray(new String[0]);
    }

    public static void main(String[] args) {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();
        jwtTokenProvider.init(); // Ensure signingKey is initialized

        // User ID
        String userId = "1";

        // User roles
        String[] roles = {"ADMIN"};

        // Generate Token
        String token = jwtTokenProvider.generateToken(userId, roles);
        log.info("Generated Token: " + token);

        // Validate Token
        boolean isValid = jwtTokenProvider.validateToken(token);
        log.info("Token Valid: " + isValid);

        // Extract User ID
        String extractedUserId = jwtTokenProvider.extractUserId(token);
        log.info("Extracted User ID: " + extractedUserId);

        // Extract Roles
        String[] extractedRoles = jwtTokenProvider.extractRoles(token);
        log.info("Extracted Roles: " + Arrays.toString(extractedRoles));

        // Extract Expiration Time
        Date expirationDate = jwtTokenProvider.extractExpiration(token);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("Token Expiration Time: " + sdf.format(expirationDate));

        // Refresh Token
        String refreshedToken = jwtTokenProvider.refreshToken(token);
        log.info("Refreshed Token: " + refreshedToken);
    }
}