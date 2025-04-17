package com.acenexus.tata.gatewayservice.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String account;
    private String password;
}