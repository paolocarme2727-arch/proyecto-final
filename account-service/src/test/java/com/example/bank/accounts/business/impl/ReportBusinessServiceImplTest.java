package com.example.bank.accounts.business.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.business.domain.impl.AccountDomainServiceImpl;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for account reports.
 */
class ReportBusinessServiceImplTest {

    private final BankAccountRepository accountRepository = mock(BankAccountRepository.class);
    private final AccountMovementRepository movementRepository = mock(AccountMovementRepository.class);
    private final AccountDomainService accountDomainService = new AccountDomainServiceImpl(accountRepository);
    private final ReportBusinessServiceImpl reportBusinessServiceImpl = new ReportBusinessServiceImpl(
            accountRepository,
            movementRepository,
            accountDomainService);

    @BeforeEach
    void useImmediateScheduler() {
        AccountBusinessTestSupport.useImmediateScheduler();
    }

    @AfterEach
    void resetSchedulers() {
        AccountBusinessTestSupport.resetSchedulers();
    }

    @Test
    void givenAccountMovements_whenFindRecentDebitCardMovements_thenFiltersDebitCardPayments() {
        BankAccount account = BankAccount.builder().id("account-1").build();
        AccountMovement movement = AccountMovement.builder()
                .accountId("account-1")
                .type(MovementTypeEnum.DEBIT_CARD_PAYMENT)
                .amount(BigDecimal.ONE)
                .build();

        when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
        when(movementRepository.findByAccountIdAndTypeOrderByCreatedAtDesc(
                eq("account-1"),
                eq(MovementTypeEnum.DEBIT_CARD_PAYMENT),
                any(Pageable.class))).thenReturn(List.of(movement));

        reportBusinessServiceImpl.findRecentDebitCardMovements("account-1")
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(value -> value.getType() == MovementTypeEnum.DEBIT_CARD_PAYMENT);
    }
}
