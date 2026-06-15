package com.example.bank.wallets.mappers;

import com.example.bank.wallets.expose.model.Wallet;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Maps wallet domain documents to OpenAPI response models.
 */
@Component
public class WalletMapper {

    /**
     * Converts a domain wallet into the generated API model.
     */
    public Wallet toApiWallet(com.example.bank.wallets.domain.Wallet wallet) {
        return new Wallet()
                .id(wallet.getId())
                .documentType(com.example.bank.wallets.expose.model.DocumentType.fromValue(wallet.getDocumentType().name()))
                .documentNumber(wallet.getDocumentNumber())
                .phoneNumber(wallet.getPhoneNumber())
                .imei(wallet.getImei())
                .email(wallet.getEmail())
                .debitCardId(wallet.getDebitCardId())
                .balance(wallet.getBalance())
                .createdAt(toOffsetDateTime(wallet.getCreatedAt()))
                .updatedAt(toOffsetDateTime(wallet.getUpdatedAt()));
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}

