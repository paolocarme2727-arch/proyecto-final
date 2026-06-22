package com.example.bank.credits.domain;

import com.example.bank.credits.util.enums.CreditMovementType;
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
 * Movement document for credit payments, charges and disbursements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_movements")
@CompoundIndex(name = "idx_credit_movement_product_created", def = "{'creditProductId': 1, 'createdAt': -1}")
public class CreditMovement {

    @Id
    private String id;
    @Indexed
    private String creditProductId;
    private CreditMovementType type;
    private BigDecimal amount;
    private BigDecimal resultingDebt;
    private BigDecimal availableCredit;
    @Indexed
    private LocalDateTime createdAt;
}

