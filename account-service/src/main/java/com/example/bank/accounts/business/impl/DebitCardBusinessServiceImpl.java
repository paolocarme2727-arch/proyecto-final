package com.example.bank.accounts.business.impl;

import com.example.bank.accounts.business.DebitCardService;
import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.domain.policy.MovementRules;
import com.example.bank.accounts.events.DebitCardEventPublisher;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.expose.model.DebitCardRequest;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.repository.DebitCardRepository;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles debit-card creation and debit-card-based payments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebitCardBusinessServiceImpl implements DebitCardService {

    private final BankAccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final DebitCardRepository debitCardRepository;
    private final DebitCardEventPublisher debitCardEventPublisher;
    private final MovementRules movementRules;
    private final AccountDomainService accountDomainService;

    /**
     * Creates a debit card linked to an existing account.
     */
    public Single<DebitCard> createDebitCard(DebitCardRequest request) {
        return Single.fromCallable(() -> {
                    BankAccount account = accountDomainService.findExistingAccount(request.getAccountId());
                    if (!account.getCustomerId().equals(request.getCustomerId())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "La cuenta no pertenece al cliente");
                    }
                    DebitCard card = debitCardRepository.save(toNewDebitCard(request));
                    debitCardEventPublisher.registerCreated(card);
                    return card;
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(card -> log.info("Tarjeta de débito creada {}", card.getId()));
    }

    /**
     * Pays with a debit card by charging its linked account.
     */
    public Single<AccountMovement> payWithDebitCard(String id, DebitCardPaymentRequest request) {
        return findExistingDebitCard(id)
                .flatMap(card -> Single.fromCallable(() -> registerAccountMovement(
                        card.getAccountId(),
                        request.getAmount(),
                        MovementTypeEnum.DEBIT_CARD_PAYMENT)))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(movement -> log.info("Pago con tarjeta de débito registrado con la tarjeta {}", id));
    }

    /**
     * Registers wallet movements against the debit-card linked account.
     */
    public Single<AccountMovement> registerWalletDebitCardMovement(
            String cardId,
            BigDecimal amount,
            MovementTypeEnum movementTypeEnum) {
        return findExistingDebitCard(cardId)
                .flatMap(card -> Single.fromCallable(() -> registerAccountMovement(
                        card.getAccountId(),
                        amount,
                        movementTypeEnum)));
    }

    private AccountMovement registerAccountMovement(String accountId, BigDecimal amount, MovementTypeEnum movementTypeEnum) {
        BankAccount account = accountDomainService.findExistingAccount(accountId);
        movementRules.validateMovement(account, amount, movementTypeEnum);
        BigDecimal fee = movementRules.calculateTransactionFee(account);
        BankAccount saved = accountRepository.save(applyAccountMovement(account, amount, movementTypeEnum, fee));
        return movementRepository.save(toAccountMovement(saved, amount, movementTypeEnum, fee));
    }

    private DebitCard toNewDebitCard(DebitCardRequest request) {
        return DebitCard.builder()
                .customerId(request.getCustomerId())
                .accountId(request.getAccountId())
                .cardNumber(request.getCardNumber())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private BankAccount applyAccountMovement(
            BankAccount account,
            BigDecimal amount,
            MovementTypeEnum movementTypeEnum,
            BigDecimal fee) {
        account.setBalance(movementRules.calculateResultingBalance(account, amount, movementTypeEnum, fee));
        account.setMonthlyMovementCount(account.getMonthlyMovementCount() + 1);
        account.setChargedFees((account.getChargedFees() == null ? BigDecimal.ZERO : account.getChargedFees()).add(fee));
        account.setUpdatedAt(LocalDateTime.now());
        return account;
    }

    private AccountMovement toAccountMovement(
            BankAccount account,
            BigDecimal amount,
            MovementTypeEnum movementTypeEnum,
            BigDecimal fee) {
        return AccountMovement.builder()
                .accountId(account.getId())
                .type(movementTypeEnum)
                .amount(amount)
                .fee(fee)
                .resultingBalance(account.getBalance())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Single<DebitCard> findExistingDebitCard(String id) {
        return Single.fromCallable(() -> {
                    DebitCard card = debitCardRepository.findById(id)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Tarjeta de débito no encontrada"));
                    if (!card.isActive()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La tarjeta de débito está inactiva");
                    }
                    return card;
                })
                .subscribeOn(Schedulers.io());
    }
}
