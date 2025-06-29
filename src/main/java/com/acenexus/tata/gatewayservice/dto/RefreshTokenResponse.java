package com.acenexus.tata.gatewayservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {
    private Long id;
    private String username;
    private String token;
    private String refreshToken;
}