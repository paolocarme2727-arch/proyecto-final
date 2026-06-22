package com.example.bank.accounts;

import com.example.bank.accounts.config.AccountProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Starts the bank account microservice.
 */
@SpringBootApplication
@EnableConfigurationProperties(AccountProperties.class)
@EnableScheduling
public class AccountServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}

