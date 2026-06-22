package com.example.bank.accounts.business.impl;

import com.example.bank.accounts.business.ReportService;
import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.expose.model.AccountProductReportResponse;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Handles account reports and recent movement queries.
 */
@Service
@RequiredArgsConstructor
public class ReportBusinessServiceImpl implements ReportService {

    private final BankAccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final AccountDomainService accountDomainService;

    /**
     * Generates a complete account report by product type and time interval.
     */
    public Single<AccountProductReportResponse> getProductReport(
            com.example.bank.accounts.expose.model.AccountType type,
            OffsetDateTime from,
            OffsetDateTime to) {
        return Single.fromCallable(() -> {
                    AccountTypeEnum accountTypeEnum = AccountTypeEnum.valueOf(type.getValue());
                    List<BankAccount> accounts = accountRepository.findByType(accountTypeEnum);
                    List<String> accountIds = accounts.stream().map(BankAccount::getId).toList();
                    List<AccountMovement> movements = accountIds.isEmpty()
                            ? List.of()
                            : movementRepository.findByAccountIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
                                    accountIds,
                                    from.toLocalDateTime(),
                                    to.toLocalDateTime());
                    return toAccountReport(type, from, to, accounts.size(), movements);
                })
                .subscribeOn(Schedulers.io());
    }

    /**
     * Lists the latest debit-card-style movements for a bank account.
     */
    public Flowable<AccountMovement> findRecentDebitCardMovements(String id) {
        return Single.fromCallable(() -> {
                    BankAccount account = accountDomainService.findExistingAccount(id);
                    return movementRepository.findByAccountIdAndTypeOrderByCreatedAtDesc(
                            account.getId(),
                            MovementTypeEnum.DEBIT_CARD_PAYMENT,
                            PageRequest.of(0, 10));
                })
                .flattenAsFlowable(movements -> movements)
                .subscribeOn(Schedulers.io());
    }

    private AccountProductReportResponse toAccountReport(
            com.example.bank.accounts.expose.model.AccountType type,
            OffsetDateTime from,
            OffsetDateTime to,
            int productCount,
            List<AccountMovement> movements) {
        BigDecimal totalAmount = movements.stream()
                .map(AccountMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = movements.stream()
                .map(movement -> movement.getFee() == null ? BigDecimal.ZERO : movement.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<String> accountIds = movements.stream().map(AccountMovement::getAccountId).collect(java.util.stream.Collectors.toSet());
        return new AccountProductReportResponse()
                .productType(type)
                .from(from)
                .to(to)
                .productCount(productCount == 0 ? accountIds.size() : productCount)
                .movementCount(movements.size())
                .totalAmount(totalAmount)
                .totalFees(totalFees)
                .movements(movements.stream().map(this::toApiMovement).toList());
    }

    private com.example.bank.accounts.expose.model.AccountMovement toApiMovement(AccountMovement movement) {
        return new com.example.bank.accounts.expose.model.AccountMovement()
                .id(movement.getId())
                .accountId(movement.getAccountId())
                .type(com.example.bank.accounts.expose.model.MovementType.fromValue(movement.getType().name()))
                .amount(movement.getAmount())
                .fee(movement.getFee())
                .resultingBalance(movement.getResultingBalance())
                .createdAt(movement.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
