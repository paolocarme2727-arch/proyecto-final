package com.example.bank.accounts.business;

import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.expose.model.DebitCardRequest;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;

/**
 * Debit-card use cases.
 */
public interface DebitCardService {

    /**
     * Creates a debit card associated with an account.
     */
    Single<DebitCard> createDebitCard(DebitCardRequest request);

    /**
     * Pays with a debit card using the linked account balance.
     */
    Single<AccountMovement> payWithDebitCard(String id, DebitCardPaymentRequest request);

    /**
     * Applies wallet debit-card movements to linked accounts.
     */
    Single<AccountMovement> registerWalletDebitCardMovement(String cardId, BigDecimal amount, MovementTypeEnum movementTypeEnum);
}
