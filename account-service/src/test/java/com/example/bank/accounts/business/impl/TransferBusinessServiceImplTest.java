package com.example.bank.accounts.business.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.business.domain.impl.AccountDomainServiceImpl;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.policy.MovementRules;
import com.example.bank.accounts.expose.model.TransferRequest;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for transfers.
 */
class TransferBusinessServiceImplTest {

    private final BankAccountRepository accountRepository = mock(BankAccountRepository.class);
    private final AccountMovementRepository movementRepository = mock(AccountMovementRepository.class);
    private final AccountDomainService accountDomainService = new AccountDomainServiceImpl(accountRepository);
    private final TransferBusinessServiceImpl transferBusinessServiceImpl = new TransferBusinessServiceImpl(
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
    void givenTwoAccounts_whenTransfer_thenUpdatesBalancesAndCreatesMovements() {
        BankAccount source = bankAccount("source-account", BigDecimal.valueOf(300));
        BankAccount target = bankAccount("target-account", BigDecimal.valueOf(50));

        when(accountRepository.findById("source-account")).thenReturn(Optional.of(source));
        when(accountRepository.findById("target-account")).thenReturn(Optional.of(target));
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(AccountMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transferBusinessServiceImpl.transfer(new TransferRequest("source-account", "target-account", BigDecimal.valueOf(25)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(response -> {
                    assertThat(response.getSourceBalance()).isEqualByComparingTo(BigDecimal.valueOf(275));
                    assertThat(response.getTargetBalance()).isEqualByComparingTo(BigDecimal.valueOf(75));
                    return true;
                });
    }

    private BankAccount bankAccount(String id, BigDecimal balance) {
        return BankAccount.builder()
                .id(id)
                .customerId(id + "-customer")
                .balance(balance)
                .type(AccountTypeEnum.CHECKING)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .monthlyMovementCount(0)
                .build();
    }
}
