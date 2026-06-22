package com.example.bank.wallets.domain;

import com.example.bank.wallets.util.enums.DocumentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Yanki wallet document owned only by wallet-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wallets")
public class Wallet {

    @Id
    private String id;
    private DocumentType documentType;
    private String documentNumber;
    @Indexed(unique = true)
    private String phoneNumber;
    private String imei;
    private String email;
    @Indexed
    private String debitCardId;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

