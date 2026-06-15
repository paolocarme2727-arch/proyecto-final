package com.example.bank.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures HTTP clients for gateway proxying.
 */
@Configuration
public class GatewayRestClientConfig {

    /**
     * Creates a RestClient builder.
     *
     * @return RestClient builder
     */
    @Bean
    public RestClient.Builder gatewayRestClientBuilder() {
        return RestClient.builder();
    }
}
