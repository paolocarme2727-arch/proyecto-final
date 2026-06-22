package com.example.bank.accounts.util.enums;

/**
 * Movements supported by bank accounts.
 */
public enum MovementTypeEnum {
    DEPOSIT,
    WITHDRAWAL, //retiro
    TRANSFER_IN,
    TRANSFER_OUT,
    DEBIT_CARD_PAYMENT,
    WALLET_PAYMENT_IN, //pago con billetera digital
    WALLET_PAYMENT_OUT
}

