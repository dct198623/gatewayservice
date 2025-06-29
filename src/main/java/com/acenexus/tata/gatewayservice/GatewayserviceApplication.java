package com.acenexus.tata.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GatewayserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayserviceApplication.class, args);
    }

}
