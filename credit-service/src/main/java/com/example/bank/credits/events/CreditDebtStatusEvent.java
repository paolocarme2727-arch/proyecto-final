package com.example.bank.credits.events;

/**
 * Event published when a customer's overdue debt status changes.
 */
public record CreditDebtStatusEvent(String customerId, boolean hasOverdueDebt) {
}

