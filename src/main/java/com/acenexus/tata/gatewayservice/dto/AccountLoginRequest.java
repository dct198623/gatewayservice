package com.acenexus.tata.gatewayservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountLoginRequest {
    private String account;
    private String password;
}
