package com.example.bank.accounts.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Movement document for deposits and withdrawals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "account_movements")
@CompoundIndex(name = "idx_account_movement_account_created", def = "{'accountId': 1, 'createdAt': -1}")
public class AccountMovement {

    @Id
    private String id;
    @Indexed
    private String accountId;
    private MovementType type;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal resultingBalance;
    @Indexed
    private LocalDateTime createdAt;
}

