package com.example.bank.accounts.events;

/**
 * Event consumed from Kafka to cache customer credit debt status.
 */
public record CreditDebtStatusEvent(String customerId, boolean hasOverdueDebt) {
}

