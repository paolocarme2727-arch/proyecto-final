package com.example.bank.accounts.mappers;

import com.example.bank.accounts.expose.model.AccountMovement;
import com.example.bank.accounts.expose.model.BankAccount;
import com.example.bank.accounts.expose.model.DebitCard;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Maps account domain documents to OpenAPI response models.
 */
@Component
public class BankAccountMapper {

    /**
     * Converts a domain account into the generated API model.
     */
    public BankAccount toApiAccount(com.example.bank.accounts.domain.BankAccount account) {
        return new BankAccount()
                .id(account.getId())
                .customerId(account.getCustomerId())
                .customerType(com.example.bank.accounts.expose.model.CustomerType.fromValue(account.getCustomerTypeEnum().name()))
                .customerProfile(com.example.bank.accounts.expose.model.CustomerProfile.fromValue(
                        account.getCustomerProfileEnum() == null ? "REGULAR" : account.getCustomerProfileEnum().name()))
                .type(com.example.bank.accounts.expose.model.AccountType.fromValue(account.getType().name()))
                .balance(account.getBalance())
                .maintenanceFee(account.getMaintenanceFee())
                .minimumOpeningAmount(account.getMinimumOpeningAmount())
                .minimumDailyAverageAmount(account.getMinimumDailyAverageAmount())
                .monthlyMovementLimit(account.getMonthlyMovementLimit())
                .monthlyMovementCount(account.getMonthlyMovementCount())
                .freeTransactionLimit(account.getFreeTransactionLimit())
                .transactionFee(account.getTransactionFee())
                .chargedFees(account.getChargedFees())
                .movementDay(account.getMovementDay())
                .holders(account.getHolders())
                .authorizedSigners(account.getAuthorizedSigners())
                .createdAt(toOffsetDateTime(account.getCreatedAt()))
                .updatedAt(toOffsetDateTime(account.getUpdatedAt()));
    }

    /**
     * Converts a domain account movement into the generated API model.
     */
    public AccountMovement toApiMovement(com.example.bank.accounts.domain.AccountMovement movement) {
        return new AccountMovement()
                .id(movement.getId())
                .accountId(movement.getAccountId())
                .type(com.example.bank.accounts.expose.model.MovementType.fromValue(movement.getType().name()))
                .amount(movement.getAmount())
                .fee(movement.getFee())
                .resultingBalance(movement.getResultingBalance())
                .createdAt(toOffsetDateTime(movement.getCreatedAt()));
    }

    /**
     * Converts a domain debit card into the generated API model.
     */
    public DebitCard toApiDebitCard(com.example.bank.accounts.domain.DebitCard card) {
        return new DebitCard()
                .id(card.getId())
                .customerId(card.getCustomerId())
                .accountId(card.getAccountId())
                .cardNumber(card.getCardNumber())
                .active(card.isActive())
                .createdAt(toOffsetDateTime(card.getCreatedAt()))
                .updatedAt(toOffsetDateTime(card.getUpdatedAt()));
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}

