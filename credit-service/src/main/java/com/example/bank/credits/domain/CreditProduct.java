package com.example.bank.credits.domain;

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
 * Credit product document owned only by the credit service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_products")
@CompoundIndex(name = "idx_credit_customer_type", def = "{'customerId': 1, 'type': 1}")
@CompoundIndex(name = "idx_credit_customer_overdue", def = "{'customerId': 1, 'overdueDebt': 1}")
public class CreditProduct {

    @Id
    private String id;
    @Indexed
    private String customerId;
    private CustomerType customerType;
    @Indexed
    private CreditProductType type;
    private BigDecimal creditLimit;
    private BigDecimal usedAmount;
    private BigDecimal outstandingBalance;
    @Indexed
    private boolean overdueDebt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

