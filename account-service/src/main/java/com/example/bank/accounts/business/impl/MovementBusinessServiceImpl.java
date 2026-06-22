package com.example.bank.accounts.business.impl;

import com.example.bank.accounts.business.MovementService;
import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.policy.MovementRules;
import com.example.bank.accounts.expose.model.MoneyRequest;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles account deposits, withdrawals and movement registration rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovementBusinessServiceImpl implements MovementService {

    private final BankAccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final MovementRules movementRules;
    private final AccountDomainService accountDomainService;

    /**
     * Registers a deposit movement.
     */
    public Single<AccountMovement> deposit(String id, MoneyRequest request) {
        return registerMovement(id, request.getAmount(), MovementTypeEnum.DEPOSIT);
    }

    /**
     * Registers a withdrawal movement.
     */
    public Single<AccountMovement> withdraw(String id, MoneyRequest request) {
        return registerMovement(id, request.getAmount(), MovementTypeEnum.WITHDRAWAL);
    }

    /**
     * Returns every movement for one account.
     */
    public Flowable<AccountMovement> findMovements(String id) {
        return Single.fromCallable(() -> {
                    BankAccount account = accountDomainService.findExistingAccount(id);
                    return movementRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
                })
                .flattenAsFlowable(movements -> movements)
                .subscribeOn(Schedulers.io());
    }

    private Single<AccountMovement> registerMovement(String id, BigDecimal amount, MovementTypeEnum movementTypeEnum) {
        return Single.fromCallable(() -> {
                    BankAccount account = accountDomainService.findExistingAccount(id);
                    movementRules.validateMovement(account, amount, movementTypeEnum);
                    return saveMovement(account, amount, movementTypeEnum);
                })
                .subscribeOn(Schedulers.io())
                .doOnSuccess(movement -> log.info("Movimiento {} registrado en la cuenta {}", movement.getType(), id));
    }

    private AccountMovement saveMovement(BankAccount account, BigDecimal amount, MovementTypeEnum movementTypeEnum) {
        BigDecimal fee = movementRules.calculateTransactionFee(account);
        BankAccount saved = accountRepository.save(applyAccountMovement(account, amount, movementTypeEnum, fee));
        return movementRepository.save(toAccountMovement(saved, amount, movementTypeEnum, fee));
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
}
