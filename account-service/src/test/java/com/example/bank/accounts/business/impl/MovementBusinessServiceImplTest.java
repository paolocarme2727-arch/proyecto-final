package com.example.bank.accounts.business.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.business.domain.impl.AccountDomainServiceImpl;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.policy.MovementRules;
import com.example.bank.accounts.expose.model.MoneyRequest;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.CustomerProfileEnum;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for account movements.
 */
class MovementBusinessServiceImplTest {

    private final BankAccountRepository accountRepository = mock(BankAccountRepository.class);
    private final AccountMovementRepository movementRepository = mock(AccountMovementRepository.class);
    private final AccountDomainService accountDomainService = new AccountDomainServiceImpl(accountRepository);
    private final MovementBusinessServiceImpl movementBusinessServiceImpl = new MovementBusinessServiceImpl(
            accountRepository,
            movementRepository,
            new MovementRules(AccountBusinessTestSupport.properties()),
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
    void givenSavingsAccountAtMonthlyLimit_whenWithdraw_thenRejectsMovement() {
        BankAccount account = BankAccount.builder()
                .id("savings-1")
                .balance(BigDecimal.valueOf(100))
                .type(AccountTypeEnum.SAVINGS)
                .monthlyMovementLimit(1)
                .monthlyMovementCount(1)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .build();

        when(accountRepository.findById("savings-1")).thenReturn(Optional.of(account));

        movementBusinessServiceImpl.withdraw("savings-1", new MoneyRequest(BigDecimal.TEN))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
        verify(accountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void givenVipSavingsAccount_whenWithdrawalWouldBreakAverageFloor_thenRejectsMovement() {
        BankAccount account = BankAccount.builder()
                .id("vip-1")
                .balance(BigDecimal.valueOf(550))
                .customerProfileEnum(CustomerProfileEnum.VIP)
                .type(AccountTypeEnum.SAVINGS)
                .minimumDailyAverageAmount(BigDecimal.valueOf(500))
                .monthlyMovementLimit(20)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .build();

        when(accountRepository.findById("vip-1")).thenReturn(Optional.of(account));

        movementBusinessServiceImpl.withdraw("vip-1", new MoneyRequest(BigDecimal.valueOf(75)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
        verify(accountRepository, never()).save(any(BankAccount.class));
    }
}
