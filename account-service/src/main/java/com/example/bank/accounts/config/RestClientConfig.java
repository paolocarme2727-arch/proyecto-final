package com.example.bank.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures HTTP clients for external service calls.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a RestClient builder for inter-service calls.
     *
     * @return RestClient builder
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}

