package com.example.bank.accounts.expose.web;

import static com.example.bank.accounts.util.ResponseUtils.resolve;

import com.example.bank.accounts.business.DebitCardService;
import com.example.bank.accounts.expose.model.AccountMovement;
import com.example.bank.accounts.expose.model.DebitCard;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.expose.model.DebitCardRequest;
import com.example.bank.accounts.mappers.BankAccountMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated debit-card contract.
 */
@RestController
@RequiredArgsConstructor
public class DebitCardApiImpl implements DebitCardApi {

    private final DebitCardService debitCardService;
    private final BankAccountMapper accountMapper;

    /**
     * Creates a debit card linked to a bank account.
     */
    @Override
    public ResponseEntity<DebitCard> createDebitCard(@Valid DebitCardRequest debitCardRequest) {
        return resolve(() -> debitCardService.createDebitCard(debitCardRequest)
                .map(accountMapper::toApiDebitCard)
                .map(card -> ResponseEntity.status(HttpStatus.CREATED).body(card)));
    }

    /**
     * Pays with a debit card.
     */
    @Override
    public ResponseEntity<AccountMovement> payWithDebitCard(
            String id,
            @Valid DebitCardPaymentRequest debitCardPaymentRequest) {
        return resolve(() -> debitCardService.payWithDebitCard(id, debitCardPaymentRequest)
                .map(accountMapper::toApiMovement)
                .map(ResponseEntity::ok));
    }
}
