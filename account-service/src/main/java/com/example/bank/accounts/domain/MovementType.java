package com.example.bank.accounts.domain;

/**
 * Movements supported by bank accounts.
 */
public enum MovementType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT,
    DEBIT_CARD_PAYMENT,
    WALLET_PAYMENT_IN,
    WALLET_PAYMENT_OUT
}

