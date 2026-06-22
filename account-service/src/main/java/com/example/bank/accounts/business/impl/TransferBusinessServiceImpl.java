package com.example.bank.accounts.business.impl;

import com.example.bank.accounts.business.TransferService;
import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.policy.MovementRules;
import com.example.bank.accounts.expose.model.TransferRequest;
import com.example.bank.accounts.expose.model.TransferResponse;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles transfers between accounts in the bank.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferBusinessServiceImpl implements TransferService {

    private final BankAccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final MovementRules movementRules;
    private final AccountDomainService accountDomainService;

    /**
     * Transfers money to another account in the bank and stores both sides of the movement.
     */
    public Single<TransferResponse> transfer(TransferRequest request) {
        return validateTransferRequest(request)
                .andThen(Single.fromCallable(() -> executeTransfer(
                        accountDomainService.findExistingAccount(request.getSourceAccountId()),
                        accountDomainService.findExistingAccount(request.getTargetAccountId()),
                        request)))
                .subscribeOn(Schedulers.io())
                .doOnSuccess(response -> log.info("Transferidos {} desde la cuenta {} hacia la cuenta {}",
                        response.getAmount(), response.getSourceAccountId(), response.getTargetAccountId()));
    }

    private TransferResponse executeTransfer(BankAccount source, BankAccount target, TransferRequest request) {
        if (source.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las cuentas de origen y destino deben ser distintas");
        }
        movementRules.validateMovement(source, request.getAmount(), MovementTypeEnum.TRANSFER_OUT);
        movementRules.validateMovement(target, request.getAmount(), MovementTypeEnum.TRANSFER_IN);
        BigDecimal sourceFee = movementRules.calculateTransactionFee(source);
        BigDecimal targetFee = movementRules.calculateTransactionFee(target);
        applyTransferMovement(source, request.getAmount(), MovementTypeEnum.TRANSFER_OUT, sourceFee);
        applyTransferMovement(target, request.getAmount(), MovementTypeEnum.TRANSFER_IN, targetFee);
        return saveTransferMovements(accountRepository.save(source), accountRepository.save(target), request, sourceFee, targetFee);
    }

    private void applyTransferMovement(
            BankAccount account,
            BigDecimal amount,
            MovementTypeEnum movementTypeEnum,
            BigDecimal fee) {
        account.setBalance(movementRules.calculateResultingBalance(
                account,
                amount,
                movementTypeEnum,
                fee));
        account.setMonthlyMovementCount(account.getMonthlyMovementCount() + 1);
        account.setChargedFees((account.getChargedFees() == null ? BigDecimal.ZERO : account.getChargedFees()).add(fee));
        account.setUpdatedAt(LocalDateTime.now());
    }

    private TransferResponse saveTransferMovements(
            BankAccount source,
            BankAccount target,
            TransferRequest request,
            BigDecimal sourceFee,
            BigDecimal targetFee) {
        LocalDateTime now = LocalDateTime.now();
        AccountMovement sourceMovement = movementRepository.save(toTransferMovement(
                source,
                request.getAmount(),
                MovementTypeEnum.TRANSFER_OUT,
                sourceFee,
                now));
        AccountMovement targetMovement = movementRepository.save(toTransferMovement(
                target,
                request.getAmount(),
                MovementTypeEnum.TRANSFER_IN,
                targetFee,
                now));
        return new TransferResponse()
                .sourceAccountId(source.getId())
                .targetAccountId(target.getId())
                .amount(request.getAmount())
                .sourceMovementId(sourceMovement.getId())
                .targetMovementId(targetMovement.getId())
                .sourceBalance(source.getBalance())
                .targetBalance(target.getBalance())
                .createdAt(now.atOffset(ZoneOffset.UTC));
    }

    private AccountMovement toTransferMovement(
            BankAccount account,
            BigDecimal amount,
            MovementTypeEnum movementTypeEnum,
            BigDecimal fee,
            LocalDateTime createdAt) {
        return AccountMovement.builder()
                .accountId(account.getId())
                .type(movementTypeEnum)
                .amount(amount)
                .fee(fee)
                .resultingBalance(account.getBalance())
                .createdAt(createdAt)
                .build();
    }

    private Completable validateTransferRequest(TransferRequest request) {
        return Completable.fromAction(() -> {
            if (request.getSourceAccountId().equals(request.getTargetAccountId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las cuentas de origen y destino deben ser distintas");
            }
        });
    }
}
