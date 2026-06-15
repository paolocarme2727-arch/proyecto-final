package com.example.bank.accounts.business.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.MovementType;
import com.example.bank.accounts.events.WalletPaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for wallet payment event processing.
 */
class WalletPaymentConsumerTest {

    private final BankAccountServiceImpl accountService = mock(BankAccountServiceImpl.class);
    private final WalletPaymentConsumer consumer = new WalletPaymentConsumer(accountService, new ObjectMapper());

    @BeforeEach
    void useImmediateScheduler() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @AfterEach
    void resetSchedulers() {
        RxJavaPlugins.reset();
    }

    /**
     * Verifies that linked wallet payments are applied to source and target debit-card accounts.
     */
    @Test
    void givenWalletPaymentWithDebitCards_whenProcess_thenRegistersDebitAndCreditMovements() {
        WalletPaymentEvent event = new WalletPaymentEvent(
                "source-wallet",
                "target-wallet",
                "source-card",
                "target-card",
                BigDecimal.valueOf(25),
                LocalDateTime.now());

        when(accountService.registerWalletDebitCardMovement("source-card", BigDecimal.valueOf(25), MovementType.WALLET_PAYMENT_OUT))
                .thenReturn(Single.just(AccountMovement.builder().build()));
        when(accountService.registerWalletDebitCardMovement("target-card", BigDecimal.valueOf(25), MovementType.WALLET_PAYMENT_IN))
                .thenReturn(Single.just(AccountMovement.builder().build()));

        consumer.process(event)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertComplete()
                .assertNoErrors();

        verify(accountService).registerWalletDebitCardMovement("source-card", BigDecimal.valueOf(25), MovementType.WALLET_PAYMENT_OUT);
        verify(accountService).registerWalletDebitCardMovement("target-card", BigDecimal.valueOf(25), MovementType.WALLET_PAYMENT_IN);
    }
}

