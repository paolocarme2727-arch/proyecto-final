package com.example.bank.accounts.business.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.business.domain.impl.AccountDomainServiceImpl;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.domain.policy.MovementRules;
import com.example.bank.accounts.events.DebitCardEventPublisher;
import com.example.bank.accounts.expose.model.DebitCardRequest;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.repository.DebitCardRepository;
import com.example.bank.accounts.util.enums.AccountTypeEnum;
import com.example.bank.accounts.util.enums.MovementTypeEnum;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for debit-card business operations.
 */
class DebitCardBusinessServiceImplTest {

    private final BankAccountRepository accountRepository = mock(BankAccountRepository.class);
    private final AccountMovementRepository movementRepository = mock(AccountMovementRepository.class);
    private final DebitCardRepository debitCardRepository = mock(DebitCardRepository.class);
    private final DebitCardEventPublisher eventPublisher = mock(DebitCardEventPublisher.class);
    private final AccountDomainService accountDomainService = new AccountDomainServiceImpl(accountRepository);
    private final DebitCardBusinessServiceImpl debitCardBusinessServiceImpl = new DebitCardBusinessServiceImpl(
            accountRepository,
            movementRepository,
            debitCardRepository,
            eventPublisher,
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
    void givenExistingAccount_whenCreateDebitCard_thenRegistersOutboxEvent() {
        BankAccount account = BankAccount.builder()
                .id("account-1")
                .customerId("customer-1")
                .balance(BigDecimal.TEN)
                .build();

        when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
        when(debitCardRepository.save(any(DebitCard.class))).thenAnswer(invocation -> {
            DebitCard card = invocation.getArgument(0);
            card.setId("card-1");
            return card;
        });

        debitCardBusinessServiceImpl.createDebitCard(new DebitCardRequest(
                        "customer-1",
                        "account-1",
                        "5100000000000001"))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors();

        verify(eventPublisher).registerCreated(any(DebitCard.class));
    }

    @Test
    void givenActiveDebitCard_whenPayWithDebitCard_thenCreatesPaymentMovement() {
        DebitCard card = DebitCard.builder().id("card-1").accountId("account-1").active(true).build();
        BankAccount account = BankAccount.builder()
                .id("account-1")
                .balance(BigDecimal.TEN)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .type(AccountTypeEnum.CHECKING)
                .build();

        when(debitCardRepository.findById("card-1")).thenReturn(Optional.of(card));
        when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(AccountMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        debitCardBusinessServiceImpl.payWithDebitCard("card-1", new DebitCardPaymentRequest(BigDecimal.valueOf(4)))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(movement -> {
                    assertThat(movement.getType()).isEqualTo(MovementTypeEnum.DEBIT_CARD_PAYMENT);
                    assertThat(movement.getResultingBalance()).isEqualByComparingTo(BigDecimal.valueOf(6));
                    return true;
                });
    }
}
