package com.example.bank.accounts.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event emitted by wallet-service after a Yanki payment request.
 */
public record WalletPaymentEvent(
        String sourceWalletId,
        String targetWalletId,
        String sourceDebitCardId,
        String targetDebitCardId,
        BigDecimal amount,
        LocalDateTime createdAt) {
}

