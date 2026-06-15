package com.example.bank.accounts.business.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.proxy.CreditProductProxy;
import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.domain.MovementType;
import com.example.bank.accounts.events.CreditDebtStatusCache;
import com.example.bank.accounts.events.DebitCardEventPublisher;
import com.example.bank.accounts.events.WalletPaymentEvent;
import com.example.bank.accounts.repository.AccountMovementRepository;
import com.example.bank.accounts.repository.BankAccountRepository;
import com.example.bank.accounts.repository.DebitCardRepository;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerProfile;
import com.example.bank.accounts.expose.model.CustomerType;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.expose.model.MoneyRequest;
import com.example.bank.accounts.expose.model.TransferRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for account product rules.
 */
class BankAccountServiceImplTest {

    private final BankAccountRepository accountRepository = mock(BankAccountRepository.class);
    private final AccountMovementRepository movementRepository = mock(AccountMovementRepository.class);
    private final DebitCardRepository debitCardRepository = mock(DebitCardRepository.class);
    private final CreditProductProxy creditProductProxy = mock(CreditProductProxy.class);
    private final CreditDebtStatusCache creditDebtStatusCache = mock(CreditDebtStatusCache.class);
    private final DebitCardEventPublisher debitCardEventPublisher = mock(DebitCardEventPublisher.class);
    private final AccountProperties properties = new AccountProperties(
            20,
            BigDecimal.valueOf(15),
            BigDecimal.ZERO,
            20,
            BigDecimal.valueOf(3.5),
            BigDecimal.valueOf(500));
    private final BankAccountServiceImpl accountService = new BankAccountServiceImpl(
            accountRepository,
            movementRepository,
            debitCardRepository,
            properties,
            creditProductProxy,
            creditDebtStatusCache,
            debitCardEventPublisher);

    @BeforeEach
    void useImmediateScheduler() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @AfterEach
    void resetSchedulers() {
        RxJavaPlugins.reset();
    }

    /**
     * Verifies that PYME checking accounts are created without maintenance fee when the customer has a credit card.
     */
    @Test
    void givenPymeCustomerWithCreditCard_whenCreateCheckingAccount_thenHasNoMaintenanceFee() {
        AccountRequest request = new AccountRequest("business-1", CustomerType.BUSINESS, AccountType.CHECKING)
                .customerProfile(CustomerProfile.PYME)
                .holders(List.of("holder-1"))
                .initialBalance(BigDecimal.valueOf(100));

        when(creditProductProxy.customerHasCreditCard("business-1")).thenReturn(io.reactivex.rxjava3.core.Single.just(Boolean.TRUE));
        when(creditDebtStatusCache.hasOverdueDebt("business-1")).thenReturn(io.reactivex.rxjava3.core.Single.just(Boolean.FALSE));
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestObserver<BankAccount> observer = accountService.create(request).test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(account -> {
                    assertThat(account.getMaintenanceFee()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(account.getCustomerProfile()).isEqualTo(com.example.bank.accounts.domain.CustomerProfile.PYME);
                    return true;
                });
    }

    /**
     * Verifies a VIP banking flow from product creation to transfer and debit-card payment.
     */
    @Test
    void givenVipCustomerWithCreditCard_whenRunAccountFlow_thenUpdatesBalancesAndMovements() {
        Map<String, BankAccount> accounts = new HashMap<>();
        Map<String, DebitCard> cards = new HashMap<>();
        List<AccountMovement> movements = new java.util.ArrayList<>();
        AtomicInteger accountSequence = new AtomicInteger();
        AtomicInteger cardSequence = new AtomicInteger();
        AtomicInteger movementSequence = new AtomicInteger();
        useStatefulRepositories(accounts, cards, movements, accountSequence, cardSequence, movementSequence);
        when(creditDebtStatusCache.hasOverdueDebt("vip-flow")).thenReturn(Single.just(Boolean.FALSE));
        when(creditProductProxy.customerHasCreditCard("vip-flow")).thenReturn(Single.just(Boolean.TRUE));
        when(debitCardEventPublisher.publishCreated(any(DebitCard.class))).thenReturn(Completable.complete());

        BankAccount savings = accountService.create(new AccountRequest("vip-flow", CustomerType.PERSONAL, AccountType.SAVINGS)
                        .customerProfile(CustomerProfile.VIP)
                        .initialBalance(BigDecimal.valueOf(1000))
                        .minimumOpeningAmount(BigDecimal.ZERO))
                .blockingGet();
        BankAccount checking = accountService.create(new AccountRequest("vip-flow", CustomerType.PERSONAL, AccountType.CHECKING)
                        .initialBalance(BigDecimal.valueOf(200))
                        .minimumOpeningAmount(BigDecimal.ZERO))
                .blockingGet();
        DebitCard debitCard = accountService.createDebitCard(new com.example.bank.accounts.expose.model.DebitCardRequest(
                        "vip-flow",
                        savings.getId(),
                        "5100000000000001"))
                .blockingGet();

        var transfer = accountService.transfer(new TransferRequest(savings.getId(), checking.getId(), BigDecimal.valueOf(100)))
                .blockingGet();
        AccountMovement debitPayment = accountService.payWithDebitCard(
                        debitCard.getId(),
                        new DebitCardPaymentRequest(BigDecimal.valueOf(30)))
                .blockingGet();

        assertThat(transfer.getSourceBalance()).isEqualByComparingTo(BigDecimal.valueOf(900));
        assertThat(transfer.getTargetBalance()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(debitPayment.getType()).isEqualTo(MovementType.DEBIT_CARD_PAYMENT);
        assertThat(accounts.get(savings.getId()).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(870));
        assertThat(accounts.get(checking.getId()).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(movements).extracting(AccountMovement::getType)
                .containsExactly(MovementType.TRANSFER_OUT, MovementType.TRANSFER_IN, MovementType.DEBIT_CARD_PAYMENT);
    }

    /**
     * Verifies that a Yanki event charges and credits linked debit-card bank accounts.
     */
    @Test
    void givenLinkedDebitCards_whenWalletPaymentEventIsConsumed_thenMovesBankAccountBalances() {
        Map<String, BankAccount> accounts = new HashMap<>();
        Map<String, DebitCard> cards = new HashMap<>();
        List<AccountMovement> movements = new java.util.ArrayList<>();
        useStatefulRepositories(accounts, cards, movements, new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
        BankAccount source = BankAccount.builder()
                .id("source-account")
                .customerId("source-customer")
                .balance(BigDecimal.valueOf(300))
                .type(com.example.bank.accounts.domain.AccountType.CHECKING)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        BankAccount target = BankAccount.builder()
                .id("target-account")
                .customerId("target-customer")
                .balance(BigDecimal.valueOf(50))
                .type(com.example.bank.accounts.domain.AccountType.CHECKING)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        accounts.put(source.getId(), source);
        accounts.put(target.getId(), target);
        cards.put("source-card", DebitCard.builder().id("source-card").accountId(source.getId()).active(true).build());
        cards.put("target-card", DebitCard.builder().id("target-card").accountId(target.getId()).active(true).build());

        WalletPaymentConsumer consumer = new WalletPaymentConsumer(accountService, new ObjectMapper());
        consumer.process(new WalletPaymentEvent(
                        "source-wallet",
                        "target-wallet",
                        "source-card",
                        "target-card",
                        BigDecimal.valueOf(25),
                        LocalDateTime.now()))
                .blockingAwait();

        assertThat(accounts.get(source.getId()).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(275));
        assertThat(accounts.get(target.getId()).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(75));
        assertThat(movements).extracting(AccountMovement::getType)
                .containsExactly(MovementType.WALLET_PAYMENT_OUT, MovementType.WALLET_PAYMENT_IN);
    }

    /**
     * Verifies that VIP savings accounts require an existing credit card.
     */
    @Test
    void givenVipCustomerWithoutCreditCard_whenCreateSavingsAccount_thenRejectsAccount() {
        AccountRequest request = new AccountRequest("vip-1", CustomerType.PERSONAL, AccountType.SAVINGS)
                .customerProfile(CustomerProfile.VIP)
                .initialBalance(BigDecimal.valueOf(700))
                .minimumOpeningAmount(BigDecimal.ZERO);

        when(creditDebtStatusCache.hasOverdueDebt("vip-1")).thenReturn(io.reactivex.rxjava3.core.Single.just(Boolean.FALSE));
        when(creditProductProxy.customerHasCreditCard("vip-1")).thenReturn(io.reactivex.rxjava3.core.Single.just(Boolean.FALSE));
        when(accountRepository.existsByCustomerIdAndType(
                "vip-1",
                com.example.bank.accounts.domain.AccountType.SAVINGS)).thenReturn(Boolean.FALSE);

        TestObserver<BankAccount> observer = accountService.create(request).test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertError(error -> error instanceof ResponseStatusException exception
                && exception.getStatusCode().equals(HttpStatus.BAD_REQUEST));
        verify(accountRepository, never()).save(any(BankAccount.class));
    }

    /**
     * Verifies that debit card payments charge the linked account.
     */
    @Test
    void givenActiveDebitCard_whenPayWithDebitCard_thenCreatesPaymentMovement() {
        DebitCard card = DebitCard.builder().id("card-1").accountId("account-1").active(true).build();
        BankAccount account = BankAccount.builder()
                .id("account-1")
                .balance(BigDecimal.TEN)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .type(com.example.bank.accounts.domain.AccountType.CHECKING)
                .build();

        when(debitCardRepository.findById("card-1")).thenReturn(Optional.of(card));
        when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movementRepository.save(any(AccountMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestObserver<AccountMovement> observer = accountService.payWithDebitCard(
                "card-1",
                new DebitCardPaymentRequest(BigDecimal.valueOf(4))).test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(movement -> {
                    assertThat(movement.getType()).isEqualTo(com.example.bank.accounts.domain.MovementType.DEBIT_CARD_PAYMENT);
                    assertThat(movement.getResultingBalance()).isEqualByComparingTo(BigDecimal.valueOf(6));
                    return true;
                });
    }

    /**
     * Verifies that savings accounts reject movements after their monthly cap.
     */
    @Test
    void givenSavingsAccountAtMonthlyLimit_whenWithdraw_thenRejectsMovement() {
        BankAccount account = BankAccount.builder()
                .id("savings-1")
                .balance(BigDecimal.valueOf(100))
                .type(com.example.bank.accounts.domain.AccountType.SAVINGS)
                .monthlyMovementLimit(1)
                .monthlyMovementCount(1)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .build();

        when(accountRepository.findById("savings-1")).thenReturn(Optional.of(account));

        TestObserver<AccountMovement> observer = accountService.withdraw(
                "savings-1",
                new MoneyRequest(BigDecimal.TEN)).test();

        observer.awaitDone(5, TimeUnit.SECONDS).assertError(ResponseStatusException.class);
        verify(accountRepository, never()).save(any(BankAccount.class));
    }

    /**
     * Verifies that VIP savings accounts keep the configured daily average floor.
     */
    @Test
    void givenVipSavingsAccount_whenWithdrawalWouldBreakAverageFloor_thenRejectsMovement() {
        BankAccount account = BankAccount.builder()
                .id("vip-1")
                .balance(BigDecimal.valueOf(550))
                .customerProfile(com.example.bank.accounts.domain.CustomerProfile.VIP)
                .type(com.example.bank.accounts.domain.AccountType.SAVINGS)
                .minimumDailyAverageAmount(BigDecimal.valueOf(500))
                .monthlyMovementLimit(20)
                .freeTransactionLimit(20)
                .transactionFee(BigDecimal.valueOf(3.5))
                .chargedFees(BigDecimal.ZERO)
                .build();

        when(accountRepository.findById("vip-1")).thenReturn(Optional.of(account));

        TestObserver<AccountMovement> observer = accountService.withdraw(
                "vip-1",
                new MoneyRequest(BigDecimal.valueOf(75))).test();

        observer.awaitDone(5, TimeUnit.SECONDS).assertError(ResponseStatusException.class);
        verify(accountRepository, never()).save(any(BankAccount.class));
    }

    /**
     * Verifies that personal customers may own multiple fixed term accounts.
     */
    @Test
    void givenPersonalCustomer_whenCreateFixedTermAccount_thenDoesNotCheckDuplicateType() {
        AccountRequest request = new AccountRequest("customer-1", CustomerType.PERSONAL, AccountType.FIXED_TERM)
                .initialBalance(BigDecimal.valueOf(100))
                .movementDay(LocalDate.now().getDayOfMonth());

        when(creditDebtStatusCache.hasOverdueDebt("customer-1")).thenReturn(io.reactivex.rxjava3.core.Single.just(Boolean.FALSE));
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestObserver<BankAccount> observer = accountService.create(request).test();

        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
        verify(accountRepository, never()).existsByCustomerIdAndType(
                eq("customer-1"),
                eq(com.example.bank.accounts.domain.AccountType.FIXED_TERM));
    }

    /**
     * Verifies that debit card recent movement reports only include debit card payment movements.
     */
    @Test
    void givenAccountMovements_whenFindRecentDebitCardMovements_thenFiltersDebitCardPayments() {
        BankAccount account = BankAccount.builder().id("account-1").build();
        AccountMovement movement = AccountMovement.builder()
                .accountId("account-1")
                .type(MovementType.DEBIT_CARD_PAYMENT)
                .amount(BigDecimal.ONE)
                .build();

        when(accountRepository.findById("account-1")).thenReturn(Optional.of(account));
        when(movementRepository.findByAccountIdAndTypeOrderByCreatedAtDesc(
                eq("account-1"),
                eq(MovementType.DEBIT_CARD_PAYMENT),
                any(Pageable.class))).thenReturn(List.of(movement));

        var observer = accountService.findRecentDebitCardMovements("account-1").test();

        observer.awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors()
                .assertValue(value -> value.getType() == MovementType.DEBIT_CARD_PAYMENT);
    }

    private void useStatefulRepositories(
            Map<String, BankAccount> accounts,
            Map<String, DebitCard> cards,
            List<AccountMovement> movements,
            AtomicInteger accountSequence,
            AtomicInteger cardSequence,
            AtomicInteger movementSequence) {
        when(accountRepository.findById(any(String.class))).thenAnswer(invocation -> Optional.ofNullable(accounts.get(invocation.getArgument(0))));
        when(accountRepository.existsByCustomerIdAndType(any(String.class), any(com.example.bank.accounts.domain.AccountType.class)))
                .thenAnswer(invocation -> accounts.values().stream()
                        .anyMatch(account -> account.getCustomerId().equals(invocation.getArgument(0))
                                && account.getType() == invocation.getArgument(1)));
        when(accountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            if (account.getId() == null) {
                account.setId("account-" + accountSequence.incrementAndGet());
            }
            accounts.put(account.getId(), account);
            return account;
        });
        when(debitCardRepository.findById(any(String.class))).thenAnswer(invocation -> Optional.ofNullable(cards.get(invocation.getArgument(0))));
        when(debitCardRepository.save(any(DebitCard.class))).thenAnswer(invocation -> {
            DebitCard card = invocation.getArgument(0);
            if (card.getId() == null) {
                card.setId("card-" + cardSequence.incrementAndGet());
            }
            cards.put(card.getId(), card);
            return card;
        });
        when(movementRepository.save(any(AccountMovement.class))).thenAnswer(invocation -> {
            AccountMovement movement = invocation.getArgument(0);
            if (movement.getId() == null) {
                movement.setId("movement-" + movementSequence.incrementAndGet());
            }
            movements.add(movement);
            return movement;
        });
    }
}

