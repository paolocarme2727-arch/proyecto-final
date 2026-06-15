package com.example.bank.accounts.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External account rules loaded from Config Server or application properties.
 */
@ConfigurationProperties(prefix = "banking.accounts")
public record AccountProperties(
        int savingsMonthlyMovementLimit,
        BigDecimal checkingMaintenanceFee,
        BigDecimal minimumOpeningAmount,
        int freeTransactionLimit,
        BigDecimal transactionFee,
        BigDecimal vipMonthlyMinimumAverageAmount
) {
}

