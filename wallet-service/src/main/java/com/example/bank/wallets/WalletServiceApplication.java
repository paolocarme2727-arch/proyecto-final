package com.example.bank.wallets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Yanki wallet microservice.
 */
@SpringBootApplication
public class WalletServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}

