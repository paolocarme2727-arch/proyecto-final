package com.example.bank.credits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the credit product microservice.
 */
@SpringBootApplication
public class CreditServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CreditServiceApplication.class, args);
    }
}

