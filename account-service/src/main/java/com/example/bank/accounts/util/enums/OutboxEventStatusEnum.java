package com.example.bank.accounts.util.enums;

/**
 * Outbox event delivery status.
 */
public enum OutboxEventStatusEnum {
    PENDING,
    PUBLISHED,
    FAILED
}
