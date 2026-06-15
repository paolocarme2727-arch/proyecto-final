package com.example.bank.accounts.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Bank account document owned only by the account service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bank_accounts")
@CompoundIndex(name = "idx_account_customer_type", def = "{'customerId': 1, 'type': 1}")
public class BankAccount {

    @Id
    private String id;
    @Indexed
    private String customerId;
    private CustomerType customerType;
    private CustomerProfile customerProfile;
    @Indexed
    private AccountType type;
    private BigDecimal balance;
    private BigDecimal maintenanceFee;
    private BigDecimal minimumOpeningAmount;
    private BigDecimal minimumDailyAverageAmount;
    private Integer monthlyMovementLimit;
    private int monthlyMovementCount;
    private int freeTransactionLimit;
    private BigDecimal transactionFee;
    private BigDecimal chargedFees;
    private Integer movementDay;
    private List<String> holders;
    private List<String> authorizedSigners;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

